package top.pixlauncher.tools;

import net.minecraftforge.installer.ClientInstall;
import java.io.File;

/**
 * Headless client install for the legacy Forge 1.8.9 installer, whose CLI
 * only knows --installServer/--extract. Runs ON the installer's classpath and
 * calls the same ClientInstall entry the GUI "Install client" button uses.
 * Usage: java -cp forge-installer.jar;tools/classes top.pixlauncher.tools.ForgeInstallHelper <gameDir>
 */
public final class ForgeInstallHelper {
    private ForgeInstallHelper() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ForgeInstallHelper <gameDir>");
            System.exit(2);
        }
        File target = new File(args[0]).getAbsoluteFile();
        boolean ok = new ClientInstall().run(target, input -> true);
        System.out.println(ok ? "FORGE_CLIENT_INSTALL_OK" : "FORGE_CLIENT_INSTALL_FAILED");
        System.exit(ok ? 0 : 1);
    }
}
