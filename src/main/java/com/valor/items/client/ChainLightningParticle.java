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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * One animated bolt stretched between two enemies. It starts at the particle position and ends at
 * position + (dx, dy, dz), the values the server sends as the "speed" of the particle (count 0).
 */
@OnlyIn(Dist.CLIENT)
public class ChainLightningParticle extends TextureSheetParticle {
    /** Size of the frames: 128 x 39 pixels. */
    private static final float ASPECT = 39.0F / 128.0F;

    private static final int FRAMES = 11;
    /** One frame per tick (20 fps). The animation plays frames 1..11, then starts again and stops at frame LAST_REPEAT_FRAME. */
    private static final int LAST_REPEAT_FRAME = 6;

    private final SpriteSet sprites;
    private final Vec3 delta;

    protected ChainLightningParticle(ClientLevel level, double x, double y, double z,
                                     Vec3 delta, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.delta = delta;
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
        if (!this.removed) {
            this.setSprite(this.sprites.get(this.age % FRAMES, FRAMES - 1));
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        // middle of the bolt, relative to the camera
        double mx = this.x + this.delta.x * 0.5D - cam.x;
        double my = this.y + this.delta.y * 0.5D - cam.y;
        double mz = this.z + this.delta.z * 0.5D - cam.z;

        Vec3 along = this.delta.scale(0.5D); // from the middle to the end
        Vec3 up = along.cross(new Vec3(mx, my, mz));
        if (up.lengthSqr() < 1.0E-8D) {
            return; // looking straight along the bolt
        }
        double halfHeight = this.delta.length() * ASPECT * 0.5D;
        up = up.normalize().scale(halfHeight);
        if (up.y < 0.0D) {
            up = up.scale(-1.0D); // keep the picture the right way up
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        vertex(buffer, mx - along.x - up.x, my - along.y - up.y, mz - along.z - up.z, u0, v1, light);
        vertex(buffer, mx - along.x + up.x, my - along.y + up.y, mz - along.z + up.z, u0, v0, light);
        vertex(buffer, mx + along.x + up.x, my + along.y + up.y, mz + along.z + up.z, u1, v0, light);
        vertex(buffer, mx + along.x - up.x, my + along.y - up.y, mz + along.z - up.z, u1, v1, light);
        // back face too: the quad is not a billboard, so culling could hide one side
        vertex(buffer, mx + along.x - up.x, my + along.y - up.y, mz + along.z - up.z, u1, v1, light);
        vertex(buffer, mx + along.x + up.x, my + along.y + up.y, mz + along.z + up.z, u1, v0, light);
        vertex(buffer, mx - along.x + up.x, my - along.y + up.y, mz - along.z + up.z, u0, v0, light);
        vertex(buffer, mx - along.x - up.x, my - along.y - up.y, mz - along.z - up.z, u0, v1, light);
    }

    private void vertex(VertexConsumer buffer, double px, double py, double pz, float u, float v, int light) {
        buffer.vertex(px, py, pz).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
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
            return new ChainLightningParticle(level, x, y, z, new Vec3(dx, dy, dz), this.sprites);
        }
    }
}
