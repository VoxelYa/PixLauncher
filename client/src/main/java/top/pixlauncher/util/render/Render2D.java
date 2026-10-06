package top.pixlauncher.util.render;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import static top.pixlauncher.util.MC.font;

/**
 * Thin helpers over vanilla drawing calls (same renderer the game itself
 * uses — no extra shader state to fight with).
 */
public final class Render2D {
    private Render2D() { }

    public static void rect(float x, float y, float w, float h, int argb) {
        float x2 = x + w, y2 = y + h;
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        if (a <= 0.002f) return;
        GlStateManager.func_179131_c(a, ((argb >> 16) & 0xFF) / 255.0f, ((argb >> 8) & 0xFF) / 255.0f, (argb & 0xFF) / 255.0f);
        Gui.func_73734_a((int) x, (int) y, (int) x2, (int) y2, argb);
        GlStateManager.func_179131_c(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void outline(float x, float y, float w, float h, int argb) {
        rect(x, y, w, 1, argb);
        rect(x, y + h - 1, w, 1, argb);
        rect(x, y, 1, h, argb);
        rect(x + w - 1, y, 1, h, argb);
    }

    public static void text(String text, float x, float y, int argb) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        font().func_175063_a(text, (int) x, (int) y, argb);
        GL11.glPopAttrib();
    }

    public static float textWidth(String text) {
        return font().func_78256_a(text);
    }
}
