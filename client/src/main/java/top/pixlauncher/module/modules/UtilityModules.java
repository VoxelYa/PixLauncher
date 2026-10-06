package top.pixlauncher.module.modules;

import net.minecraft.client.audio.SoundCategory;
import org.lwjgl.input.Keyboard;
import top.pixlauncher.events.Events;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.settings.Settings;
import top.pixlauncher.util.MC;

import java.util.HashMap;
import java.util.Map;

import static top.pixlauncher.util.render.Render2D.rect;
import static top.pixlauncher.util.render.Render2D.text;
import static top.pixlauncher.util.render.Render2D.textWidth;

/** Utility modules (8). */
public final class UtilityModules {
    private UtilityModules() { }

    private static final int BG = HudModules.BG;
    private static final int WHITE = HudModules.WHITE;
    private static final int ACCENT = HudModules.ACCENT;

    private static boolean flag(String id) {
        Module m = ModuleManager.byId(id);
        return m != null && m.isEnabled();
    }

    // ---------------------------------------------------------------- 39
    public static final class ClientSettings extends Module {
        public final Settings.BooleanSetting animations = new Settings.BooleanSetting("anim", "Interface animations", true);

        public ClientSettings() {
            super("clientsettings", "Client Settings", Category.CORE);
            register(animations);
        }

        @Override protected void init() { setEnabled(true); } // core module, always on

        public void render(float x, float y) {
            rect(x, y, textWidth("Core") + 4, 12, BG);
            text("Core", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Core") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 40
    public static final class AutoGG extends Module {
        private long lastSent;

        AutoGG() { super("autogg", "Auto GG", Category.UTILITY); }

        @Override protected void onEnable() {
            Events.subscribe(Events.ChatLine.class, e -> {
                String text = e.message.func_150260_c();
                if (System.currentTimeMillis() - lastSent < 10_000) return;
                String lower = text.toLowerCase();
                if (lower.contains("you won") || lower.contains("victory") || lower.contains("game over")
                        || lower.contains("you have won") || lower.contains("win!")) {
                    if (MC.player() != null) {
                        MC.player().func_71165_d("gg");
                        lastSent = System.currentTimeMillis();
                    }
                }
            });
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Auto GG") + 4, 12, BG);
            text("Auto GG", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Auto GG") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 41
    public static final class AutoText extends Module {
        public final Settings.Bind key1 = new Settings.Bind("key1", "Message 1 key", Keyboard.KEY_NONE);
        public final Settings.Text text1 = new Settings.Text("text1", "Message 1", "gl hf");
        public final Settings.Bind key2 = new Settings.Bind("key2", "Message 2 key", Keyboard.KEY_NONE);
        public final Settings.Text text2 = new Settings.Text("text2", "Message 2", "gg");

        AutoText() {
            super("autotext", "Quick Messages", Category.UTILITY);
            register(key1); register(text1); register(key2); register(text2);
        }

        @Override protected void onEnable() {
            Events.subscribe(Events.Key.class, e -> {
                if (MC.player() == null) return;
                if (key1.matches(e.key)) MC.player().func_71165_d(text1.get());
                if (key2.matches(e.key)) MC.player().func_71165_d(text2.get());
            });
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Quick Msg") + 4, 12, BG);
            text("Quick Msg", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Quick Msg") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 42
    public static final class Nametags extends Module {
        public final Settings.BooleanSetting background = new Settings.BooleanSetting("bg", "Background", true);
        public final Settings.BooleanSetting hideNpcs = new Settings.BooleanSetting("hidenpcs", "Hide NPCs", false);

        Nametags() {
            super("nametags", "Nametags", Category.UTILITY);
            register(background); register(hideNpcs);
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Nametags") + 4, 12, BG);
            text("Nametags", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Nametags") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 43
    public static final class SoundModifier extends Module {
        public final Settings.Num liquid = new Settings.Num("liquid", "Water", 1f, 0f, 1f, 0.05f);
        public final Settings.Num dig = new Settings.Num("dig", "Dig", 1f, 0f, 1f, 0.05f);
        public final Settings.Num game = new Settings.Num("game", "Game", 1f, 0f, 1f, 0.05f);
        public final Settings.Num hostile = new Settings.Num("hostile", "Mobs", 1f, 0f, 1f, 0.05f);
        private final Map<SoundCategory, Float> applied = new HashMap<SoundCategory, Float>();

        SoundModifier() {
            super("soundmodifier", "Sound Control", Category.UTILITY);
            register(liquid); register(dig); register(game); register(hostile);
        }

        void apply() {
            applyCat(SoundCategory.WEATHER, liquid.get());
            applyCat(SoundCategory.BLOCKS, dig.get());
            applyCat(SoundCategory.PLAYERS, game.get());
            applyCat(SoundCategory.MOBS, hostile.get());
        }

        private void applyCat(SoundCategory cat, float volume) {
            Float last = applied.get(cat);
            if (last != null && last == volume) return;
            applied.put(cat, volume);
            try {
                // SoundManager sits behind a private field; srg name is stable in production
                java.lang.reflect.Field f = net.minecraft.client.audio.SoundHandler.class.getDeclaredField("field_147694_f");
                f.setAccessible(true);
                net.minecraft.client.audio.SoundManager sm = (net.minecraft.client.audio.SoundManager) f.get(MC.mc().func_147118_V());
                sm.func_148601_a(cat, volume);
            } catch (Throwable broken) {
                System.out.println("[PixLauncher] sound apply failed: " + broken);
            }
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Sound") + 4, 12, BG);
            text("Sound", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Sound") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 44
    public static final class TimeChanger extends Module {
        public final Settings.Mode time = new Settings.Mode("time", "Time", Settings.options("Day", "Sunset", "Night"), "Day");

        TimeChanger() {
            super("timechanger", "Time Changer", Category.UTILITY);
            register(time);
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Time: " + time.get()) + 4, 12, BG);
            text("Time: " + time.get(), x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Time: " + time.get()) + 4; }
        public float height() { return 12; }

        public long overrideTicks() {
            String t = time.get();
            return t.equals("Night") ? 18000L : t.equals("Sunset") ? 12500L : 1000L;
        }
    }

    // ---------------------------------------------------------------- 45
    public static final class ParticlesModifier extends Module {
        public final Settings.BooleanSetting fewerBreak = new Settings.BooleanSetting("fewer", "Fewer break particles", true);

        ParticlesModifier() {
            super("particlesmodifier", "Particle Control", Category.UTILITY);
            register(fewerBreak);
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Particles") + 4, 12, BG);
            text("Particles", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Particles") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 46
    public static final class TntTimer extends Module {
        public final Settings.Num warnAt = new Settings.Num("warn", "Warn below (s)", 1.0f, 0.2f, 5f, 0.1f);

        TntTimer() {
            super("tnttimer", "TNT Timer (logic)", Category.UTILITY);
            register(warnAt);
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("TNT logic") + 4, 12, BG);
            text("TNT logic", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("TNT logic") + 4; }
        public float height() { return 12; }
    }

    // ------------------------------------------------------------ registry
    public static void registerAll() {
        ModuleManager.add(new ClientSettings());
        ModuleManager.add(new AutoGG());
        ModuleManager.add(new AutoText());
        ModuleManager.add(new Nametags());
        ModuleManager.add(new SoundModifier());
        ModuleManager.add(new TimeChanger());
        ModuleManager.add(new ParticlesModifier());
        ModuleManager.add(new TntTimer());

        Events.subscribe(Events.Tick.class, e -> {
            if (flag("soundmodifier")) ((SoundModifier) ModuleManager.byId("soundmodifier")).apply();
        });
    }
}
