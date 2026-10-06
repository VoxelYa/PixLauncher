package top.pixlauncher.module.modules;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import top.pixlauncher.events.Events;
import top.pixlauncher.hud.HudManager;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.settings.Settings;
import top.pixlauncher.util.MC;
import top.pixlauncher.util.render.Render2D;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import static top.pixlauncher.util.render.Render2D.rect;
import static top.pixlauncher.util.render.Render2D.text;
import static top.pixlauncher.util.render.Render2D.textWidth;

/**
 * HUD widgets, first half. Every module doubles as its own widget
 * (HudManager.Widget); positions/scaling live in HudManager placements.
 */
public final class HudModules {
    private HudModules() { }

    static final int BG = 0xA0101218;
    static final int ACCENT = 0xFF4F6EF7;
    static final int WHITE = 0xFFFFFFFF;

    /** shared click timestamps for CPS modules */
    static final Deque<Long> LEFT_CLICKS = new ArrayDeque<Long>();
    static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<Long>();
    static boolean lastLeft, lastRight;

    static int cps(Deque<Long> clicks) {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        return clicks.size();
    }

    static void trackClicks() {
        boolean left = Mouse.isButtonDown(0);
        boolean right = Mouse.isButtonDown(1);
        long now = System.currentTimeMillis();
        if (left && !lastLeft) LEFT_CLICKS.addLast(now);
        if (right && !lastRight) RIGHT_CLICKS.addLast(now);
        lastLeft = left;
        lastRight = right;
    }

    static float line(String s) { return textWidth(s) + 4; }

    // ---------------------------------------------------------------- 1
    static final class FpsDisplay extends Module implements HudManager.Widget {
        FpsDisplay() { super("fpsdisplay", "FPS Display", Category.HUD); }

