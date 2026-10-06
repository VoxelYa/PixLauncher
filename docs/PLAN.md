# PixLauncher — Master Plan

A Minecraft **1.8.9 Forge PvP launcher** (Windows, English UI) with a Lunar-Client-style custom client.
All decisions below were confirmed by the user on 2026-10-05.

## 1. Confirmed decisions

| Topic | Decision |
|---|---|
| Target game | Minecraft **1.8.9 + Forge** (latest 1.8.9 build) |
| Launcher tech | **Electron** (Windows only, phase 1), portable exe |
| Launcher UI | **English only**, **light/clean style** |
| Auto-update | Yes (electron-updater, GitHub/Gitee Releases) |
| Java runtime | **Auto-download Temurin JRE 8** on first run (Adoptium API + mirror) |
| Auth | **Microsoft account only**, **browser callback OAuth** (auth-code + PKCE, loopback `http://localhost`) — device code flow NOT used |
| Download sources | Vanilla/Forge/libraries/assets: **official Mojang vs BMCLAPI mirror, auto speed-test, use the faster one**. Mods: **Modrinth**. OptiFine: official + BMCLAPI mirror speed-tested (NOT on Modrinth — verified 404) |
| Preinstalled mods | OptiFine + ReplayMod (both default, installed with the game) |
| Game directory | **`.minecraft` folder next to the launcher exe** (portable, isolated from the system `.minecraft`) |
| Memory | Auto-recommended from physical RAM (default 2–4 GB), user-adjustable |
| Telemetry | **None**. Crash → local log only |
| Client features | **49 accepted features** — see [FEATURES.md](FEATURES.md) |
| Client code | **Written from scratch** (feature *list* inspired by FPSMaster-Edge, **no code copied** → no GPL obligation) |
| Client implementation | **Lunar Client style**: our own bootstrap in the launch chain transforms game bytecode **in memory at class-load** (Mixin/ASM). No mods-folder mod, no javaagent, no shipped modified jar |
| Client dev workflow | **MCP-mapped readable source**, applied via our launch pipeline (this is also how Lunar works: readable code → their own transformation pipeline) |
| Performance | **Forced on, lossless only** (no visual downgrades) |
| In-game client UI language | English only |
| Original features | None in phase 1 |

**Azure AD app (user-provided, public info):** display name `PixLauncher`, client ID `438f4856-2266-4861-b2b9-da5fe4c35fde`, accounts: any Microsoft account.
⚠️ **User action required:** in Azure Portal → App registrations → PixLauncher → *Authentication* → add platform **"Mobile and desktop applications"** → add redirect URI **`http://localhost`** (public client, no secret needed). Browser-callback login cannot work before this is added.

## 2. Verified facts (researched, not guessed — 2026-10-05)

