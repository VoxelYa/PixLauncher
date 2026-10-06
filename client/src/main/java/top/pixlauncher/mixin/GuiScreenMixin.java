package top.pixlauncher.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.RenderModules;
import top.pixlauncher.util.MC;

/** BetterScreens: flat translucent backdrop instead of the dirt gradient. */
@Mixin(GuiScreen.class)
public class GuiScreenMixin {

    @Inject(method = "func_146276_q_", at = @At("HEAD"), cancellable = true)
    private void pix$onDefaultBackground(CallbackInfo ci) {
        Module m = ModuleManager.byId("betterscreen");
        if (m == null || !m.isEnabled()) return;
        // keep our own ClickGui look untouched
        if (MC.mc().field_71462_r != null
                && MC.mc().field_71462_r.getClass().getName().contains("PixGuiScreen")) return;
        ci.cancel();
        int w = MC.scaledResolution().func_78326_a();
        int h = MC.scaledResolution().func_78328_b();
        float strength = m.settings().size() > 0 ? 0.4f : 0.4f;
        int alpha = (int) (0xE0 * Math.min(1.0f, strength / 0.8f));
        net.minecraft.client.gui.Gui.func_73734_a(0, 0, w, h, (alpha << 24) | 0x0A0C12);
    }
}
