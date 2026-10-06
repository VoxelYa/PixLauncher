package top.pixlauncher.module.modules;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import org.lwjgl.opengl.GL11;
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

/**
 * Render-category modules. Wire-box drawing happens in Render3D handlers;
 * engine-touching bits live in dedicated mixins that read these flags.
 */
public final class RenderModules {
    private RenderModules() { }

    private static final int BG = HudModules.BG;
    private static final int WHITE = HudModules.WHITE;

    private static boolean flag(String id) {
        Module m = ModuleManager.byId(id);
        return m != null && m.isEnabled();
    }

    // ---------------------------------------------------------------- 21
    public static final class Crosshair extends Module {
        public final Settings.BooleanSetting dot = new Settings.BooleanSetting("dot", "Center dot", true);
        public final Settings.Col color = new Settings.Col("color", "Color", 0xFFFFFFFF);
        public final Settings.Num gap = new Settings.Num("gap", "Gap", 3, 0, 10, 1);
        public final Settings.Num length = new Settings.Num("length", "Length", 6, 2, 16, 1);

        public Crosshair() {
            super("crosshair", "Custom Crosshair", Category.RENDER);
            register(dot); register(color); register(gap); register(length);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Crosshair") + 4, 12, BG);
            text("Crosshair", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Crosshair") + 4; }
        public float height() { return 12; }

        /** Called from the HUD overlay when the vanilla crosshair would draw. */
        public void drawCrosshair(float cx, float cy) {
            int c = color.get();
            float g = gap.get(), len = length.get(), th = 1.5f;
            rect(cx - len, cy - th / 2, len, th, c);
            rect(cx + g, cy - th / 2, len, th, c);
            rect(cx - th / 2, cy - len, th, len, c);
            rect(cx - th / 2, cy + g, th, len, c);
            if (dot.get()) rect(cx - 1, cy - 1, 2, 2, c);
        }
    }

    // ---------------------------------------------------------------- 22
    public static final class FullBright extends Module {
        private float previousGamma = -1;

        public FullBright() { super("fullbright", "Full Bright", Category.RENDER); }

        @Override protected void onEnable() {
            previousGamma = MC.settings().field_74333_Y;
            MC.settings().field_74333_Y = 10.0f;
        }

        @Override protected void onDisable() {
            if (previousGamma >= 0) MC.settings().field_74333_Y = previousGamma;
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Full Bright") + 4, 12, BG);
            text("Full Bright", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Full Bright") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 23
    public static final class MotionBlur extends Module {
        public final Settings.Num strength = new Settings.Num("strength", "Strength", 0.55f, 0.2f, 0.85f, 0.05f);

        public MotionBlur() {
            super("motionblur", "Motion Blur", Category.RENDER);
            register(strength);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Motion Blur") + 4, 12, BG);
            text("Motion Blur", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Motion Blur") + 4; }
        public float height() { return 12; }

        /** Read by EntityRendererMixin: alpha used for the framebuffer feedback pass. */
        public float alpha() { return strength.get(); }
    }

    // ---------------------------------------------------------------- 24
    public static final class HitColor extends Module {
        public final Settings.Col color = new Settings.Col("color", "Hit color", 0x80FF4040);

        public HitColor() {
            super("hitcolor", "Hit Color", Category.RENDER);
            register(color);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Hit Color") + 4, 12, BG);
            text("Hit Color", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Hit Color") + 4; }
        public float height() { return 12; }

        /** Recently-attacked entities get a brief colored wire box. */
        private final Map<Entity, Long> flashes = new HashMap<Entity, Long>();

        void flash(Entity e) { flashes.put(e, System.currentTimeMillis()); }

        void drawFlashes() {
            long now = System.currentTimeMillis();
            for (Map.Entry<Entity, Long> en : flashes.entrySet()) {
                long age = now - en.getValue();
                if (age > 300 || !en.getKey().func_70089_S()) { flashes.remove(en.getKey()); continue; }
                drawWireBox(en.getKey().func_174813_aQ(), (color.get() & 0xFFFFFF) | (0x90 << 24));
            }
        }
    }

    // ---------------------------------------------------------------- 25
    public static final class Hitboxes extends Module {
        public final Settings.Col color = new Settings.Col("color", "Box color", 0x80FFFFFF);
        public final Settings.Num expand = new Settings.Num("expand", "Expand", 0.1f, 0f, 1f, 0.05f);

        public Hitboxes() {
            super("hitboxes", "Hitboxes", Category.RENDER);
            register(color); register(expand);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Hitboxes") + 4, 12, BG);
            text("Hitboxes", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Hitboxes") + 4; }
        public float height() { return 12; }

        void draw() {
            if (MC.world() == null || MC.player() == null) return;
            int c = color.get();
            float ex = expand.get();
            for (Object o : MC.world().field_72996_f) {
                Entity e = (Entity) o;
                if (e == MC.player() || !e.func_70089_S()) continue;
                if (MC.player().func_70011_f(e.field_70165_t, e.field_70163_u, e.field_70161_v) > 32) continue;
                AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(ex, ex, ex);
                drawWireBox(bb, c);
            }
        }
    }

    // ---------------------------------------------------------------- 26
    public static final class BlockOverlay extends Module {
        public final Settings.Col color = new Settings.Col("color", "Overlay color", 0x304F6EF7);

        public BlockOverlay() {
            super("blockoverlay", "Block Overlay", Category.RENDER);
            register(color);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Block Overlay") + 4, 12, BG);
            text("Block Overlay", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Block Overlay") + 4; }
        public float height() { return 12; }

        void draw() {
            if (MC.mouseOver() == null || MC.mouseOver().field_72313_a != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) return;
            BlockPos p = MC.mouseOver().func_178782_a();
            if (p == null) return;
            AxisAlignedBB bb = AxisAlignedBB.func_178781_a(p.func_177958_n(), p.func_177956_o(), p.func_177952_p(),
                    p.func_177958_n() + 1, p.func_177956_o() + 1, p.func_177952_p() + 1);
            drawWireBox(bb, color.get());
        }
    }

    // ---------------------------------------------------------------- 27
    public static final class ItemPhysics extends Module {
        /** per-item angular velocity, written into EntityItem.hoverStart */
        private final Map<Integer, Float> spins = new HashMap<Integer, Float>();

        public ItemPhysics() { super("itemphysics", "Item Physics", Category.RENDER); }

        @Override protected void init() { setEnabled(true); } // visible by default

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Item Physics") + 4, 12, BG);
            text("Item Physics", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Item Physics") + 4; }
        public float height() { return 12; }

        /**
         * Real effect: every dropped item gets its own stable random angular
         * velocity written into EntityItem.hoverStart (public float) — items
         * tumble independently and spin faster while airborne, instead of
         * vanilla's uniform rotation.
         */
        void apply() {
            if (MC.world() == null) return;
            for (Object o : MC.world().field_72996_f) {
                if (!(o instanceof EntityItem)) continue;
                EntityItem item = (EntityItem) o;
                int id = item.func_145782_y();
                Float vel = spins.get(id);
                if (vel == null) {
                    vel = 0.12f + (id % 17) * 0.035f;   // stable per item id
                    if (id % 2 == 0) vel = -vel;        // varied direction
                    spins.put(id, vel);
                }
                float speed = item.field_70122_E ? vel * 0.25f : vel; // faster tumble in air
                item.field_70290_d += speed;
            }
            if (spins.size() > 512) spins.clear(); // cheap GC for despawned items
        }
    }

    // ---------------------------------------------------------------- 28
    public static final class CustomFog extends Module {
        public final Settings.Col color = new Settings.Col("color", "Fog color", 0xFFC0D8FF);
        public final Settings.Num distance = new Settings.Num("distance", "Start distance", 0.2f, 0.05f, 1f, 0.05f);

        public CustomFog() {
            super("customfog", "Custom Fog", Category.RENDER);
            register(color); register(distance);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Custom Fog") + 4, 12, BG);
            text("Custom Fog", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Custom Fog") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 29
    public static final class FireModifier extends Module {
        public final Settings.Num height = new Settings.Num("height", "Height", 0.5f, 0f, 1f, 0.05f);

        public FireModifier() {
            super("firemodifier", "Fire Modifier", Category.RENDER);
            register(height);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Fire Modifier") + 4, 12, BG);
            text("Fire Modifier", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Fire Modifier") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 30
    public static final class MinimizedBobbing extends Module {
        private boolean previous = true;

        public MinimizedBobbing() { super("minimizebobbing", "Less Bobbing", Category.RENDER); }

        @Override protected void onEnable() {
            previous = MC.settings().field_74336_f;
            MC.settings().field_74336_f = false;
        }

        @Override protected void onDisable() {
            MC.settings().field_74336_f = previous;
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Less Bobbing") + 4, 12, BG);
            text("Less Bobbing", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Less Bobbing") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 31
    public static final class MoreParticles extends Module {
        public final Settings.BooleanSetting crits = new Settings.BooleanSetting("crits", "Extra crits", true);

        public MoreParticles() {
            super("moreparticles", "More Particles", Category.RENDER);
            register(crits);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("More Particles") + 4, 12, BG);
            text("More Particles", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("More Particles") + 4; }
        public float height() { return 12; }

        void onAttack(Entity target) {
            if (MC.world() == null) return;
            for (int i = 0; i < 6; i++) {
                MC.world().func_175688_a(net.minecraft.util.EnumParticleTypes.CRIT,
                        target.field_70165_t, target.field_70163_u + target.field_70131_O * 0.5, target.field_70161_v,
                        (Math.random() - 0.5) * 0.4, Math.random() * 0.3, (Math.random() - 0.5) * 0.4);
            }
        }
    }

    // ---------------------------------------------------------------- 32
    public static final class CleanView extends Module {
        public CleanView() { super("cleanview", "Clean View", Category.RENDER); }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Clean View") + 4, 12, BG);
            text("Clean View", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Clean View") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 33
    public static final class ChatAvatars extends Module {
        public ChatAvatars() { super("chatavatars", "Chat Avatars", Category.RENDER); }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("Avatars") + 4, 12, BG);
            text("Avatars", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("Avatars") + 4; }
        public float height() { return 12; }
    }

    // ---------------------------------------------------------------- 34
    public static final class CustomFov extends Module {
        public final Settings.BooleanSetting noSpeed = new Settings.BooleanSetting("nospeed", "No sprint FOV", true);
        public final Settings.BooleanSetting noBow = new Settings.BooleanSetting("nobow", "No bow FOV", true);

        public CustomFov() {
            super("customfov", "FOV Control", Category.RENDER);
            register(noSpeed); register(noBow);
        }

        public void render(float x, float y) {
            rect(x, y, HudModules.line("FOV Control") + 4, 12, BG);
            text("FOV Control", x + 2, y + 2, WHITE);
        }
        public float width() { return HudModules.line("FOV Control") + 4; }
        public float height() { return 12; }
    }

    // --------------------------------------------------------- shared draw
    static void drawWireBox(AxisAlignedBB bb, int argb) {
        GlStateManager.func_179090_x(); // disable texture
        GlStateManager.func_179147_l(); // enable blend
        GlStateManager.func_179129_p(); // disable depth? keep depth on for v1: comment
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        float a = ((argb >>> 24) & 0xFF) / 255f;
        GL11.glColor4f(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, a);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72334_f);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72339_c);
        GL11.glEnd();
        GlStateManager.func_179126_j(); // enable depth
        GlStateManager.func_179084_k(); // disable blend
        GlStateManager.func_179145_e(); // enable texture
    }

    // ------------------------------------------------------------ registry
    public static void registerAll() {
        ModuleManager.add(new Crosshair());
        ModuleManager.add(new FullBright());
        ModuleManager.add(new MotionBlur());
        ModuleManager.add(new HitColor());
        ModuleManager.add(new Hitboxes());
        ModuleManager.add(new BlockOverlay());
        ModuleManager.add(new ItemPhysics());
        ModuleManager.add(new CustomFog());
        ModuleManager.add(new FireModifier());
        ModuleManager.add(new MinimizedBobbing());
        ModuleManager.add(new MoreParticles());
        ModuleManager.add(new CleanView());
        ModuleManager.add(new ChatAvatars());
        ModuleManager.add(new CustomFov());

        // cross-module render wiring
        Events.subscribe(Events.Attack.class, e -> {
            Module mp = ModuleManager.byId("moreparticles");
            if (mp != null && mp.isEnabled()) ((MoreParticles) mp).onAttack(e.target);
            Module hc = ModuleManager.byId("hitcolor");
            if (hc != null && hc.isEnabled()) ((HitColor) hc).flash(e.target);
        });
        Events.subscribe(Events.Render3D.class, e -> {
            if (flag("hitboxes")) ((Hitboxes) ModuleManager.byId("hitboxes")).draw();
            if (flag("blockoverlay")) ((BlockOverlay) ModuleManager.byId("blockoverlay")).draw();
            if (flag("hitcolor")) ((HitColor) ModuleManager.byId("hitcolor")).drawFlashes();
        });
    }
}
