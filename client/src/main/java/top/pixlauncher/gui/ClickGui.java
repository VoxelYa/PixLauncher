package top.pixlauncher.gui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import top.pixlauncher.client.PixClient;
import top.pixlauncher.hud.HudManager;
import top.pixlauncher.module.Category;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.settings.Setting;
import top.pixlauncher.module.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static top.pixlauncher.util.render.Render2D.rect;
import static top.pixlauncher.util.render.Render2D.text;
import static top.pixlauncher.util.render.Render2D.textWidth;

/**
 * ClickGUI (RShift): one panel per category, left click toggles a module,
 * right click expands its settings. HUD Editor button enters drag mode.
 */
public final class ClickGui {

    private final PixGuiScreen screen = new PixGuiScreen();

    public GuiScreen display() { return screen; }

    static final class PixGuiScreen extends GuiScreen {
        static final int PANEL_W = 190, ROW = 14;
        private final Map<Category, Integer> panelX = new LinkedHashMap<Category, Integer>();
        private final Map<Category, Boolean> dragging = new LinkedHashMap<Category, Boolean>();
        private final Map<Category, Integer> panelY = new LinkedHashMap<Category, Integer>();
        private final Map<Module, Boolean> expanded = new LinkedHashMap<Module, Boolean>();
        private Module capturingBind;
        private int lastMouseX, lastMouseY;

        private List<Module> inCategory(Category c) {
            List<Module> out = new ArrayList<Module>();
            for (Module m : top.pixlauncher.module.ModuleManager.all()) if (m.category() == c) out.add(m);
            return out;
        }

        private void ensureLayout() {
            int i = 0;
            for (Category c : Category.values()) {
                if (!panelX.containsKey(c)) {
                    panelX.put(c, 8 + i * (PANEL_W + 8));
                    panelY.put(c, 8);
                    dragging.put(c, false);
                }
                i++;
            }
        }

