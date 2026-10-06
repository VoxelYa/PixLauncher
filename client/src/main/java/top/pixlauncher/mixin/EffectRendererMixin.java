package top.pixlauncher.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;

/**
 * Block-destroy particle control (ParticlesModifier): lets one burst through,
 * cancels the rest of the 4x4x4 spray. Signature matches
 * EffectRenderer.func_180533_a(BlockPos, IBlockState) exactly (verified via
 * javap on the runtime jar).
 */
@Mixin(EffectRenderer.class)
public class EffectRendererMixin {

    @Inject(method = "func_180533_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onBlockDestroy(BlockPos pos, IBlockState state, CallbackInfo ci) {
        Module m = ModuleManager.byId("particlesmodifier");
        if (m != null && m.isEnabled()) {
            if (top.pixlauncher.module.modules.ParticlesModifierFewerMarker.consume()) ci.cancel(); // one burst only
        }
    }
}
