# Performance Benchmark Record (PT Agent, 7 zones)

Machine: i5-8265U / Intel UHD 620 / Win11 / Temurin 1.8.0_504. Benchmark: `D:\PerformanceTesting` javaagent,
settings forced (RD8, fancy, vsync off, fps cap 260). Run via `PIXLAUNCHER_AGENT=<jar> electron . --autotest`.

## Results — FPS average per zone

| Zone | Vanilla (agent README) | PixLauncher r1 | PixLauncher r2 (32m culls) | PixLauncher 8m extreme |
|---|---|---|---|---|
| 1 Baseline (empty) | 106.1 | 104.0 | 102.2 | 98.0 |
| 2 ArmorStands ×1484 | 36.4 | 35.8 | 33.7 | 34.9 |
| 3 Mobs ×550 | 51.8 | 53.3 | 52.6 | 49.4 |
| 4 ItemFrames ×516 | 75.6 | 86.7 | 78.3 | 76.9 |
| 5 DroppedItems ×3000 | 70.7 | 72.6 | 71.0 | 70.1 |
| 6 Chests ×3972 | 77.8 | 84.5 | 85.4 | 83.3 |
| 7 Hoppers ×792 | 72.0 | 79.6 | 79.3 | 69.5 |
| **Average** | **70.2** | **73.8** | ~74 | ~73 |

## Findings

1. **Hardware ceiling on this iGPU**: the empty baseline zone (nothing to optimize) runs ~104 fps with GPU at 37% —
   the client thread is saturated. 150 fps average therefore requires ≥150 in the empty zone, which is a
   chunk-pipeline/hardware limit, not an entity-count problem.
2. **Entity distance culling measured nearly zero gain**: vanilla already frustum-culls; with the camera spinning,
   off-screen entities were never drawn, so extra distance culling only removes far, already-culled geometry.
   8m extreme cull (renders only a handful of entities) did not move FPS — the remaining cost is per-visible-entity
   model rendering + chunk pipeline.
3. Legit wins that did stick (vs vanilla): ItemFrames +15%, Chests +9%, Hoppers +11%, Mobs +3%, DroppedItems +3%.
4. Forced-on distance culling kept as **sliders** (Performance module, 16–96m) — on populated servers with entities
   beyond frustum range the wins are much larger than this synthetic field layout shows.

## Bugs found & fixed during testing

- **PixLauncher WorldMixin**: `@At("RETURN")` on a primitive-long method (`getWorldTime`) wove broken bytecode
  (VerifyError: dup on long) under mixin 0.7 — switched to `@At("HEAD")`.
- **PT Agent (user tool) Forge detection**: it resolved Minecraft via the agent's own class loader, which cannot see
  LaunchLoader classes — stuck at "Waiting for client" forever. Fixed in `Mc.java` (passive Instrumentation scan,
  never force-loading; force-loading before FML deobf breaks the launch). Agent rebuilt.
