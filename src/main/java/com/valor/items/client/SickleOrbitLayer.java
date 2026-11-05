package com.valor.items.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Death Sickle circling around the player while it is ready, followed by a see-through trail.
 * Never drawn in first person (the player's own body is not drawn there either).
 */
@OnlyIn(Dist.CLIENT)
public class SickleOrbitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Distance from the player, in blocks. */
    private static final double RADIUS = 0.85D;
    /** Height in the layer space of a player (0 is at the shoulders, positive is down). */
    private static final double HEIGHT = 0.55D;
    /** Radians per tick. */
    private static final double SPEED = 0.12D;
    private static final float SCALE = 1.35F;
    /** The trail: a glowing ribbon behind the sickle, in radians of the orbit, with its colours and width (blocks). */
    private static final double TRAIL_ARC = 1.5D;
    private static final int TRAIL_SEGMENTS = 28;
    private static final float TRAIL_HALF_WIDTH = 0.17F;
    private static final float[] HEAD_COLOR = {177 / 255.0F, 3 / 255.0F, 252 / 255.0F};   // #b103fc
    private static final float[] TAIL_COLOR = {177 / 255.0F, 3 / 255.0F, 252 / 255.0F};   // #b103fc (it fades out through the alpha)

    public SickleOrbitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (!SickleOrbit.isShown(player) || player.isInvisible() || player.isSpectator()) {
            return;
        }
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        BakedModel model = mc.getModelManager().getModel(SickleOrbit.MODEL);
        double time = ageInTicks * SPEED;
        double bob = Math.sin(ageInTicks * 0.1D) * 0.06D;
        double y = HEIGHT + bob;

        // the orbiting sickle must not get the red tint of Adrenaline that the rest of the player gets.
        // The buffers are asked one at a time: asking for the second one closes the first.
        AdrenalineTint.suppress = true;
        try {
            drawTrail(pose, buffers.getBuffer(RenderType.lightning()), time, y);
        } finally {
            AdrenalineTint.suppress = false;
        }
        AdrenalineTint.suppress = true;
        VertexConsumer base;
        try {
            base = buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        } finally {
            AdrenalineTint.suppress = false;
        }

        pose.pushPose();
        pose.translate(Math.cos(time) * RADIUS, y, Math.sin(time) * RADIUS);
        pose.mulPose(Axis.YP.rotation((float) -time));    // the blade points the way it flies
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));    // layers are drawn upside down
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5D, -0.1D, -0.5D);              // the model is centred on a block
        drawModel(pose, base, model);
        pose.popPose();
    }

    /** A soft ribbon: opaque in the middle, transparent at its edges, fading from the sickle to its tail. */
    private static void drawTrail(PoseStack pose, VertexConsumer consumer, double headAngle, double y) {
        Matrix4f matrix = pose.last().pose();
        for (int k = 0; k < TRAIL_SEGMENTS; k++) {
            float t0 = k / (float) TRAIL_SEGMENTS;
            float t1 = (k + 1) / (float) TRAIL_SEGMENTS;
            double a0 = headAngle - t0 * TRAIL_ARC;
            double a1 = headAngle - t1 * TRAIL_ARC;
            float w0 = TRAIL_HALF_WIDTH * (1.0F - 0.45F * t0);
            float w1 = TRAIL_HALF_WIDTH * (1.0F - 0.45F * t1);
            float alpha0 = 0.85F * (float) Math.pow(1.0F - t0, 1.3D);
            float alpha1 = 0.85F * (float) Math.pow(1.0F - t1, 1.3D);
            float x0 = (float) (Math.cos(a0) * RADIUS);
            float z0 = (float) (Math.sin(a0) * RADIUS);
            float x1 = (float) (Math.cos(a1) * RADIUS);
            float z1 = (float) (Math.sin(a1) * RADIUS);
            float yy = (float) y;
            // lower half and upper half of the ribbon; the edge row has alpha 0
            quad(consumer, matrix, x0, z0, yy - w0, 0.0F, x0, z0, yy, alpha0, x1, z1, yy, alpha1, x1, z1, yy - w1, 0.0F, t0, t1);
            quad(consumer, matrix, x0, z0, yy, alpha0, x0, z0, yy + w0, 0.0F, x1, z1, yy + w1, 0.0F, x1, z1, yy, alpha1, t0, t1);
        }
    }

    private static void quad(VertexConsumer c, Matrix4f m,
                             float ax, float az, float ay, float aa,
                             float bx, float bz, float by, float ba,
                             float cx, float cz, float cy, float ca,
                             float dx, float dz, float dy, float da, float t0, float t1) {
        vertex(c, m, ax, ay, az, t0, aa);
        vertex(c, m, bx, by, bz, t0, ba);
        vertex(c, m, cx, cy, cz, t1, ca);
        vertex(c, m, dx, dy, dz, t1, da);
        // the same quad the other way round: faces that look away from the camera are not drawn
        vertex(c, m, dx, dy, dz, t1, da);
        vertex(c, m, cx, cy, cz, t1, ca);
        vertex(c, m, bx, by, bz, t0, ba);
        vertex(c, m, ax, ay, az, t0, aa);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, float x, float y, float z, float t, float alpha) {
        float r = HEAD_COLOR[0] + (TAIL_COLOR[0] - HEAD_COLOR[0]) * t;
        float g = HEAD_COLOR[1] + (TAIL_COLOR[1] - HEAD_COLOR[1]) * t;
        float b = HEAD_COLOR[2] + (TAIL_COLOR[2] - HEAD_COLOR[2]) * t;
        c.vertex(m, x, y, z).color(r, g, b, alpha).endVertex();
    }

    private static void drawModel(PoseStack pose, VertexConsumer consumer, BakedModel model) {
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            drawQuads(pose, consumer, model.getQuads(null, direction, random));
        }
        drawQuads(pose, consumer, model.getQuads(null, null, random));
    }

    private static void drawQuads(PoseStack pose, VertexConsumer consumer, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}