        @Override public void render(float x, float y) {
            String s = MC.debugFps() + " FPS";
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, WHITE);
        }
        @Override public float width() { return line(MC.debugFps() + " FPS") + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 2
    static final class CpsDisplay extends Module implements HudManager.Widget {
        CpsDisplay() { super("cpsdisplay", "CPS Display", Category.HUD); }

        @Override public void render(float x, float y) {
            String s = "CPS " + cps(LEFT_CLICKS) + " | " + cps(RIGHT_CLICKS);
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, WHITE);
        }
        @Override public float width() { return line("CPS " + cps(LEFT_CLICKS) + " | " + cps(RIGHT_CLICKS)) + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 3
    static final class Keystrokes extends Module implements HudManager.Widget {
        final Settings.Col pressed = new Settings.Col("pressed", "Pressed color", 0xFF4F6EF7);
        final Settings.BooleanSetting showSpace = new Settings.BooleanSetting("space", "Show spacebar", true);

        Keystrokes() {
            super("keystrokes", "Keystrokes", Category.HUD);
            register(pressed);
            register(showSpace);
        }

        private void key(float x, float y, float w, float h, String label, int keyCode) {
            boolean down = keyCode == -1 ? lastLeft : (keyCode == -2 ? Mouse.isButtonDown(1) : Keyboard.isKeyDown(keyCode));
            rect(x, y, w, h, down ? pressed.get() : BG);
            int tw = (int) textWidth(label);
            text(label, x + (w - tw) / 2, y + (h - 8) / 2, WHITE);
        }

        @Override public void render(float x, float y) {
            float s = 18, g = 2;
            key(x, y, s, s, "W", Keyboard.KEY_W);
            key(x, y + s + g, s, s, "A", Keyboard.KEY_A);
            key(x + s + g, y + s + g, s, s, "S", Keyboard.KEY_S);
            key(x + (s + g) * 2, y + s + g, s, s, "D", Keyboard.KEY_D);
            key(x, y + (s + g) * 2, s, s, "L", -1);
            key(x + s + g, y + (s + g) * 2, s, s, "R", -2);
            if (showSpace.get()) {
                rect(x, y + (s + g) * 3, s * 3 + g * 2, 6, Keyboard.isKeyDown(Keyboard.KEY_SPACE) ? pressed.get() : BG);
            }
        }
        @Override public float width() { return 18 * 3 + 2 * 2; }
        @Override public float height() { return showSpace.get() ? 18 * 3 + 2 * 2 + 8 : 18 * 3 + 2 * 2; }
    }

    // ---------------------------------------------------------------- 4
    static final class PingDisplay extends Module implements HudManager.Widget {
        PingDisplay() { super("pingdisplay", "Ping Display", Category.HUD); }

        private int ping() {
            if (MC.player() == null || MC.player().field_71174_a == null) return 0;
            NetworkPlayerInfo info = MC.player().field_71174_a.func_175102_a(MC.player().func_110124_au());
            return info == null ? 0 : info.func_178853_c();
        }

        @Override public void render(float x, float y) {
            String s = ping() + " ms";
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, WHITE);
        }
        @Override public float width() { return line(ping() + " ms") + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 5
    static final class CoordsDisplay extends Module implements HudManager.Widget {
        CoordsDisplay() { super("coordsdisplay", "Coords Display", Category.HUD); }

        @Override public void render(float x, float y) {
            if (MC.player() == null) return;
            double px = MC.player().field_70165_t, py = MC.player().field_70163_u, pz = MC.player().field_70161_v;
            String s = "XYZ " + (int) px + " / " + (int) py + " / " + (int) pz;
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, WHITE);
        }
        @Override public float width() {
            if (MC.player() == null) return 40;
            double px = MC.player().field_70165_t, py = MC.player().field_70163_u, pz = MC.player().field_70161_v;
            return line("XYZ " + (int) MC.player().field_70165_t + " / " + (int) MC.player().field_70163_u + " / " + (int) MC.player().field_70161_v) + 4;
        }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 6
    static final class ComboDisplay extends Module implements HudManager.Widget {
        private int combo;
        private long lastHit;

        ComboDisplay() { super("combodisplay", "Combo Display", Category.HUD); }

        @Override protected void onEnable() {
            Events.subscribe(Events.Attack.class, e -> {
                combo++;
                lastHit = System.currentTimeMillis();
            });
        }

        @Override public void render(float x, float y) {
            if (System.currentTimeMillis() - lastHit > 2000 && combo != 0) combo = 0;
            String s = combo + " Combo";
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, combo > 2 ? ACCENT : WHITE);
        }
        @Override public float width() { return line(combo + " Combo") + 4; }
        @Override public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 7
    static final class ArmorDisplay extends Module implements HudManager.Widget {
        ArmorDisplay() { super("armordisplay", "Armor Display", Category.HUD); }

        @Override public void render(float x, float y) {
            if (MC.player() == null) return;
            float cy = y;
            for (int i = 3; i >= 0; i--) {
                net.minecraft.item.ItemStack stack = MC.player().field_71071_by.field_70460_b[i];
                if (stack == null) continue;
                int max = stack.func_77960_j();
                String s = stack.func_151000_E() + (max > 0 ? " " + (stack.func_77952_i() * 100 / max) + "%" : "");
                rect(x, cy, line(s) + 4, 12, BG);
                text(s, x + 2, cy + 2, WHITE);
                cy += 12;
            }
        }
        @Override public float width() {
            float w = 40;
            if (MC.player() != null) {
                for (int i = 0; i < 4; i++) {
                    net.minecraft.item.ItemStack stack = MC.player().field_71071_by.field_70460_b[i];
                    if (stack == null) continue;
                    int max = stack.func_77960_j();
                    String s = stack.func_151000_E() + (max > 0 ? " " + (stack.func_77952_i() * 100 / max) + "%" : "");
                    w = Math.max(w, line(s) + 4);
                }
            }
            return w;
        }
        @Override public float height() {
            int n = 0;
            if (MC.player() != null) for (int i = 0; i < 4; i++) if (MC.player().field_71071_by.field_70460_b[i] != null) n++;
            return Math.max(1, n) * 12;
        }
    }

    // ---------------------------------------------------------------- 8
    static final class PotionDisplay extends Module implements HudManager.Widget {
        PotionDisplay() { super("potiondisplay", "Potion Status", Category.HUD); }

        @Override public void render(float x, float y) {
            if (MC.player() == null) return;
            float cy = y;
            for (Object o : MC.player().func_70651_bq()) {
                PotionEffect e = (PotionEffect) o;
                String s = e.func_76453_d() + " " + (e.func_76459_b() / 20) + "s";
                rect(x, cy, line(s) + 4, 12, BG);
                text(s, x + 2, cy + 2, WHITE);
                cy += 12;
            }
        }
        @Override public float width() {
            float w = 40;
            if (MC.player() != null) {
                for (Object o : MC.player().func_70651_bq()) {
                    PotionEffect e = (PotionEffect) o;
                    w = Math.max(w, line(e.func_76453_d() + " " + (e.func_76459_b() / 20) + "s") + 4);
                }
            }
            return w;
        }
        @Override public float height() {
            return MC.player() == null ? 1 : Math.max(1, MC.player().func_70651_bq().size()) * 12;
        }
    }

    // ---------------------------------------------------------------- 9
    static final class ScoreboardWidget extends Module implements HudManager.Widget {
        final Settings.BooleanSetting hideNumbers = new Settings.BooleanSetting("nonumbers", "Hide numbers", true);

        ScoreboardWidget() {
            super("scoreboard", "Scoreboard", Category.HUD);
            register(hideNumbers);
        }

        @Override public void render(float x, float y) {
            if (MC.player() == null) return;
            ScoreObjective obj = MC.player().func_96123_co().func_96539_a(1);
            if (obj == null) return;
            List<Score> scores = new ArrayList<Score>(MC.player().func_96123_co().func_96534_i(obj));
            Collections.sort(scores, new Comparator<Score>() {
                public int compare(Score a, Score b) { return b.func_96652_c() - a.func_96652_c(); }
            });
            if (scores.size() > 15) scores = scores.subList(0, 15);

            String title = obj.func_96678_d();
            float w = line(title) + 6;
            List<String> lines = new ArrayList<String>();
            for (Score s : scores) {
                String name = s.func_96653_e();
                String value = hideNumbers.get() ? "" : String.valueOf(s.func_96652_c());
                lines.add(name + (value.isEmpty() ? "" : "  " + value));
                w = Math.max(w, line(lines.get(lines.size() - 1)) + 6);
            }
            float h = 14 + lines.size() * 11;
            rect(x, y, w, h, 0x90101218);
            rect(x, y, w, 14, BG);
            text(title, x + 3, y + 3, WHITE);
            float cy = y + 14;
            for (String line : lines) {
                text(line, x + 3, cy + 2, 0xFFD0D4E0);
                cy += 11;
            }
        }
        @Override public float width() { return 80; }
        @Override public float height() { return 14; }
    }

    // --------------------------------------------------------------- 10
    static final class TabWidget extends Module implements HudManager.Widget {
        TabWidget() { super("taboverlay", "Tab Info", Category.HUD); }

        @Override public void render(float x, float y) {
            if (MC.player() == null || MC.player().field_71174_a == null) return;
            java.util.Collection<NetworkPlayerInfo> infos = MC.player().field_71174_a.func_175106_d();
            String s = infos.size() + " players online";
            rect(x, y, line(s) + 4, 12, BG);
            text(s, x + 2, y + 2, WHITE);
        }
        @Override public float width() {
            if (MC.player() == null || MC.player().field_71174_a == null) return 60;
            return line(MC.player().field_71174_a.func_175106_d().size() + " players online") + 4;
        }
        @Override public float height() { return 12; }
    }

    // ------------------------------------------------------------ registry
    public static void registerAll() {
        top.pixlauncher.module.ModuleManager.add(new FpsDisplay());
        top.pixlauncher.module.ModuleManager.add(new CpsDisplay());
        top.pixlauncher.module.ModuleManager.add(new Keystrokes());
        top.pixlauncher.module.ModuleManager.add(new PingDisplay());
        top.pixlauncher.module.ModuleManager.add(new CoordsDisplay());
        top.pixlauncher.module.ModuleManager.add(new ComboDisplay());
        top.pixlauncher.module.ModuleManager.add(new ArmorDisplay());
        top.pixlauncher.module.ModuleManager.add(new PotionDisplay());
        top.pixlauncher.module.ModuleManager.add(new ScoreboardWidget());
        top.pixlauncher.module.ModuleManager.add(new TabWidget());
    }
}
