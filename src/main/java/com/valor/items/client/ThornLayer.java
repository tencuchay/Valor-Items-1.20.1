package com.valor.items.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.valor.items.ValorItems;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * While Countersiege of the Mail of Pain is active, 24 thorns around the player grow and shrink again and again,
 * each one at its own random pace, like a ball of spikes. Never drawn in first person.
 */
@OnlyIn(Dist.CLIENT)
public class ThornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ValorItems.MODID, "textures/entity/thorn.png");
    private static final int COUNT = 24;
    /** Centre of the ball in the layer space of a player (0 is at the shoulders, positive is down). */
    private static final float CENTER_Y = 0.6F;
    /** Size of the shape the thorns start from: it follows the body (half width, half depth, half height; blocks). */
    private static final float RADIUS_X = 0.27F;
    private static final float RADIUS_Z = 0.14F;
    private static final float RADIUS_Y = 0.85F;
    /** How deep inside the body a thorn starts, so that it grows out of the player and not out of the air. */
    private static final float SINK = 0.2F;
    private static final float MAX_LENGTH = 1.0F;
    private static final float HALF_WIDTH = 0.14F;
    private static final int FADE_TICKS = 8;

    /** entity id -> {end game time, total ticks} */
    private static final Map<Integer, long[]> ACTIVE = new ConcurrentHashMap<>();

    private static final float[][] DIRECTIONS = new float[COUNT][3];
    private static final float[][] PACE = new float[COUNT][4]; // two speeds and two phases

    static {
        Random random = new Random(24L);
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < COUNT; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / COUNT;
            double r = Math.sqrt(1.0D - y * y);
            double angle = golden * i;
            DIRECTIONS[i][0] = (float) (Math.cos(angle) * r);
            DIRECTIONS[i][1] = (float) y;
            DIRECTIONS[i][2] = (float) (Math.sin(angle) * r);
            PACE[i][0] = 0.10F + random.nextFloat() * 0.22F;
            PACE[i][1] = 0.25F + random.nextFloat() * 0.35F;
            PACE[i][2] = random.nextFloat() * 6.2832F;
            PACE[i][3] = random.nextFloat() * 6.2832F;
        }
    }

    public static void mark(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks <= 0 || mc.level == null) {
            ACTIVE.remove(entityId);
        } else {
            ACTIVE.put(entityId, new long[] {mc.level.getGameTime() + ticks, ticks});
        }
    }

    public ThornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        long[] state = ACTIVE.get(player.getId());
        if (state == null || mc.level == null || player.isInvisible() || player.isSpectator()) {
            return;
        }
        double now = mc.level.getGameTime() + partialTick;
        double left = state[0] - now;
        if (left <= 0.0D) {
            ACTIVE.remove(player.getId());
            return;
        }
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        // the thorns grow at the start and shrink at the end of Countersiege
        double elapsed = state[1] - left;
        float envelope = (float) Math.min(1.0D, Math.min(elapsed / FADE_TICKS, left / FADE_TICKS));

        AdrenalineTint.suppress = true;
        VertexConsumer consumer;
        try {
            consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        } finally {
            AdrenalineTint.suppress = false;
        }
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        double time = ageInTicks + player.getId() * 17.0D;
        for (int i = 0; i < COUNT; i++) {
            float[] pace = PACE[i];
            double wave = 0.6D * Math.sin(time * pace[0] + pace[2]) + 0.4D * Math.sin(time * pace[1] + pace[3]);
            // out for a while, in for a while: the middle of the wave is what makes it pop in and out
            float extension = smoothstep(-0.25F, 0.55F, (float) wave) * envelope;
            if (extension <= 0.02F) {
                continue;
            }
            drawThorn(consumer, matrix, normal, light, DIRECTIONS[i], extension * MAX_LENGTH);
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0F, Math.min(1.0F, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    /** One thorn: two crossed quads along the direction, its base on the ball and its tip pointing away. */
    private static void drawThorn(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float[] d, float length) {
        float dx = d[0];
        float dy = d[1];
        float dz = d[2];
        float bx = dx * RADIUS_X;
        float by = CENTER_Y + dy * RADIUS_Y;
        float bz = dz * RADIUS_Z;
        // direction of the thorn: the normal of the ellipsoid
        float ux = dx / RADIUS_X;
        float uy = dy / RADIUS_Y;
        float uz = dz / RADIUS_Z;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        // two directions perpendicular to the thorn
        float ax = Math.abs(uy) > 0.9F ? 1.0F : 0.0F;
        float ay = Math.abs(uy) > 0.9F ? 0.0F : 1.0F;
        float p1x = uy * 0.0F - uz * ay;
        float p1y = uz * ax - ux * 0.0F;
        float p1z = ux * ay - uy * ax;
        float pl = (float) Math.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
        p1x /= pl;
        p1y /= pl;
        p1z /= pl;
        float p2x = uy * p1z - uz * p1y;
        float p2y = uz * p1x - ux * p1z;
        float p2z = ux * p1y - uy * p1x;
        // the base is inside the body; with length 0 the tip is still hidden inside it
        float sx = bx - ux * SINK;
        float sy = by - uy * SINK;
        float sz = bz - uz * SINK;
        float reach = length - SINK * 0.5F;
        float tx = bx + ux * reach;
        float ty = by + uy * reach;
        float tz = bz + uz * reach;
        quad(c, m, n, light, sx, sy, sz, tx, ty, tz, p1x * HALF_WIDTH, p1y * HALF_WIDTH, p1z * HALF_WIDTH, ux, uy, uz);
        quad(c, m, n, light, sx, sy, sz, tx, ty, tz, p2x * HALF_WIDTH, p2y * HALF_WIDTH, p2z * HALF_WIDTH, ux, uy, uz);
    }

    private static void quad(VertexConsumer c, Matrix4f m, Matrix3f n, int light,
                             float sx, float sy, float sz, float tx, float ty, float tz,
                             float px, float py, float pz, float nx, float ny, float nz) {
        vertex(c, m, n, light, sx - px, sy - py, sz - pz, 0.0F, 1.0F, nx, ny, nz);
        vertex(c, m, n, light, sx + px, sy + py, sz + pz, 1.0F, 1.0F, nx, ny, nz);
        vertex(c, m, n, light, tx + px, ty + py, tz + pz, 1.0F, 0.0F, nx, ny, nz);
        vertex(c, m, n, light, tx - px, ty - py, tz - pz, 0.0F, 0.0F, nx, ny, nz);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, Matrix3f n, int light, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz) {
        c.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
