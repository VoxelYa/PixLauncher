package top.pixlauncher.mixin;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.UtilityModules;
import top.pixlauncher.util.MC;
import top.pixlauncher.module.Module;

/** Nametags: keep nameplates visible through the vanilla visibility rules. */
@Mixin(RendererLivingEntity.class)
public class RendererLivingEntityMixin {

    @Inject(method = "func_177070_b", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$canRenderName(net.minecraft.entity.EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        Module m = ModuleManager.byId("nametags");
        if (m == null || !m.isEnabled() || MC.player() == null) return;
        UtilityModules.Nametags tags = (UtilityModules.Nametags) m;
        if (tags.hideNpcs.get() && !(entity instanceof net.minecraft.client.entity.EntityOtherPlayerMP)
                && !(entity instanceof net.minecraft.client.entity.EntityOtherPlayerMP)) {
            cir.setReturnValue(false);
            return;
        }
        if ((entity instanceof net.minecraft.client.entity.EntityOtherPlayerMP || entity == MC.player())
                && entity instanceof net.minecraft.entity.EntityLivingBase) {
            cir.setReturnValue(true);
        }
    }
}
