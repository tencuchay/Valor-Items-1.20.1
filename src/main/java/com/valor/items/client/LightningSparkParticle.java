package com.valor.items.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One animated spark effect on the enemy that triggers Chain Lightning. It always faces the camera.
 * The height of the effect is sent by the server as the "speed" x of the particle (count 0).
 */
@OnlyIn(Dist.CLIENT)
public class LightningSparkParticle extends TextureSheetParticle {
    /** Size of the frames: 80 x 128 pixels. */
    private static final float WIDTH_PER_HEIGHT = 80.0F / 128.0F;

    private static final int FRAMES = 11;
    /** One frame per tick (20 fps). The animation plays frames 1..11, then starts again and stops at frame LAST_REPEAT_FRAME. */
    private static final int LAST_REPEAT_FRAME = 6;

    private final SpriteSet sprites;
    private final float halfHeight;
    /** The enemy the sparks are attached to: they move with it. */
    private final Entity target;

    protected LightningSparkParticle(ClientLevel level, double x, double y, double z,
                                     float height, int entityId, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.halfHeight = height * 0.5F;
        this.target = entityId >= 0 ? level.getEntity(entityId) : null;
        this.lifetime = FRAMES + LAST_REPEAT_FRAME - 1;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.setSprite(sprites.get(0, 10));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.target != null && !this.target.isRemoved()) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.x = this.target.getX();
            this.y = this.target.getY() + this.target.getBbHeight() * 0.5D;
            this.z = this.target.getZ();
        }
        if (!this.removed) {
            this.setSprite(this.sprites.get(this.age % FRAMES, FRAMES - 1));
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x);
        float cy = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y);
        float cz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z);
        float halfWidth = this.halfHeight * WIDTH_PER_HEIGHT;
        Quaternionf rotation = camera.rotation();

        Vector3f[] corners = {
                new Vector3f(-halfWidth, -this.halfHeight, 0.0F),
                new Vector3f(-halfWidth, this.halfHeight, 0.0F),
                new Vector3f(halfWidth, this.halfHeight, 0.0F),
                new Vector3f(halfWidth, -this.halfHeight, 0.0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation).add(cx, cy, cz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        vertex(buffer, corners[0], u1, v1, light);
        vertex(buffer, corners[1], u1, v0, light);
        vertex(buffer, corners[2], u0, v0, light);
        vertex(buffer, corners[3], u0, v1, light);
    }

    private void vertex(VertexConsumer buffer, Vector3f p, float u, float v, int light) {
        buffer.vertex(p.x(), p.y(), p.z()).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public boolean shouldCull() {
        return false;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new LightningSparkParticle(level, x, y, z, (float) dx, (int) dy, this.sprites);
        }
    }
}
