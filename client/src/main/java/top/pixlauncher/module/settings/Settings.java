package top.pixlauncher.module.settings;

import java.util.Arrays;
import java.util.List;

/**
 * Setting implementations beyond the boolean one. All are plain data with
 * getters/setters — the ClickGUI renders them generically off Setting base.
 */
public final class Settings {
    private Settings() { }

    public static final class Num extends Setting<Float> {
        public final float min, max, step;
        public Num(String id, String label, float def, float min, float max, float step) {
            super(id, label, def);
            this.min = min; this.max = max; this.step = step;
        }
        public int percent() {
            return (int) Math.round(((get() - min) / (max - min)) * 100.0);
        }
    }

    public static final class Mode extends Setting<String> {
        public final List<String> options;
        public Mode(String id, String label, List<String> options, String def) {
            super(id, label, def);
            this.options = options;
        }
        public void cycle() {
            int i = options.indexOf(get());
            set(options.get((i + 1) % options.size()));
        }
    }

    public static final class Bind extends Setting<Integer> {
        public Bind(String id, String label, int defaultKey) {
            super(id, label, defaultKey);
        }
        public boolean matches(int key) { return get() != 0 && get() == key; }
    }

    /** RGBA packed color; ClickGUI edits channels via sliders in v1. */
    public static final class Col extends Setting<Integer> {
        public Col(String id, String label, int rgba) {
            super(id, label, rgba);
        }
        public int r() { return (get() >> 16) & 0xFF; }
        public int g() { return (get() >> 8) & 0xFF; }
        public int b() { return get() & 0xFF; }
        public int a() { return (get() >>> 24) & 0xFF; }
    }

    public static final class Text extends Setting<String> {
        public Text(String id, String label, String defaultValue) {
            super(id, label, defaultValue);
        }
    }

    /** re-export so feature files can use Settings.BooleanSetting uniformly */
    public static class BooleanSetting extends top.pixlauncher.module.settings.BooleanSetting {
        public BooleanSetting(String id, String label, boolean defaultValue) {
            super(id, label, defaultValue);
        }
    }

    public static List<String> options(String... values) {
        return Arrays.asList(values);
    }
}
