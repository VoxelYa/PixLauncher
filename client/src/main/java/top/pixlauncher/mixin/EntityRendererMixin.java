package top.pixlauncher.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import java.nio.FloatBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.pixlauncher.events.Events;
import top.pixlauncher.module.Module;
import top.pixlauncher.module.ModuleManager;
import top.pixlauncher.module.modules.OptimizeModules;
import top.pixlauncher.module.modules.RenderModules;
import top.pixlauncher.util.MC;

/**
 * EntityRenderer hooks: world render event, FOV control, zoom, hurt-cam
 * removal, custom fog and simple framebuffer motion blur.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "func_175068_a", at = @At("HEAD"))
    private void pix$onRenderWorld(int pass, float partialTicks, long frameTime, CallbackInfo ci) {
        if (pass == 0 && MC.inGame()) Events.fire(new Events.Render3D(partialTicks));
    }

    /** FOV: applies FOV Control + Smooth Zoom. HEAD (not RETURN) — mixin 0.7
     *  weaves un-cancellable callback sites on multi-return primitives. */
    @Inject(method = "func_78481_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onFov(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!MC.inGame()) return;
        Module fov = ModuleManager.byId("customfov");
        Module zoom = ModuleManager.byId("smoothzoom");
        boolean fovOn = fov != null && fov.isEnabled();
        boolean zoomOn = zoom != null && zoom.isEnabled();
        if (!fovOn && !zoomOn) return;
        float value = MC.settings().field_74334_X; // user FOV — modifiers stripped
        if (zoomOn) value *= ((OptimizeModules.SmoothZoom) zoom).currentFactor();
        cir.setReturnValue(value);
    }

    /** Hurt cam removal. */
    @Inject(method = "func_78482_e", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onHurtCam(float partialTicks, CallbackInfo ci) {
        Module m = ModuleManager.byId("nohurtcam");
        if (m != null && m.isEnabled()) ci.cancel();
    }

    /** Custom fog: replace the vanilla fog parameters when enabled. */
    @Inject(method = "func_78468_a", at = @At("HEAD"), cancellable = true, require = 0)
    private void pix$onFog(int fogMode, float partialTicks, CallbackInfo ci) {
        Module m = ModuleManager.byId("customfog");
        if (m == null || !m.isEnabled() || MC.player() == null) return;
        RenderModules.CustomFog fog = (RenderModules.CustomFog) m;
        ci.cancel();

        int argb = fog.color.get();
        GL11.glFogf(GL11.GL_FOG_START, 16f * fog.distance.get() * (fogMode == 0 ? 4f : 1f));
        GL11.glFogf(GL11.GL_FOG_END, 256f * Math.max(0.15f, fog.distance.get()));
        GL11.glFogi(GL11.GL_FOG_MODE, fogMode == 0 ? GL11.GL_LINEAR : GL11.GL_EXP);
        FloatBuffer fogColor = org.lwjgl.BufferUtils.createFloatBuffer(16);
        fogColor.put(new float[]{
                ((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, 1f});
        fogColor.flip();
        GL11.glFog(GL11.GL_FOG_COLOR, fogColor);
    }

    /**
     * Motion blur (framebuffer feedback): after the frame is complete, blend
     * the framebuffer's own texture back over it at low alpha — previous-frame
     * remnants trail behind moving objects.
     */
    @Inject(method = "func_181560_a", at = @At("RETURN"), require = 0)
    private void pix$onUpdateCameraAndRender(float partialTicks, long frameTime, CallbackInfo ci) {
        Module m = ModuleManager.byId("motionblur");
        if (m == null || !m.isEnabled() || MC.mc().field_71441_e == null) return;
        RenderModules.MotionBlur blur = (RenderModules.MotionBlur) m;
        float alpha = blur.alpha();
        if (alpha <= 0.01f) return;

        net.minecraft.client.shader.Framebuffer fb = MC.mc().func_147110_a();
        if (fb == null) return;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0d, 1d, 1d, 0d, -1d, 1d);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GlStateManager.func_179129_p(); // disable depth
        GlStateManager.func_179147_l(); // enable blend
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.func_179145_e(); // enable texture
        fb.func_147612_c();             // bindFramebufferTexture — sample last frame

        GL11.glColor4f(1f, 1f, 1f, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(0f, 0f);
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(1f, 0f);
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(1f, 1f);
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(0f, 1f);
        GL11.glEnd();

        fb.func_147606_d();             // unbindFramebufferTexture
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GlStateManager.func_179084_k(); // disable blend
        GlStateManager.func_179126_j(); // enable depth

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }
}
