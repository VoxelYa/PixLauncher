package top.pixlauncher.features.hud;

import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.settings.BooleanSetting;

/**
 * First vertical-slice feature (accepted in docs/FEATURES.md #1).
 * Rendering wiring (HUD pipeline + position via the HUD editor) lands in M3;
 * the module skeleton, settings and config persistence exist from day one.
 */
public final class FpsDisplay extends Module {

    private final BooleanSetting shadow = new BooleanSetting("shadow", "Font shadow", true);

    public FpsDisplay() {
        super("fpsdisplay", "FPS Display", Category.HUD);
    }

    @Override
    protected void init() {
        // settings were auto-registered by reflection in ModuleManager
    }
}