        @Override
        public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
            ensureLayout();
            func_146276_q_(); // vanilla dark gradient
            for (Category c : Category.values()) {
                if (Boolean.TRUE.equals(dragging.get(c))) {
                    panelX.put(c, mouseX - PANEL_W / 2);
                    panelY.put(c, mouseY - 8);
                }
                int x = panelX.get(c), y = panelY.get(c);
                List<Module> mods = inCategory(c);
                rect(x, y, PANEL_W, 14, 0xF0181B26);
                text(c.label(), x + 4, y + 3, 0xFFFFFFFF);
                String hudBtn = c == Category.HUD ? " [HUD Editor]" : "";
                text(hudBtn, x + PANEL_W - textWidth(hudBtn) - 2, y + 3, 0xFF8FA0C8);

                int ry = y + 14;
                for (Module m : mods) {
                    boolean on = m.isEnabled();
                    rect(x, ry, PANEL_W, ROW, on ? 0xE0222A3C : 0xD0141620);
                    text(m.name(), x + 6, ry + 3, on ? 0xFF4F6EF7 : 0xFFC0C6D4);
                    Object ex = expanded.get(m);
                    text(ex == Boolean.TRUE ? "-" : "+", x + PANEL_W - 10, ry + 3, 0xFF8FA0C8);
                    ry += ROW;
                    if (ex == Boolean.TRUE) {
                        for (Setting<?> s : m.settings()) {
                            String label = s.label();
                            if (s instanceof Settings.BooleanSetting) {
                                boolean v = ((Settings.BooleanSetting) s).get();
                                rect(x + 8, ry + 2, 8, 8, v ? 0xFF4F6EF7 : 0xFF303848);
                                text(label, x + 22, ry + 3, 0xFFC0C6D4);
                            } else if (s instanceof Settings.Num) {
                                Settings.Num n = (Settings.Num) s;
                                text(label + " " + n.get(), x + 8, ry + 3, 0xFFC0C6D4);
                                rect(x + 8, ry + 11, PANEL_W - 16, 2, 0xFF303848);
                                float frac = (n.get() - n.min) / (n.max - n.min);
                                rect(x + 8, ry + 10, (PANEL_W - 16) * frac, 3, 0xFF4F6EF7);
                                ry += 4;
                            } else if (s instanceof Settings.Mode) {
                                Settings.Mode mo = (Settings.Mode) s;
                                text(label + " <" + mo.get() + ">", x + 8, ry + 3, 0xFFC0C6D4);
                            } else if (s instanceof Settings.Bind) {
                                String v = capturingBind == m ? "press…" : Keyboard.getKeyName(((Settings.Bind) s).get());
                                text(label + " [" + v + "]", x + 8, ry + 3, 0xFFC0C6D4);
                            } else if (s instanceof Settings.Col) {
                                Settings.Col col = (Settings.Col) s;
                                rect(x + 8, ry + 2, 8, 8, col.get());
                                text(label, x + 22, ry + 3, 0xFFC0C6D4);
                            }
                            ry += 13;
                        }
                    }
                }
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private Category categoryAt(int mx, int my) {
            for (Category c : Category.values()) {
                int x = panelX.get(c), y = panelY.get(c);
                if (mx >= x && mx <= x + PANEL_W && my >= y && my <= y + 14) return c;
            }
            return null;
        }

        @Override
        protected void func_73864_a(int mouseX, int mouseY, int button) {
            ensureLayout();
            Category header = categoryAt(mouseX, mouseY);
            if (header != null) {
                if (button == 0) dragging.put(header, true);
                if (button == 2 && header == Category.HUD) {
                    PixClient.get().hud().setEditing(true);
                    field_146297_k.func_147108_a(null);
                }
                return;
            }
            for (Category c : Category.values()) {
                int x = panelX.get(c), y = panelY.get(c);
                int ry = y + 14;
                for (Module m : inCategory(c)) {
                    if (mouseX >= x && mouseX <= x + PANEL_W && mouseY >= ry && mouseY < ry + ROW) {
                        if (button == 0) m.toggle();
                        if (button == 1) {
                            Boolean ex = expanded.get(m);
                            expanded.put(m, ex == null ? Boolean.TRUE : (ex ? Boolean.FALSE : Boolean.TRUE));
                        }
                        return;
                    }
                    if (expanded.get(m) == Boolean.TRUE) {
                        for (final Setting<?> s : m.settings()) {
                            if (s instanceof Settings.BooleanSetting) {
                                if (hit(mouseX, mouseY, x + 8, ry + 2, PANEL_W - 16, 10)) {
                                    ((Settings.BooleanSetting) s).set(!((Settings.BooleanSetting) s).get());
                                    return;
                                }
                            } else if (s instanceof Settings.Bind) {
                                if (hit(mouseX, mouseY, x + 8, ry, PANEL_W - 16, 12)) {
                                    capturingBind = m;
                                    return;
                                }
                            }
                            ry += s instanceof Settings.Num ? 17 : 13;
                        }
                    }
                    ry += ROW;
                }
            }
        }

        private boolean hit(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        @Override
        protected void func_146273_a(int mouseX, int mouseY, int button, long time) {
            for (Category c : Category.values()) {
                int x = panelX.get(c), y = panelY.get(c);
                int ry = y + 14;
                for (Module m : inCategory(c)) {
                    ry += ROW;
                    if (expanded.get(m) == Boolean.TRUE) {
                        for (Setting<?> s : m.settings()) {
                            if (s instanceof Settings.Num && button == 0) {
                                Settings.Num n = (Settings.Num) s;
                                float frac = Math.max(0f, Math.min(1f, (mouseX - (x + 8)) / (float) (PANEL_W - 16)));
                                n.set(n.min + (n.max - n.min) * frac);
                                n.set(Math.round(n.get() / n.step) * n.step);
                            }
                            ry += s instanceof Settings.Num ? 17 : 13;
                        }
                    }
                }
            }
        }

        @Override
        public void func_146274_d() { // handleMouseInput
            super.func_146274_d();
            if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
                for (Category c : Category.values()) dragging.put(c, false);
            }
        }

        @Override
        public void func_146282_l() { // handleKeyboardInput
            super.func_146282_l();
            if (capturingBind != null && Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();
                if (key != Keyboard.KEY_ESCAPE) {
                    for (Setting<?> s : capturingBind.settings()) {
                        if (s instanceof Settings.Bind) {
                            ((Settings.Bind) s).set(key);
                            break;
                        }
                    }
                }
                capturingBind = null;
            }
        }

        @Override
        protected void func_73869_a(char typed, int key) {
            if (key == Keyboard.KEY_ESCAPE) {
                ConfigManagerBridge.save();
                field_146297_k.func_147108_a(null);
                return;
            }
            super.func_73869_a(typed, key);
        }
    }
}
