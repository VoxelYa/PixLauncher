package top.pixlauncher.hud;

import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;

/** Indirection so HudManager can resolve modules without a cyclic compile dependency. */
final class ModuleManagerHolder {
    private ModuleManagerHolder() { }

    static Module byId(String id) {
        return ModuleManager.byId(id);
    }
}
