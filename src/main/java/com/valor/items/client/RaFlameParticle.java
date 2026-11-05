package com.valor.items.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A flame of the Mantle of Ra: always facing the camera, always fully lit, plays its 6 frames once.
 * The server sends the velocity as the "speed" of the particle (count 0).
 */
@OnlyIn(Dist.CLIENT)
public class RaFlameParticle extends TextureSheetParticle {
    /** Must be the same as RING_LIFETIME_TICKS of MantleOfRaEvents (it sizes the ring). */
    private static final int LIFETIME = 12;
    private static final int FRAMES = 6;

    private final SpriteSet sprites;
    /** Every flame fades during its own number of ticks, so they do not vanish all at once. */
    private final float fadeTicks;

    protected RaFlameParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, float size, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.lifetime = LIFETIME - this.random.nextInt(3);
        this.fadeTicks = 2.0F + this.random.nextFloat() * 5.0F;
        this.quadSize = size * (0.85F + this.random.nextFloat() * 0.3F);
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.setSprite(sprites.get(0, FRAMES - 1));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            int frame = Math.min(this.age * FRAMES / this.lifetime, FRAMES - 1);
            this.setSprite(this.sprites.get(frame, FRAMES - 1));
            // fades out during the last ticks (random per flame)
            this.alpha = Math.min(1.0F, (this.lifetime - this.age) / this.fadeTicks);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;

        public Provider(SpriteSet sprites, float size) {
            this.sprites = sprites;
            this.size = size;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new RaFlameParticle(level, x, y, z, dx, dy, dz, this.size, this.sprites);
        }
    }
}
