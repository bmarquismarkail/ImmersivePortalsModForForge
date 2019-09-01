package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class RendererDeferred extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering() {
    
    }
    
    @Override
    public void onAfterTranslucentRendering() {
        renderPortals();
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
    
        OFHelper.bindToShaderFrameBuffer();
    
        GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
    
        copyDepthFromMainToDeferred();
    
        if (!testShouldRenderPortal(portal)) {
            return;
        }
    
        portalLayers.push(portal);
    
        manageCameraAndRenderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
    
        portalLayers.pop();
    
        deferredBuffer.fb.beginWrite(true);
    
        RenderHelper.drawFrameBufferUp(portal, mc.getFramebuffer(), CGlobal.shaderManager);
    
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFHelper.bindToShaderFrameBuffer();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        if (shouldRenderPortalInEntityRenderer(portal)) {
            RenderHelper.drawPortalViewTriangle(portal);
        }
    }
    
    private boolean shouldRenderPortalInEntityRenderer(Portal portal) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        if (cameraEntity == null) {
            return false;
        }
        Vec3d cameraPos = cameraEntity.getPos();
        if (Shaders.isShadowPass) {
            return true;
        }
        if (isRendering()) {
            return portal.isInFrontOfPortal(cameraPos);
        }
        return false;
    }
    
    //NOTE it will write to shader depth buffer
    private boolean testShouldRenderPortal(Portal portal) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.disableTexture();
            GlStateManager.colorMask(false, false, false, false);
            RenderHelper.setupCameraTransformation();
            GL20.glUseProgram(0);
        
            RenderHelper.drawPortalViewTriangle(portal);
            
            GlStateManager.enableTexture();
            GlStateManager.colorMask(true, true, true, true);
        });
    }
    
    private void copyDepthFromMainToDeferred() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, deferredBuffer.fb.viewWidth, deferredBuffer.fb.viewHeight,
            GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
        );
    
        OFHelper.bindToShaderFrameBuffer();
    }
    
    @Override
    public void onRenderCenterEnded() {
        if (isRendering()) {
            return;
        }
    
        if (RenderHelper.renderedPortalNum == 0) {
            return;
        }
    
        GlStateManager.enableAlphaTest();
        GlFramebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
    
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = false;
        deferredBuffer.fb.draw(mainFrameBuffer.viewWidth, mainFrameBuffer.viewHeight);
        CGlobal.doDisableAlphaTestWhenRenderingFrameBuffer = true;
    }
}
