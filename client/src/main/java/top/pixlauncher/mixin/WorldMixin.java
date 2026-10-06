package top.pixlauncher.mixin;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.UtilityModules;
import top.pixlauncher.util.MC;

/**
 * World hooks: client-visible time override (TimeChanger) and the forced-on
 * particle distance cull (Performance).
 */
@Mixin(World.class)
public class WorldMixin {

    // HEAD (not RETURN): mixin 0.7 weaves broken bytecode (VerifyError dup on
    // long) when injecting at multiple primitive-long return sites.
    @Inject(method = "func_72820_D", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onWorldTime(CallbackInfoReturnable<Long> cir) {
        Module m = ModuleManager.byId("timechanger");
        if (m != null && m.isEnabled()) {
            cir.setReturnValue(((UtilityModules.TimeChanger) m).overrideTicks());
        }
    }

    @Inject(method = "func_175688_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onSpawnParticle(EnumParticleTypes type, double x, double y, double z,
                                     double vx, double vy, double vz, int[] ids, CallbackInfo ci) {
        if (!MC.inGame() || MC.player() == null) return;
        double dx = x - MC.player().field_70165_t;
        double dy = y - MC.player().field_70163_u;
        double dz = z - MC.player().field_70161_v;
        // forced-on lossless cull: particles this far away are sub-pixel
        if (dx * dx + dy * dy + dz * dz > 48.0 * 48.0) ci.cancel();
    }

    @Inject(method = "func_175682_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onSpawnParticleAlways(EnumParticleTypes type, boolean longDistance, double x, double y, double z,
                                           double vx, double vy, double vz, int[] ids, CallbackInfo ci) {
        if (!MC.inGame() || MC.player() == null) return;
        double dx = x - MC.player().field_70165_t;
        double dy = y - MC.player().field_70163_u;
        double dz = z - MC.player().field_70161_v;
        if (!longDistance && dx * dx + dy * dy + dz * dz > 48.0 * 48.0) ci.cancel();
    }
}
