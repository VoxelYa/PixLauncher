package top.pixlauncher.mixin;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.events.Events;

/** Chat receive hook (AutoGG trigger, future chat widgets). */
@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {

    @Inject(method = "func_146227_a", at = @At("HEAD"))
    private void pix$onChat(IChatComponent message, CallbackInfo ci) {
        Events.fire(new Events.ChatLine(message));
    }
}
