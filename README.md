<div align="center">
  <img src="assets/icon.png" width="128" alt="PixLauncher logo">
  <h1>PixLauncher</h1>
  <p>A PvP launcher for Minecraft 1.8.9 Forge, with its own built-in client.</p>
</div>

---

I started PixLauncher because setting up 1.8.9 the way PvP players actually want it has
always been annoying: Forge, OptiFine, ReplayMod, then a pile of client mods on top, and
half of them fight each other. This launcher does that setup for you on first run, and it
ships its own game client whose features are wired directly into the game at launch —
the same approach Lunar Client uses, so nothing gets dropped into your mods folder and
nothing touches your normal `.minecraft`.

Everything here was written from scratch for this project. The feature list takes
inspiration from other PvP clients, but no code was borrowed.

## What it does

**Launcher (the exe):**

- Signs you in with your Microsoft account through the browser, PKCE + loopback redirect
- Downloads everything on first run — vanilla 1.8.9, Forge, OptiFine and ReplayMod —
  picking between the official Mojang servers and a community mirror (BMCLAPI) based on
  measured speed, not guesswork
- Bundles its own Java 8 runtime so you don't have to install anything
- Lets you browse and install mods from Modrinth straight into your mods folder
- Updates itself; game files live in a `.minecraft` folder next to the exe, isolated
  from your system install

**In-game client (49 features):**

- HUD: FPS, CPS, keystrokes, ping, coordinates, combo counter, armor and potion status,
  scoreboard and tab list, reach, damage, sprint/toggle-sneak, TNT timers, chat tools and
  more — all movable and resizable with a drag-and-drop HUD editor
- Render: custom crosshair, fullbright, hit color, hitboxes, block overlay, item
  physics, fog control, motion blur, chat avatars, FOV fixes
- Utility: auto-GG, quick chat messages, custom nametags, per-category volume, client-side
  time change, profile system for saving setups
- Performance: a set of lossless engine optimizations (culling, caching, batching,
  faster loading) that are always on. We deliberately do not include options that lower
  visual quality to gain FPS.

Settings are English-only for now, both in the launcher and in game.

## Getting started

Grab the latest exe from Releases, run it, sign in, wait for the download, press Play.
That's the whole flow. Windows only for now.

## Building from source

The launcher is Electron + TypeScript:

```bash
cd launcher
npm install
npm start          # dev run
npm run dist       # portable exe
```

The client is Java 8, compiled against the deobfuscated 1.8.9 jar that Forge's own
installer produces. See `client/README.md` for the setup.

## Notes

- No telemetry. Crash logs stay on your disk.
- Your login tokens are stored encrypted (layered AES-GCM and ChaCha20-Poly1305,
  key material bound to your user account); the plaintext only ever exists briefly
  in memory.
- MCP mappings, Forge and OptiFine belong to their respective owners. The launcher
  downloads them from official or mirrored sources at runtime rather than
  redistributing them.

## License

MIT — you may use and build on this project, including commercially, as long as the
attribution stays visible: keep a "Based on PixLauncher" credit in your app's UI
(about screen or footer both work). See [LICENSE](LICENSE).
