package com.valor.items.client;

import com.valor.items.entity.ArcticIceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ArcticIceRenderer extends GeoEntityRenderer<ArcticIceEntity> {
    public ArcticIceRenderer(EntityRendererProvider.Context context) {
        super(context, new ArcticIceModel());
        this.shadowRadius = 0.0F;
    }

    /** GeckoLib only turns living entities; this one is turned by the yaw it was given when it appeared. */
    @Override
    protected void applyRotations(ArcticIceEntity animatable, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(animatable, poseStack, ageInTicks,
                net.minecraft.util.Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick);
    }

    /** Always full brightness, wherever the ice is. */
    @Override
    public void render(ArcticIceEntity entity, float entityYaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // the owner does not see his own ice from the first person view (the cold overlay is shown instead)
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && entity.getOwnerId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    }

    /** The ice is see-through where its texture is. */
    @Override
    public RenderType getRenderType(ArcticIceEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        // the emissive translucent type has no light or side shading at all
        return RenderType.entityTranslucentEmissive(texture);
    }
}
