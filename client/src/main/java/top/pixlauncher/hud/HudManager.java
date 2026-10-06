package top.pixlauncher.hud;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import top.pixlauncher.events.Events;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.util.render.Render2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the on-screen HUD widgets. Each widget wraps a Module that implements
 * Widget; position (corner anchor + offsets) persists through ConfigManager.
 * Editor mode (drag / mouse-wheel scale) is entered from the ClickGUI.
 */
public final class HudManager {

    /** A module that draws a widget on the HUD. */
    public interface Widget {
        void render(float x, float y);
        float width();
        float height();
    }

    public static final class Placement {
        public int corner;      // 0 TL, 1 TR, 2 BL, 3 BR
        public int x, y;        // offset from the anchored corner
        public float scale = 1.0f;
    }

    public static final class Entry {
        public final Module module;
        public final Widget widget;
        public final Placement place = new Placement();

        Entry(Module module, Widget widget) {
            this.module = module;
            this.widget = widget;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private boolean editing;
    private Entry dragging;

    public void register(Module module, Widget widget) {
        entries.add(new Entry(module, widget));
    }

    public List<Entry> entries() { return entries; }

    public Entry byModule(Module module) {
        for (Entry e : entries) if (e.module == module) return e;
        return null;
    }

    public boolean isEditing() { return editing; }

    public void setEditing(boolean value) { editing = value; }

    public void start() {
        Events.subscribe(Events.Render2D.class, this::onRender2D);
    }

    private void onRender2D(Events.Render2D event) {
        int sw = event.resolution.func_78326_a();
        int sh = event.resolution.func_78328_b();

        if (editing) handleEditorInput(sw, sh);

        for (Entry e : entries) {
            if (!e.module.isEnabled()) continue;
            int x = (e.place.corner == 0 || e.place.corner == 2) ? e.place.x : sw - e.place.x - (int) e.widget.width();
            int y = (e.place.corner == 0 || e.place.corner == 1) ? e.place.y : sh - e.place.y - (int) e.widget.height();
            e.widget.render(x, y);
            if (editing) {
                Render2D.outline(x - 2, y - 2, e.widget.width() + 4, e.widget.height() + 4, 0xA04F6EF7);
            }
        }
    }

    private void handleEditorInput(int sw, int sh) {
        int mx = Mouse.getX() / 2; // gui-scaled approximation is fine for dragging
        int my = sh - Mouse.getY() / 2;
        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0) {
                if (Mouse.getEventButtonState()) {
                    dragging = entryAt(mx, my, sw, sh);
                } else {
                    dragging = null;
                }
            }
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0 && dragging != null) {
                dragging.place.scale = Math.max(0.5f, Math.min(3.0f, dragging.place.scale + (wheel > 0 ? 0.1f : -0.1f)));
            }
        }
        if (dragging != null && Mouse.isButtonDown(0)) {
            int x = (dragging.place.corner == 0 || dragging.place.corner == 2) ? mx : sw - mx - (int) dragging.widget.width();
            int y = (dragging.place.corner == 0 || dragging.place.corner == 1) ? my : sh - my - (int) dragging.widget.height();
            dragging.place.x = Math.max(0, x);
            dragging.place.y = Math.max(0, y);
        }
    }

    private Entry entryAt(int mx, int my, int sw, int sh) {
        for (Entry e : entries) {
            if (!e.module.isEnabled()) continue;
            int x = (e.place.corner == 0 || e.place.corner == 2) ? e.place.x : sw - e.place.x - (int) e.widget.width();
            int y = (e.place.corner == 0 || e.place.corner == 1) ? e.place.y : sh - e.place.y - (int) e.widget.height();
            if (mx >= x - 2 && mx <= x + e.widget.width() + 2 && my >= y - 2 && my <= y + e.widget.height() + 2) {
                return e;
            }
        }
        return null;
    }

    /** Modules that registered HUD widgets (used by the category listing). */
    public static Module hudModuleOf(Category category, String id) {
        return ModuleManagerHolder.byId(id);
    }
}
