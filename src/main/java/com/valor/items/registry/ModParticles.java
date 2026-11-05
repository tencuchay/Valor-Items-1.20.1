package com.valor.items.registry;

import com.valor.items.ValorItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ValorItems.MODID);

    /** The bolt between two enemies. Spawned with count 0: the "speed" is the vector to the next enemy. */
    public static final RegistryObject<SimpleParticleType> CHAIN_LIGHTNING =
            PARTICLES.register("chain_lightning", () -> new SimpleParticleType(false));

    /** The sparks on the enemy that triggers Chain Lightning. Spawned with count 0: the "speed" x is its height. */
    public static final RegistryObject<SimpleParticleType> LIGHTNING_SPARK =
            PARTICLES.register("lightning_spark", () -> new SimpleParticleType(false));

    /** Small flame of Mantle of Ra (6 frames). Spawned with count 0: the "speed" is the velocity. */
    public static final RegistryObject<SimpleParticleType> RA_FIRE =
            PARTICLES.register("ra_fire", () -> new SimpleParticleType(false));

    /** Big swirling flame of Mantle of Ra (6 frames). Spawned with count 0: the "speed" is the velocity. */
    public static final RegistryObject<SimpleParticleType> RA_FLAME =
            PARTICLES.register("ra_flame", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
