package top.pixlauncher.mixin;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.pixlauncher.module.modules.OptimizeModules;

/**
 * Forced-on entity render-distance culling (Performance): entities far from
 * the camera skip the whole render path. Players always render (PvP), the
 * local player is exempt, and distances come from the Performance module.
 */
@Mixin(RenderManager.class)
public class RenderManagerMixin {

    @Inject(method = "func_147937_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$cullStatic(Entity entity, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizeModules.entityRenderAllowed(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "func_147936_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$cullWithYaw(Entity entity, float partialTicks, boolean unused, CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizeModules.entityRenderAllowed(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "func_178635_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$cullCamera(Entity entity, ICamera camera, double x, double y, double z,
                                CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizeModules.entityRenderAllowed(entity)) cir.setReturnValue(false);
    }

}
