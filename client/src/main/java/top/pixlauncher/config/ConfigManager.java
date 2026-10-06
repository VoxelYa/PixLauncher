package top.pixlauncher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.settings.Setting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON config with named profiles (user decision: multiple profiles with
 * switch / import / export). Files live under .minecraft/pixlauncher-client/.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String activeProfile = "default";

    private ConfigManager() { }

    private static File dir() {
        File gameDir = top.pixlauncher.util.Paths.gameDir();
        File dir = new File(gameDir, "pixlauncher-client" + File.separator + "profiles");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static String activeProfile() { return activeProfile; }

    public static List<String> profiles() {
        List<String> names = new ArrayList<String>();
        File[] files = dir().listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) for (File f : files) names.add(f.getName().replace(".json", ""));
        if (!names.contains("default")) names.add(0, "default");
        return names;
    }

    public static void save() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        Map<String, Object> mods = new LinkedHashMap<String, Object>();
        for (Module m : ModuleManager.all()) {
            Map<String, Object> mo = new LinkedHashMap<String, Object>();
            mo.put("enabled", m.isEnabled());
            mo.put("key", m.keyBind());
            Map<String, Object> st = new LinkedHashMap<String, Object>();
            for (Setting<?> s : m.settings()) st.put(s.id(), String.valueOf(s.get()));
            mo.put("settings", st);
            mods.put(m.id(), mo);
        }
        root.put("modules", mods);
        root.put("hud", HudConfigIo.capture());
        try (FileWriter w = new FileWriter(new File(dir(), activeProfile + ".json"))) {
            GSON.toJson(root, w);
        } catch (Exception broken) {
            System.out.println("[PixLauncher] config save failed: " + broken);
        }
    }

    @SuppressWarnings("unchecked")
    public static void load(String profile) {
        activeProfile = profile;
        File f = new File(dir(), profile + ".json");
        if (!f.exists()) return;
        try (FileReader r = new FileReader(f)) {
            Map<String, Object> root = GSON.fromJson(r, Map.class);
            Map<String, Object> mods = (Map<String, Object>) root.get("modules");
            if (mods != null) {
                for (Module m : ModuleManager.all()) {
                    Object raw = mods.get(m.id());
                    if (!(raw instanceof Map)) continue;
                    Map<String, Object> mo = (Map<String, Object>) raw;
                    m.setEnabled(Boolean.TRUE.equals(mo.get("enabled")));
                    Object key = mo.get("key");
                    if (key instanceof Number) m.setKeyBind(((Number) key).intValue());
                    Map<String, Object> st = (Map<String, Object>) mo.get("settings");
                    if (st == null) continue;
                    for (Setting<?> s : m.settings()) {
                        Object v = st.get(s.id());
                        if (v == null) continue;
                        applySetting(s, String.valueOf(v));
                    }
                }
            }
            Object hud = root.get("hud");
            if (hud instanceof Map) HudConfigIo.restore((Map<String, Object>) hud);
        } catch (Exception broken) {
            System.out.println("[PixLauncher] config load failed: " + broken);
        }
    }

    private static void applySetting(Setting<?> setting, String raw) {
        try {
            if (setting instanceof top.pixlauncher.module.settings.BooleanSetting) {
                ((top.pixlauncher.module.settings.BooleanSetting) setting).set(Boolean.parseBoolean(raw));
            } else if (setting instanceof top.pixlauncher.module.settings.Settings.Num) {
                ((top.pixlauncher.module.settings.Settings.Num) setting).set(Float.parseFloat(raw));
            } else if (setting instanceof top.pixlauncher.module.settings.Settings.Mode) {
                ((top.pixlauncher.module.settings.Settings.Mode) setting).set(raw);
            } else if (setting instanceof top.pixlauncher.module.settings.Settings.Bind) {
                ((top.pixlauncher.module.settings.Settings.Bind) setting).set(Integer.parseInt(raw));
            } else if (setting instanceof top.pixlauncher.module.settings.Settings.Col) {
                ((top.pixlauncher.module.settings.Settings.Col) setting).set((int) Long.parseLong(raw));
            }
        } catch (NumberFormatException bad) {
            System.out.println("[PixLauncher] bad setting value " + setting.id() + "=" + raw);
        }
    }
}
