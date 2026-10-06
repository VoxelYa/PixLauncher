# PixLauncher — Feature Manifest (Phase 1)

Source of the feature list: FPSMaster-Edge (`D:\reference-repos\FPSMaster-Edge`, GPL-3.0).
**We only took the feature list as inspiration — no code is copied** (user decision), so PixLauncher is not a GPL derivative.

Total: **49 features accepted** out of 67 modules + 4 non-module features reviewed.

Legend: ✅ = build in Phase 1 · ❌ = rejected by user · 🫧 = accepted, later phase (large)

## Interface / HUD (20 accepted)

| # | Feature | Module ID | Status |
|---|---|---|---|
| 1 | FPS 显示 | FPSDisplay | ✅ |
| 2 | CPS 显示 | CPSDisplay | ✅ |
| 3 | 按键显示 (WASD/鼠标/空格, 动画) | Keystrokes | ✅ |
| 4 | 延迟显示 | PingDisplay | ✅ |
| 5 | 坐标显示 | CoordsDisplay | ✅ |
| 6 | 连击显示 | ComboDisplay | ✅ |
| 7 | 盔甲显示 | ArmorDisplay | ✅ |
| 8 | 药水效果显示 | PotionDisplay | ✅ |
| 9 | 计分板美化 | Scoreboard | ✅ |
| 10 | Tab 列表美化 | TabOverlay | ✅ |
| 11 | 攻击距离显示 | ReachDisplay | ✅ |
| 12 | 伤害指示 HUD | DamageIndicatorHUD | ✅ |
| 13 | 切换潜行 | ToggleSneak | ✅ |
| 14 | 自动疾跑 | Sprint | ✅ |
| 15 | 功能列表水印 | ModsList | ✅ |
| 16 | 服务器地址显示 | ServerAddressDisplay | ✅ |
| 17 | TNT 计时 HUD | TNTTimerHUD | ✅ |
| 18 | 聊天增强 (折叠+复制) | BetterChat | ✅ |
| 19 | 界面美化 (背景模糊/动画) | BetterScreen | ✅ |
| 20 | 性能诊断 HUD (FPS曲线/内存/GC) | PerformanceHud | ✅ |

Rejected HUD: DirectionDisplay(方向罗盘) ❌, ClockDisplay(时钟) ❌, TargetDisplay(目标信息+ESP) ❌, ItemCountDisplay(物品计数) ❌, InventoryDisplay(背包显示) ❌, PlayTime(游戏时长) ❌, BlockIndicator(方块信息) ❌, LyricsDisplay(歌词) ❌, PlayerDisplay(附近玩家) ❌, HideIndicator(隐藏攻击指示) ❌, CustomTitles(标题调整) ❌, MiniMap(小地图) ❌, SaturationDisplay(饱和度条) ❌

## Render (14 accepted)

| # | Feature | Module ID | Status |
|---|---|---|---|
| 21 | 自定义准星 | Crosshair | ✅ |
| 22 | 全亮 | FullBright | ✅ |
| 23 | 动态模糊 | MotionBlur | ✅ |
| 24 | 受击变色 | HitColor | ✅ |
| 25 | 碰撞箱显示 | Hitboxes | ✅ |
| 26 | 方块高亮 | BlockOverlay | ✅ |
| 27 | 掉落物物理 | ItemPhysics | ✅ |
| 28 | 自定义雾效 | CustomFog | ✅ |
| 29 | 火焰覆盖调整 | FireModifier | ✅ |
| 30 | 视角摇晃减小 | MinimizedBobbing | ✅ |
| 31 | 更多攻击粒子/击杀特效 | MoreParticles | ✅ |
| 32 | 隐藏自身粒子 | CleanView | ✅ |
| 33 | 聊天头像 (LRU 缓存) | ChatAvatars | ✅ |
| 34 | FOV 控制 (去疾跑/飞行/拉弓变焦) | CustomFov | ✅ |

Rejected Render: FreeLook(自由视角) ❌, DamageIndicator(3D飘浮伤害数字) ❌

## Optimize (4 accepted)

| # | Feature | Module ID | Status |
|---|---|---|---|
| 35 | **性能优化 — 强制开启, 仅无损** | Performance | ✅ (forced-on) |
| 36 | 1.7 经典动画 (格挡/鱼竿/拉弓, 多风格) | OldAnimations | ✅ |
| 37 | 平滑缩放 | SmoothZoom | ✅ |
| 38 | 去受伤镜头晃动 | NoHurtCam | ✅ |

**Performance policy (user decision):**
- Performance optimizations are **always on, no toggle**.
- **Lossless only**: occlusion/entity culling, particle frustum culling, chunk rebuild throttling,
  HUD text geometry cache, glyph atlas, model batching, fast texture upload, fast glyph lookup,
  sky-color cache, armor-texture cache, sign-text culling, fast load, adaptive chunk budget, etc.
- **NO quality-reducing "subtraction" options**: no texture downscaling, no hiding grass/flowers/fences,
  no low-animation-tick, no entity render distance reduction. Visuals stay identical to vanilla.

Rejected Optimize: NoHitDelay(无攻击延迟) ❌, FixedInventory(背包鼠标修复) ❌, BetterFishingRod(鱼竿美化) ❌

## Utility (8 accepted)

| # | Feature | Module ID | Status |
|---|---|---|---|
| 39 | 客户端核心设置 (设置中心/ClickGUI/键位) | ClientSettings | ✅ (base) |
| 40 | 自动 GG (可自动排队) | AutoGG | ✅ |
| 41 | 快捷消息 | AutoText | ✅ |
| 42 | 名牌美化 | Nametags | ✅ |
| 43 | 分类音量控制 | SoundModifier | ✅ |
| 44 | 时间修改 (仅客户端) | TimeChanger | ✅ |
| 45 | 粒子行为修改 | ParticlesModifier | ✅ |
| 46 | TNT 计时器 (配合 TNTTimerHUD) | TNTTimer | ✅ |

Rejected Utility: NameProtect(名字保护) ❌, RawInput(原始输入) ❌

## Non-module client features (3 accepted, 1 later phase)

| # | Feature | Status | Note |
|---|---|---|---|
| 47 | HUD 编辑器 (拖拽/缩放所有部件) | ✅ | required by all HUD widgets |
| 48 | 多配置档案 (切换/导入/导出) | ✅ | |
| 49 | 内置回放系统 | 🫧 | user wants it **in addition to** ReplayMod; large project — phase 2 |
| — | 音乐播放器 | ❌ | (LyricsDisplay rejected with it) |
| — | 饰品系统 (披风/龙翼) | ❌ | |

## Preinstalled third-party mods (not ours, loaded normally)

| Mod | Source | Note |
|---|---|---|
| OptiFine 1.8.9 (HD U latest) | optifine.net official + BMCLAPI mirror, speed-tested | NOT on Modrinth (API 404, verified 2026-10-05) |
| ReplayMod 1.8.9 (Forge) | Modrinth `replaymod` | verified: Forge + 1.8.9 supported |
