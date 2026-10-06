package top.pixlauncher.client;

import org.lwjgl.input.Keyboard;
import top.pixlauncher.config.ConfigManager;
import top.pixlauncher.events.Events;
import top.pixlauncher.gui.ClickGui;
import top.pixlauncher.hud.HudManager;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.HudModules;
import top.pixlauncher.module.modules.HudModules2;
import top.pixlauncher.module.modules.OptimizeModules;
import top.pixlauncher.module.modules.RenderModules;
import top.pixlauncher.module.modules.UtilityModules;
import top.pixlauncher.util.MC;

/**
 * Client runtime: created on the first game tick (via MinecraftMixin), owns
 * the managers, processes key input, and saves config on shutdown.
 */
public final class PixClient {

    private static PixClient instance;

    private final HudManager hud = new HudManager();
    private final ClickGui gui = new ClickGui();
    private boolean started;

    private PixClient() { }

    public static PixClient get() {
        if (instance == null) instance = new PixClient();
        return instance;
    }

    public HudManager hud() { return hud; }

    public ClickGui gui() { return gui; }

    public synchronized void start() {
        if (started) return;
        started = true;
        System.out.println("[PixLauncher] client starting");

        HudModules.registerAll();
        HudModules2.registerAll();
        RenderModules.registerAll();
        UtilityModules.registerAll();
        OptimizeModules.registerAll();
        ModuleManager.init();
        hud.start();

        // starter defaults: the everyday PvP HUD, on out of the box
        for (String id : new String[] {
            "fpsdisplay", "cpsdisplay", "keystrokes", "pingdisplay",
            "coordsdisplay", "combodisplay", "armordisplay", "potiondisplay",
            "sprint", "togglesneak", "modslist", "crosshair", "fullbright"
        }) {
            Module m = ModuleManager.byId(id);
            if (m != null) m.setEnabled(true);
        }

        ConfigManager.load(ConfigManager.activeProfile());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> ConfigManager.save()));
        System.out.println("[PixLauncher] client ready (" + ModuleManager.all().size() + " modules)");
    }

    /** Called from the tick handler: state-diff input so vanilla keeps its event queue. */
    public void handleInput() {
        boolean[] keys = KEYS;
        for (int key = 0; key < keys.length; key++) {
            boolean down;
            try {
                down = key == 0 ? false : Keyboard.isKeyDown(key);
            } catch (Exception bad) {
                down = false;
            }
            if (down && !keys[key]) {
                Events.fire(new Events.Key(key));
                for (Module m : ModuleManager.all()) {
                    if (m.keyBind() == key) m.toggle();
                }
                if (key == Keyboard.KEY_RSHIFT && MC.mc().field_71462_r == null) {
                    MC.mc().func_147108_a(gui.display());
                }
            }
            keys[key] = down;
        }
    }

    private static final boolean[] KEYS = new boolean[256];

    public void shutdown() {
        ConfigManager.save();
    }
}
