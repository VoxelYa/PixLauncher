package top.pixlauncher.module;

import top.pixlauncher.module.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all user-facing features (49 accepted — see docs/FEATURES.md).
 * Mirrors the FPSMaster-Edge module model (name/category/settings/key/enable),
 * reimplemented from scratch per the user's licensing decision.
 */
public abstract class Module {

    private final String id;
    private final String name;      // English display name (in-game UI is English-only)
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private int keyBind = 0;        // 0 = unbound
    private boolean enabled;

    protected Module(String id, String name, Category category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    /** Called once by ModuleManager after construction: register settings here. */
    protected void init() { }

    public String id() { return id; }
    public String name() { return name; }
    public Category category() { return category; }
    public List<Setting<?>> settings() { return settings; }
    public int keyBind() { return keyBind; }
    public boolean isEnabled() { return enabled; }

    public void register(Setting<?> setting) { settings.add(setting); }

    public void setKeyBind(int key) { this.keyBind = key; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        if (value) onEnable(); else onDisable();
    }

    protected void onEnable() { }
    protected void onDisable() { }
}
