package top.pixlauncher.config;

import top.pixlauncher.client.PixClient;
import top.pixlauncher.hud.HudManager;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;

import java.util.Map;

/** Bridges HUD placements between HudManager and the config JSON tree. */
final class HudConfigIo {
    private HudConfigIo() { }

    static Map<String, Object> capture() {
        Map<String, Object> out = new java.util.LinkedHashMap<String, Object>();
        for (HudManager.Entry e : PixClient.get().hud().entries()) {
            Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
            p.put("corner", e.place.corner);
            p.put("x", e.place.x);
            p.put("y", e.place.y);
            p.put("scale", e.place.scale);
            out.put(e.module.id(), p);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static void restore(Map<String, Object> hud) {
        for (Map.Entry<String, Object> en : hud.entrySet()) {
            Module m = ModuleManager.byId(en.getKey());
            if (m == null || !(en.getValue() instanceof Map)) continue;
            HudManager.Entry e = PixClient.get().hud().byModule(m);
            if (e == null) continue;
            Map<String, Object> p = (Map<String, Object>) en.getValue();
            e.place.corner = asInt(p.get("corner"), e.place.corner);
            e.place.x = asInt(p.get("x"), e.place.x);
            e.place.y = asInt(p.get("y"), e.place.y);
            e.place.scale = asFloat(p.get("scale"), e.place.scale);
        }
    }

    private static int asInt(Object v, int fallback) {
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    private static float asFloat(Object v, float fallback) {
        return v instanceof Number ? ((Number) v).floatValue() : fallback;
    }
}
