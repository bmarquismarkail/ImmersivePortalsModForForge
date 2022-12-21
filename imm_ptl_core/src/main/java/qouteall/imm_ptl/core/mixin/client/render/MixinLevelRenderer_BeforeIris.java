package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;

@Mixin(value = LevelRenderer.class, priority = 900)
public class MixinLevelRenderer_BeforeIris {
    // inject it after Iris, run before Iris
    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"))
    private void iris$beginTranslucents(
            PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci
    ) {
        IPCGlobal.renderer.onBeginIrisTranslucentRendering(pPoseStack);
    }
}
