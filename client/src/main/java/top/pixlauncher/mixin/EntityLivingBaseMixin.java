package top.pixlauncher.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.util.MC;

/**
 * CleanView: when enabled, the local player emits no potion swirl particles
 * (vanilla null-checks the particle name before spawning).
 */
@Mixin(EntityLivingBase.class)
public class EntityLivingBaseMixin {

    @Inject(method = "func_70621_aR", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onParticleName(CallbackInfoReturnable<String> cir) {
        Module m = ModuleManager.byId("cleanview");
        if (m == null || !m.isEnabled()) return;
        Object self = this;
        if (self == MC.player()) cir.setReturnValue(null);
    }
}
