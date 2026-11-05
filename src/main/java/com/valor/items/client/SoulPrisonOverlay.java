package com.valor.items.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.valor.items.ValorItems;
import com.valor.items.registry.ModEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Draws the icon of Soul Prison above the head of the entities that have the effect (65% opaque). */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class SoulPrisonOverlay {
    private static final ResourceLocation ICON =
            new ResourceLocation(ValorItems.MODID, "textures/mob_effect/soul_prison.png");
    private static final float ALPHA = 0.65F;
    private static final float HALF_SIZE = 0.25F;

    /** entity id -> game time (client level) until which it has Soul Prison. */
    private static final java.util.Map<Integer, Long> ACTIVE = new java.util.HashMap<>();

    public static void mark(int entityId, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ACTIVE.put(entityId, minecraft.level.getGameTime() + ticks);
        }
    }

    private static boolean isActive(Entity entity, long now) {
        Long until = ACTIVE.get(entity.getId());
        if (until == null) {
            return false;
        }
        if (until < now) {
            ACTIVE.remove(entity.getId());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick();
        long now = minecraft.level.getGameTime();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ICON);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isInvisible()
                    || !(living.hasEffect(ModEffects.SOUL_PRISON.get()) || isActive(living, now))) {
                continue;
            }
            if (entity == camera.getEntity() && !camera.isDetached()) {
                continue; // first person: the icon would be inside the screen
            }
            double x = Mth.lerp(partial, entity.xOld, entity.getX()) - cam.x;
            double y = Mth.lerp(partial, entity.yOld, entity.getY()) - cam.y + entity.getBbHeight() + 0.55D;
            double z = Mth.lerp(partial, entity.zOld, entity.getZ()) - cam.z;

            PoseStack pose = new PoseStack();
            pose.mulPoseMatrix(event.getPoseStack().last().pose());
            pose.translate(x, y, z);
            pose.mulPose(camera.rotation());
            Matrix4f m = pose.last().pose();

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.vertex(m, HALF_SIZE, -HALF_SIZE, 0.0F).uv(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
            buffer.vertex(m, -HALF_SIZE, -HALF_SIZE, 0.0F).uv(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
            buffer.vertex(m, -HALF_SIZE, HALF_SIZE, 0.0F).uv(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
            buffer.vertex(m, HALF_SIZE, HALF_SIZE, 0.0F).uv(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, ALPHA).endVertex();
            Tesselator.getInstance().end();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private SoulPrisonOverlay() {}
}
