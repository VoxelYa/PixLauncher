package top.pixlauncher.module.modules;

import net.minecraft.entity.item.EntityTNTPrimed;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import top.pixlauncher.events.Events;
import top.pixlauncher.hud.HudManager;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.settings.Settings;
import top.pixlauncher.util.MC;
import top.pixlauncher.util.render.Render2D;

import java.util.ArrayDeque;
import java.util.Deque;

import static top.pixlauncher.util.render.Render2D.rect;
import static top.pixlauncher.util.render.Render2D.text;
import static top.pixlauncher.util.render.Render2D.textWidth;

/** HUD widgets, second half. */
public final class HudModules2 {
    private HudModules2() { }

    // ---------------------------------------------------------------- 11
    static final class ReachDisplay extends Module implements HudManager.Widget {
        private double lastReach = -1;

        ReachDisplay() { super("reachdisplay", "Reach Display", Category.HUD); }

        @Override protected void onEnable() {
            Events.subscribe(Events.Attack.class, e -> {
                if (MC.player() != null) lastReach = MC.player().func_70011_f(e.target.field_70165_t, e.target.field_70163_u, e.target.field_70161_v);
            });
        }

        @Override public void render(float x, float y) {
            String s = lastReach < 0 ? "Reach -" : String.format("Reach %.2f", lastReach);
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line(lastReach < 0 ? "Reach -" : String.format("Reach %.2f", lastReach)) + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 12
    static final class DamageIndicatorHud extends Module implements HudManager.Widget {
        private double lastDamage = -1;

        DamageIndicatorHud() { super("damagehud", "Damage Indicator", Category.HUD); }

        @Override protected void onEnable() {
            Events.subscribe(Events.Attack.class, e -> {
                double health = e.target instanceof net.minecraft.entity.EntityLivingBase ? ((net.minecraft.entity.EntityLivingBase) e.target).func_110143_aJ() : -1;
                if (health > 0) lastDamage = health;
            });
        }

        @Override public void render(float x, float y) {
            String s = lastDamage < 0 ? "Dmg -" : String.format("Dmg %.1f", lastDamage);
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, 0xFFFF6060);
        }
        @Override public float width() { return HudModules.line(lastDamage < 0 ? "Dmg -" : String.format("Dmg %.1f", lastDamage)) + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 13
    static final class ToggleSneak extends Module implements HudManager.Widget {
        final Settings.Bind toggleKey = new Settings.Bind("key", "Toggle key", Keyboard.KEY_C);
        private boolean sneaking;

        ToggleSneak() {
            super("togglesneak", "Toggle Sneak", Category.HUD);
            register(toggleKey);
            Events.subscribe(Events.Tick.class, e -> { if (isEnabled()) apply(); });
        }

        @Override protected void onEnable() {
            Events.subscribe(Events.Key.class, e -> {
                if (toggleKey.matches(e.key)) sneaking = !sneaking;
            });
        }

        @Override protected void onDisable() { sneaking = false; }

        @Override public void render(float x, float y) {
            String s = "Sneak [" + (sneaking ? "On" : "Off") + "]";
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, sneaking ? HudModules.ACCENT : HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line("Sneak [On]") + 4; }
        @Override public float height() { return 12; }

        void apply() {
            if (MC.player() != null) MC.player().func_70095_a(sneaking || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
        }
    }

    // ---------------------------------------------------------------- 14
    static final class SprintModule extends Module implements HudManager.Widget {
        final Settings.BooleanSetting keepSprint = new Settings.BooleanSetting("keep", "Keep sprint", true);

        SprintModule() {
            super("sprint", "Sprint", Category.HUD);
            register(keepSprint);
            Events.subscribe(Events.Tick.class, e -> { if (isEnabled()) apply(); });
        }

        @Override public void render(float x, float y) {
            boolean moving = MC.player() != null && (MC.player().field_70159_w * MC.player().field_70159_w + MC.player().field_70179_y * MC.player().field_70179_y) > 0.01;
            boolean sprinting = isEnabled() && moving;
            String s = "Sprint [" + (sprinting ? "On" : "Off") + "]";
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, sprinting ? HudModules.ACCENT : HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line("Sprint [On]") + 4; }
        @Override public float height() { return 12; }

        void apply() {
            if (MC.player() == null) return;
            boolean moving = MC.player().field_70159_w * MC.player().field_70159_w + MC.player().field_70179_y * MC.player().field_70179_y > 0.008;
            if (keepSprint.get() && moving && !MC.player().func_70093_af()) MC.player().func_70031_b(true);
        }
    }

    // ---------------------------------------------------------------- 15
    static final class ModsList extends Module implements HudManager.Widget {
        final Settings.BooleanSetting showTitle = new Settings.BooleanSetting("title", "Show title", true);

        ModsList() {
            super("modslist", "Mod List", Category.HUD);
            register(showTitle);
        }

        @Override public void render(float x, float y) {
            StringBuilder sb = new StringBuilder();
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled() && m != this) sb.append(m.name()).append(", ");
            }
            String list = sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "none";
            if (showTitle.get()) {
                text("PixLauncher", x + 1, y + 1, HudModules.ACCENT);
                text(list, x + 1, y + 12, HudModules.WHITE);
            } else {
                text(list, x + 1, y + 1, HudModules.WHITE);
            }
        }
        @Override public float width() {
            float w = showTitle.get() ? textWidth("PixLauncher") : 20;
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled() && m != this) w = Math.max(w, textWidth(m.name()));
            }
            return w + 2;
        }
        @Override public float height() {
            int enabled = 0;
            for (Module m : ModuleManager.all()) if (m.isEnabled() && m != this) enabled++;
            return (showTitle.get() ? 12 : 2) + Math.max(1, enabled) * 10;
        }
    }

