package top.pixlauncher.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.client.PixClient;
import top.pixlauncher.events.Events;

/**
 * Game tick entry: starts the client on the first tick, then per tick drives
 * input state-diffing and fires the tick event. Method names are SRG — the
 * production runtime names under Forge 1.8.9.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "func_71407_l", at = @At("HEAD"))
    private void pix$onRunTick(CallbackInfo ci) {
        PixClient.get().start();
        PixClient.get().handleInput();
        Events.fire(Events.Tick.INSTANCE);
    }
}
