package top.pixlauncher.module.modules;

/** One-shot latch so the block-destroy mixin only cancels once per event. */
public final class ParticlesModifierFewerMarker {
    private static boolean armed = true;

    private ParticlesModifierFewerMarker() { }

    public static void mark() { armed = true; }

    public static boolean consume() {
        boolean v = armed;
        armed = false;
        return v;
    }
}
