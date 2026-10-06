package top.pixlauncher.module;

import top.pixlauncher.module.settings.Setting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry + lifecycle for all modules. Feature files call add() from their
 * static registerAll(); init() runs after every module is registered.
 */
public final class ModuleManager {

    private static final List<Module> modules = new ArrayList<Module>();
    private static boolean initialized;

    private ModuleManager() { }

    public static void add(Module module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            if (Setting.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    module.register((Setting<?>) field.get(module));
                } catch (IllegalAccessException broken) {
                    throw new IllegalStateException("cannot read setting field", broken);
                }
            }
        }
        modules.add(module);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        for (Module m : modules) m.init();
        System.out.println("[PixLauncher] initialized " + modules.size() + " modules");
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public static Module byId(String id) {
        for (Module m : modules) if (m.id().equals(id)) return m;
        return null;
    }
}
