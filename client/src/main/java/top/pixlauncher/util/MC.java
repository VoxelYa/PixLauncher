package top.pixlauncher.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovingObjectPosition;

/**
 * Shared static accessors for the game object graph. Names are SRG (the
 * runtime names under Forge 1.8.9 production) — verified against the actual
 * jar we compile against, never guessed.
 */
public final class MC {
    private MC() { }

    public static Minecraft mc() { return Minecraft.func_71410_x(); }

    public static EntityPlayerSP player() { return mc().field_71439_g; }

    public static WorldClient world() { return mc().field_71441_e; }

    public static FontRenderer font() { return mc().field_71466_p; }

    public static GameSettings settings() { return mc().field_71474_y; }

    public static MovingObjectPosition mouseOver() { return mc().field_71476_x; }

    public static int debugFps() { return Minecraft.func_175610_ah(); }

    public static ScaledResolution scaledResolution() { return new ScaledResolution(mc()); }

    public static boolean inGame() {
        return mc().field_71439_g != null && mc().field_71441_e != null;
    }
}
