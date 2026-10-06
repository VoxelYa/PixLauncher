package top.pixlauncher.events;

import net.minecraft.entity.Entity;
import net.minecraft.util.IChatComponent;
import net.minecraft.client.gui.ScaledResolution;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal synchronous event bus fired by our mixins. Handlers only receive
 * their exact event type; events are plain data holders.
 */
public final class Events {

    private static final Map<Class<?>, List<Handler<?>>> HANDLERS = new ConcurrentHashMap<Class<?>, List<Handler<?>>>();

    private Events() { }

    public interface Handler<T> { void on(T event); }

    public static <T> void subscribe(Class<T> type, Handler<T> handler) {
        List<Handler<?>> list = HANDLERS.get(type);
        if (list == null) {
            list = new CopyOnWriteArrayList<Handler<?>>();
            HANDLERS.put(type, list);
        }
        list.add(handler);
    }

    @SuppressWarnings("unchecked")
    public static <T> void fire(T event) {
        List<Handler<?>> list = HANDLERS.get(event.getClass());
        if (list == null) return;
        for (Handler<?> h : list) {
            try {
                ((Handler<T>) h).on(event);
            } catch (Throwable broken) {
                System.out.println("[PixLauncher] handler failed: " + broken);
            }
        }
    }

    // ------------------------------------------------------------- events

    public static final class Tick {
        public static final Tick INSTANCE = new Tick();
        private Tick() { }
    }

    public static final class Render2D {
        public final ScaledResolution resolution;
        public Render2D(ScaledResolution resolution) { this.resolution = resolution; }
    }

    public static final class Render3D {
        public final float partialTicks;
        public Render3D(float partialTicks) { this.partialTicks = partialTicks; }
    }

    public static final class Key {
        public final int key;
        public Key(int key) { this.key = key; }
    }

    public static final class MouseClick {
        public final int button;
        public MouseClick(int button) { this.button = button; }
    }

    public static final class MouseWheel {
        public final int direction;
        public MouseWheel(int direction) { this.direction = direction; }
    }

    public static final class Attack {
        public final Entity target;
        public Attack(Entity target) { this.target = target; }
    }

    public static final class ChatLine {
        public final IChatComponent message;
        public ChatLine(IChatComponent message) { this.message = message; }
    }
}
