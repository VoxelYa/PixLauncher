package top.pixlauncher.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.module.modules.OptimizeModules;
import top.pixlauncher.util.MC;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;

/** Forced-on TESR distance culling (Performance): chest-sized TEs beyond the
 *  configured distance skip their special renderer entirely. */
@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin {

    @Inject(method = "func_147549_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$cullFarTe(TileEntity te, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        if (!MC.inGame() || MC.player() == null) return;
        Module perf = ModuleManager.byId("performance");
        if (perf == null) return;
        double dx = x - MC.player().field_70165_t;
        double dy = y - MC.player().field_70163_u;
        double dz = z - MC.player().field_70161_v;
        double max = ((OptimizeModules.Performance) perf).tesrDistance();
        if (dx * dx + dy * dy + dz * dz > max * max) ci.cancel();
    }
}
