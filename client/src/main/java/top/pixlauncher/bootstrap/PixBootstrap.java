package top.pixlauncher.bootstrap;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.Mixins;
import top.pixlauncher.util.Paths;

import java.io.File;
import java.util.List;

/**
 * Lunar-style entry point: sits FIRST in the launchwrapper tweak chain
 * (before FML's tweaker) and wires the Mixin transformer + our own ASM
 * transformers before any game class is loaded. Nothing is written to the
 * game jar and nothing is placed in the mods folder.
 */
public class PixBootstrap implements ITweaker {

    private static boolean injected = false;
    private File gameDir;
    private File assetsDir;
    private String version;
    private List<String> leftoverArgs;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String version) {
        this.gameDir = gameDir;
        this.assetsDir = assetsDir;
        this.version = version;
        this.leftoverArgs = args;
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (injected) return;
        injected = true;

        if (gameDir != null) Paths.setGameDir(gameDir);
        // NOTE: no classLoaderExclusion for top.pixlauncher — the Mixin
        // subsystem must load our mixin classes through LaunchClassLoader.

        // Single-asm rule: ASM + Mixin must load ONCE from the parent loader
        // (pixclient ships ASM 6, first on the classpath). Without these
        // exclusions FML's ASMTransformerWrapper re-derives a second
        // ClassVisitor through LaunchClassLoader and the two clash.
        classLoader.addClassLoaderExclusion("org.objectweb.asm.");
        classLoader.addClassLoaderExclusion("org.spongepowered.asm.");
        classLoader.addTransformerExclusion("org.objectweb.asm.");
        classLoader.addTransformerExclusion("org.spongepowered.asm.");

        try {
            // Delegate the mixin wiring to the launcher shipped inside the
            // Mixin jar itself — the same steps MixinBootstrap performs.
            Class<?> tweakerClass = Class.forName("org.spongepowered.asm.launch.MixinTweaker");
            ITweaker mixinTweaker = (ITweaker) tweakerClass.newInstance();
            mixinTweaker.acceptOptions(leftoverArgs, gameDir, assetsDir, version);
            mixinTweaker.injectIntoClassLoader(classLoader);
            Mixins.addConfiguration("mixins.pixlauncher.json");
        } catch (ClassNotFoundException missing) {
            System.out.println("[PixLauncher] Mixin not on classpath; running ASM transformers only");
        } catch (Exception broken) {
            throw new IllegalStateException("[PixLauncher] failed to initialize Mixin", broken);
        }

        classLoader.registerTransformer("top.pixlauncher.bootstrap.transformer.PerformanceTransformer");
        System.out.println("[PixLauncher] bootstrap injected into launch chain");
    }

    @Override
    public String getLaunchTarget() {
        // Launch uses the LAST tweaker's target; FML's tweaker comes after us.
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0]; // FML's tweaker supplies the game arguments
    }
}
