package top.pixlauncher.gui;

import top.pixlauncher.config.ConfigManager;

/** tiny indirection so the gui file stays readable */
final class ConfigManagerBridge {
    private ConfigManagerBridge() { }

    static void save() { ConfigManager.save(); }
}
