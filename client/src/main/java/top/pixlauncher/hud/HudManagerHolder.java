package top.pixlauncher.hud;

import top.pixlauncher.client.PixClient;

/** Lazily resolves the HudManager singleton without a compile cycle. */
final class HudManagerHolder {
    private HudManagerHolder() { }

    static HudManager manager() {
        return PixClient.get().hud();
    }
}