    // ---------------------------------------------------------------- 16
    static final class ServerAddressDisplay extends Module implements HudManager.Widget {
        ServerAddressDisplay() { super("serveraddress", "Server Address", Category.HUD); }

        private String address() {
            if (MC.mc().func_147104_D() != null) return MC.mc().func_147104_D().field_78845_b;
            return "Singleplayer";
        }

        @Override public void render(float x, float y) {
            String s = address();
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line(address()) + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 17
    static final class TntTimerHud extends Module implements HudManager.Widget {
        TntTimerHud() { super("tnttimerhud", "TNT Timer", Category.HUD); }

        private EntityTNTPrimed nearest() {
            if (MC.world() == null) return null;
            EntityTNTPrimed best = null;
            double bestDist = Double.MAX_VALUE;
            for (Object o : MC.world().field_72996_f) {
                if (!(o instanceof EntityTNTPrimed)) continue;
                EntityTNTPrimed tnt = (EntityTNTPrimed) o;
                double d = MC.player().func_70011_f(tnt.field_70165_t, tnt.field_70163_u, tnt.field_70161_v);
                if (d < bestDist) { bestDist = d; best = tnt; }
            }
            return best;
        }

        @Override public void render(float x, float y) {
            EntityTNTPrimed tnt = nearest();
            String s = tnt == null ? "No TNT" : "TNT " + tnt.field_70516_a / 20.0 + "s";
            rect(x, y, HudModules.line(s) + 4, 12, HudModules.BG);
            text(s, x + 2, y + 2, tnt != null && tnt.field_70516_a <= 20 ? 0xFFFF6060 : HudModules.WHITE);
        }
        @Override public float width() {
            EntityTNTPrimed tnt = nearest();
            return HudModules.line(tnt == null ? "No TNT" : "TNT " + tnt.field_70516_a / 20.0 + "s") + 4;
        }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 18
    static final class BetterChatModule extends Module implements HudManager.Widget {
        final Settings.BooleanSetting fold = new Settings.BooleanSetting("fold", "Fold repeats", true);

        BetterChatModule() {
            super("betterchat", "Better Chat", Category.HUD);
            register(fold);
        }

        @Override public void render(float x, float y) {
            rect(x, y, HudModules.line("Chat+") + 4, 12, HudModules.BG);
            text("Chat+", x + 2, y + 2, HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line("Chat+") + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 19
    static final class BetterScreenModule extends Module implements HudManager.Widget {
        final Settings.Num strength = new Settings.Num("strength", "Dim strength", 0.4f, 0.1f, 0.8f, 0.05f);

        BetterScreenModule() {
            super("betterscreen", "Better Screens", Category.HUD);
            register(strength);
        }

        @Override public void render(float x, float y) {
            rect(x, y, HudModules.line("Screens+") + 4, 12, HudModules.BG);
            text("Screens+", x + 2, y + 2, HudModules.WHITE);
        }
        @Override public float width() { return HudModules.line("Screens+") + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 20
    static final class PerformanceHud extends Module implements HudManager.Widget {
        final Settings.BooleanSetting showGraph = new Settings.BooleanSetting("graph", "FPS graph", true);
        final Deque<Integer> history = new ArrayDeque<Integer>();

        PerformanceHud() {
            super("perfhud", "Performance HUD", Category.HUD);
            register(showGraph);
        }

        @Override public void render(float x, float y) {
            int fps = MC.debugFps();
            history.addLast(fps);
            if (history.size() > 60) history.pollFirst();
            Runtime rt = Runtime.getRuntime();
            String mem = (rt.totalMemory() - rt.freeMemory()) / 1048576 + "/" + rt.maxMemory() / 1048576 + " MB";
            String line1 = fps + " FPS";
            float w = Math.max(HudModules.line(line1), HudModules.line(mem)) + 8;
            float h = 24 + (showGraph.get() ? 26 : 0);
            rect(x, y, w, h, HudModules.BG);
            text(line1, x + 4, y + 3, HudModules.WHITE);
            text(mem, x + 4, y + 14, 0xFFB8C0D8);
            if (showGraph.get()) {
                float gx = x + 4, gy = y + 26, gw = w - 8, gh = 22;
                rect(gx, gy, gw, gh, 0xFF080A10);
                int i = 0;
                int prev = -1;
                for (int v : history) {
                    float px = gx + i * (gw / 60.0f);
                    int barH = Math.max(1, (int) (Math.min(1.0f, v / 240.0f) * gh));
                    rect(px, gy + gh - barH, Math.max(1, gw / 60.0f), barH, v < 60 ? 0xFFFF6060 : HudModules.ACCENT);
                    prev = v;
                    i++;
                }
            }
        }
        @Override public float width() { return 90; }
        @Override public float height() { return showGraph.get() ? 50 : 24; }
    }

    // ------------------------------------------------------------ registry
    public static void registerAll() {
        top.pixlauncher.module.ModuleManager.add(new ReachDisplay());
        top.pixlauncher.module.ModuleManager.add(new DamageIndicatorHud());
        top.pixlauncher.module.ModuleManager.add(new ToggleSneak());
        top.pixlauncher.module.ModuleManager.add(new SprintModule());
        top.pixlauncher.module.ModuleManager.add(new ModsList());
        top.pixlauncher.module.ModuleManager.add(new ServerAddressDisplay());
        top.pixlauncher.module.ModuleManager.add(new TntTimerHud());
        top.pixlauncher.module.ModuleManager.add(new BetterChatModule());
        top.pixlauncher.module.ModuleManager.add(new BetterScreenModule());
        top.pixlauncher.module.ModuleManager.add(new PerformanceHud());
    }
}