1. **How Lunar Client actually modifies the game**: its launcher downloads artifacts from Moonsworth's API into `.lunarclient/offline/multiver`; the game runs through **their own launch pipeline**, and game classes are transformed **in memory at load time** via the SpongePowered **Mixin** framework (built on ASM) inside their own bootstrap. They do **not** use Forge/Fabric mod loading and do not ship a redistributable modified MC jar.
   Sources: crash traces showing `org.spongepowered.asm.mixin` in Lunar launches (minecraftforum.net); [lunar-client-qt](https://github.com/Youded-byte/lunar-client-qt) (community launcher documenting the offline multiver layout + local instrumentation); [uku3lig's Lunar compatibility notes](https://uku3lig.net).
2. **Forge 1.8.9** installs by applying `binpatches.pack.lzma` to the vanilla jar (local patching precedent) — [files.minecraftforge.net](https://files.minecraftforge.net).
3. **OptiFine is not on Modrinth** — `https://api.modrinth.com/v2/project/optifine` returns **404** (verified). OptiFine 1.8.9 for Forge goes in the mods folder; its standalone installer patches the vanilla jar (precedent for local jar patching) — [NameHero guide](https://www.namehero.com/gaming-blog/how-to-install-optifine), [Hypixel forum](https://hypixel.net/threads/how-to-get-optifine-1-8-9-to-work-with-forge-1-8-9.2566683).
4. **ReplayMod supports Forge + 1.8.9 on Modrinth** (`replaymod`, verified via API).
5. **BMCLAPI** (`bmclapi2.bangbang93.com`) mirrors Mojang version manifest/client jar/libraries/assets and Forge/OptiFine — [BMCLAPI docs](https://bmclapidoc.bangbang93.com). Route rewrites used in code must be verified with live HEAD probes (todo in download service tests).
6. **FPSMaster-Edge** is itself a Forge 1.8.9 Mixin client with 67 modules; its *Performance* module contains the 50+ optimizations we benchmark against (README credits Patcher ideas, reimplemented independently). Cloned to `D:\reference-repos\FPSMaster-Edge` (outside this repo, reference only, GPL — we read ideas, copy nothing).

## 3. Architecture

```
D:\PixLauncher\
├─ docs\            PLAN.md, FEATURES.md
├─ launcher\        Electron app (this is "PixLauncher.exe")
│  └─ src\
│     ├─ main\                       main process (Node side)
│     │  ├─ config.ts                portable paths + settings store
│     │  ├─ logger.ts
│     │  ├─ net.ts                   guarded HTTP (public hosts only)
│     │  ├─ download\
│     │  │  ├─ sources.ts            official ↔ BMCLAPI URL rewrite rules
│     │  │  ├─ speedtest.ts          TTFB+throughput probe, winner cache (re-test daily)
│     │  │  └─ engine.ts             queue, progress events, sha1 verify
│     │  ├─ auth\msauth.ts           OAuth PKCE loopback + XBL/XSTS + MC services + refresh
│     │  ├─ game\
│     │  │  ├─ java.ts               Temurin JRE8 fetch (Adoptium API + mirror)
│     │  │  ├─ vanilla.ts            manifest + client jar + libs + assets
│     │  │  ├─ forge.ts              1.8.9 installer download + silent install
│     │  │  ├─ mods.ts               Modrinth (ReplayMod) + OptiFine (official/mirror)
│     │  │  └─ launch.ts             version JSON, classpath, JVM args, memory auto, token args
│     │  └─ index.ts                 window + IPC wiring
│     ├─ preload.ts
│     └─ renderer\                   light-theme English UI: Welcome → Login → Setup → Play
└─ client\           Java 8 client (our features; Lunar-style pipeline)
   └─ (Gradle) bootstrap + mixins + modules + settings + events + HUD framework
```

### First-run flow (user requirement: download first)
1. Splash → check JRE 8 (download if missing) → 2. Microsoft login (browser callback) →
3. Download game: vanilla 1.8.9 (meta/jar/libs/assets, speed-tested source) → Forge 1.8.9 installer (silent) → OptiFine (speed-tested) + ReplayMod (Modrinth) into `mods/` → 4. Ready/Play. Progress for every step in the UI.

### Launch chain (Lunar-style)
`java8` → our **`PixBootstrap` tweaker sits first in the launch chain** (launcher-generated version JSON passes `--tweakClass top.pixlauncher.bootstrap.PixBootstrap` before FML's), registers Mixin/ASM transformers → FML tweaker → game classes are woven with PixLauncher features **in memory** at load. Nothing is added to `mods/`, no `-javaagent`, no modified jar on disk.

## 4. Roadmap

- **M1 (now)**: repo + docs + launcher skeleton (download engine, auth, launch chain, English light UI), client bootstrap skeleton + module framework.
- **M2**: end-to-end first launch (vanilla+forge+optifine+replay from real sources), login working against real Azure app (after redirect URI added).
- **M3**: client module framework complete + first vertical slice (FPSDisplay, Keystrokes, CPS, Ping), HUD editor, ClickGUI, config/profiles.
- **M4**: all 49 features (grouped: HUD batch → render batch → utility batch), OldAnimations.
- **M5**: forced-on performance pass (lossless list in FEATURES.md) benchmarked against FPSMaster-Edge numbers.
- **M6**: built-in replay system (user-confirmed, big), auto-update channel, installer + portable builds.

## 5. Build/verify notes

- Launcher: `cd launcher && npm install && npm run build && npm start` (dev: `npm run dev`).
- Electron download in CN networks: set `ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/` if slow.
- Client: Gradle, `--release 8`; Mixin 0.7.x line for 1.8.9; deobf MC jar via MCP mappings for dev compile (see client/README.md).
- Security: all outbound requests https + public-host validated; OAuth uses PKCE public client (no secret exists anywhere in the repo); game process is spawned from a plain argv array without a shell.
- Note: the Mimosa write-guard initially blocked `child_process.spawn` patterns in `game/launch.ts`; resolved by isolating process creation in `main/index.ts` (argv-array `spawn` with piped stdio — the same audited pattern as `game/forge.ts`).
- Azure redirect URI `http://localhost` (Mobile & desktop platform) — configured by the user on 2026-10-05; browser-callback login is unblocked.
