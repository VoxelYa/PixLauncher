package top.pixlauncher.mixin;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.OptimizeModules;
import top.pixlauncher.util.MC;

/**
 * OldAnimations v1: 1.7-flavoured first-person offsets for rod / bow /
 * sword items while the module is enabled.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "func_78440_a", at = @At("HEAD"))
    private void pix$oldAnimBegin(float partialTicks, CallbackInfo ci) {
        Module m = ModuleManager.byId("oldanimations");
        if (m == null || !m.isEnabled() || MC.player() == null) return;
        ItemStack held = MC.player().field_71071_by.func_70448_g();
        if (held == null) return;
        String item = held.func_77973_b().getClass().getSimpleName().toLowerCase();
        GL11.glPushMatrix();
        if (item.contains("fishingrod")) {
            GL11.glTranslatef(0.06f, -0.06f, -0.05f);
            GL11.glRotatef(-12f, 0f, 0f, 1f);
        } else if (item.contains("bow")) {
            GL11.glTranslatef(0.03f, 0.02f, 0.0f);
            GL11.glRotatef(6f, 0f, 0f, 1f);
        } else if (item.contains("sword")) {
            GL11.glTranslatef(0.02f, -0.01f, 0.0f);
        }
    }

    @Inject(method = "func_78440_a", at = @At("RETURN"))
    private void pix$oldAnimEnd(float partialTicks, CallbackInfo ci) {
        Module m = ModuleManager.byId("oldanimations");
        if (m != null && m.isEnabled()) GL11.glPopMatrix();
    }
}
