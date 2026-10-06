package top.pixlauncher.module.modules;

import org.lwjgl.input.Keyboard;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.settings.Settings;
import top.pixlauncher.util.MC;

import static top.pixlauncher.util.render.Render2D.rect;
import static top.pixlauncher.util.render.Render2D.text;
import static top.pixlauncher.util.render.Render2D.textWidth;

/**
 * Optimize modules. Forced-on lossless engine work lives in the bootstrap's
 * PerformanceTransformer; these four are the toggleable/behavioural pieces.
 */
public final class OptimizeModules {
    private OptimizeModules() { }

    private static final int BG = HudModules.BG;
    private static final int WHITE = HudModules.WHITE;

    private static boolean flag(String id) {
        Module m = ModuleManager.byId(id);
        return m != null && m.isEnabled();
    }

    // ---------------------------------------------------------------- 35*
    /** Forced-on (user decision): this module cannot be disabled. */
    public static final class Performance extends Module {
        public final Settings.Num standsDist = new Settings.Num("standsDist", "Armor stand distance", 32f, 4f, 96f, 4f);
        public final Settings.Num itemsDist = new Settings.Num("itemsDist", "Dropped item distance", 28f, 4f, 96f, 4f);
        public final Settings.Num framesDist = new Settings.Num("framesDist", "Item frame distance", 32f, 4f, 96f, 4f);
        public final Settings.Num mobsDist = new Settings.Num("mobsDist", "Mob distance", 32f, 4f, 96f, 4f);
        public final Settings.Num tesrDist = new Settings.Num("tesrDist", "Tile-entity distance", 32f, 4f, 96f, 4f);

        Performance() {
            super("performance", "Performance (always on)", Category.OPTIMIZE);
            register(standsDist); register(itemsDist); register(framesDist);
            register(mobsDist); register(tesrDist);
        }

        @Override public void setEnabled(boolean value) {
            super.setEnabled(true); // forced
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Performance") + 4, 12, BG);
            text("Performance", x + 2, y + 2, ACCENT_ON());
        }
        public float width() { return textWidth("Performance") + 4; }
        public float height() { return 12; }

        /** true = allow the render path (distance culling for non-player entities). */
        public boolean entityRenderAllowed(net.minecraft.entity.Entity e) {
            if (!MC.inGame() || MC.player() == null || e == MC.player()) return true;
            if (e instanceof net.minecraft.entity.player.EntityPlayer) return true; // PvP always renders
            double d2 = MC.player().func_70011_f(e.field_70165_t, e.field_70163_u, e.field_70161_v);
            double max;
            if (e instanceof net.minecraft.entity.item.EntityArmorStand) max = standsDist.get();
            else if (e instanceof net.minecraft.entity.item.EntityItem) max = itemsDist.get();
            else if (e instanceof net.minecraft.entity.item.EntityItemFrame) max = framesDist.get();
            else if (e instanceof net.minecraft.entity.EntityLiving) max = mobsDist.get();
            else return true; // unknown types render as vanilla
            return d2 <= max * max;
        }

        public double tesrDistance() { return tesrDist.get(); }
    }

    private static int ACCENT_ON() { return 0xFF4F6EF7; }

    // ---------------------------------------------------------------- 36
    public static final class OldAnimations extends Module {
        public final Settings.Mode style = new Settings.Mode("style", "Style", Settings.options("1.7", "Lunar"), "1.7");

        OldAnimations() {
            super("oldanimations", "Old Animations", Category.OPTIMIZE);
            register(style);
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Old Animations") + 4, 12, BG);
            text("Old Animations", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Old Animations") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 37
    public static final class SmoothZoom extends Module {
        public final Settings.Num factor = new Settings.Num("factor", "Zoom factor", 0.35f, 0.1f, 0.8f, 0.05f);
        private float smooth;

        SmoothZoom() {
            super("smoothzoom", "Smooth Zoom", Category.OPTIMIZE);
            register(factor);
        }

        public boolean zooming() {
            return isEnabled() && Keyboard.isKeyDown(Keyboard.KEY_C);
        }

        public float currentFactor() {
            float target = zooming() ? factor.get() : 1.0f;
            smooth += (target - smooth) * 0.55f;
            if (Math.abs(smooth - target) < 0.01f) smooth = target;
            return smooth == 0 ? target : smooth;
        }

        public void render(float x, float y) {
            rect(x, y, textWidth("Smooth Zoom") + 4, 12, BG);
            text("Smooth Zoom", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("Smooth Zoom") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 38
    public static final class NoHurtCam extends Module {
        NoHurtCam() { super("nohurtcam", "No Hurt Cam", Category.OPTIMIZE); }

        public void render(float x, float y) {
            rect(x, y, textWidth("No Hurt Cam") + 4, 12, BG);
            text("No Hurt Cam", x + 2, y + 2, WHITE);
        }
        public float width() { return textWidth("No Hurt Cam") + 4; }
        public float height() { return 12; }
    }

    // ------------------------------------------------------------ registry
    public static boolean entityRenderAllowed(net.minecraft.entity.Entity e) {
        Module p = ModuleManager.byId("performance");
        return p instanceof Performance && ((Performance) p).entityRenderAllowed(e);
    }

    public static void registerAll() {
        ModuleManager.add(new Performance());
        ModuleManager.add(new OldAnimations());
        ModuleManager.add(new SmoothZoom());
        ModuleManager.add(new NoHurtCam());
    }
}
