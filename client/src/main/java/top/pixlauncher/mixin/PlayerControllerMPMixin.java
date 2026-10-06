package top.pixlauncher.mixin;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.events.Events;
import top.pixlauncher.util.MC;

/** Left-click attack hook feeding reach / combo / damage HUDs. */
@Mixin(PlayerControllerMP.class)
public class PlayerControllerMPMixin {

    @Inject(method = "func_78764_a", at = @At("HEAD"))
    private void pix$onAttack(EntityPlayer player, Entity target, CallbackInfo ci) {
        if (MC.inGame()) Events.fire(new Events.Attack(target));
    }
}
