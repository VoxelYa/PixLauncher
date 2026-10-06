package top.pixlauncher.util;

import java.io.File;

/**
 * Game directory handed to the bootstrap by the launcher (portable layout).
 * Falls back to the working directory when launched outside PixLauncher.
 */
public final class Paths {
    private static File gameDir;

    private Paths() { }

    public static void setGameDir(File dir) {
        gameDir = dir;
    }

    public static File gameDir() {
        if (gameDir != null) return gameDir;
        String prop = System.getProperty("pixlauncher.gamedir");
        return prop != null ? new File(prop) : new File(".");
    }
}
