# PixLauncher Client (Java 8)

Lunar-style client for Minecraft 1.8.9 + Forge. **Not a Forge mod, no javaagent**:
`PixBootstrap` sits first in the launchwrapper tweak chain and weaves features
into game classes in memory (SpongePowered Mixin + ASM transformers).

Layout:
- `bootstrap/` — `PixBootstrap` (tweak chain entry, TweakOrder -1000) + ASM transformers
- `module/` — Module/Category/ModuleManager + settings framework
- `features/` — the 49 accepted features (docs/FEATURES.md)
- `mixin/` — mixin classes (populated from M3)

## Dev environment (one-time)
1. Generate a deobfuscated 1.8.9 jar with MCP `stable_22` mappings and install to
   mavenLocal as `top.pixlauncher:minecraft-deobf:1.8.9` (compile-only dependency).
2. `gradlew build` — produces `build/libs/pixlauncher-client-0.1.0.jar`.
3. Copy the jar (+ mixin/asm deps) into `<launcher>/.minecraft/pixclient/` — the
   launcher detects that folder and automatically prepends
   `--tweakClass top.pixlauncher.bootstrap.PixBootstrap` before FMLTweaker.

## Rules
- In-game UI English only; forced-on performance work is **lossless only**
  (no visual downgrades — user decision).
- Feature code references FPSMaster-Edge's *ideas only*; never copy its GPL code.
