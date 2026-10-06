package top.pixlauncher.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.events.Events;
import top.pixlauncher.util.MC;

/**
 * Fires the 2D HUD render event at the end of the (Forge) overlay pass.
 * Targeted by string so the forge class never needs to be on our compile cp.
 */
@Mixin(targets = "net.minecraftforge.client.GuiIngameForge")
public class GuiIngameForgeMixin {

    @Inject(method = "func_175180_a", at = @At("RETURN"))
    private void pix$onOverlay(float partialTicks, CallbackInfo ci) {
        if (MC.inGame()) Events.fire(new Events.Render2D(MC.scaledResolution()));
    }
}
