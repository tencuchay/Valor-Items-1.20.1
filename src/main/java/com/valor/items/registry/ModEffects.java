package com.valor.items.registry;

import com.valor.items.ValorItems;
import com.valor.items.effect.SoulPrisonEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ValorItems.MODID);

    /** Soul Prison (Tróc Nã): the target recovers less health. */
    public static final RegistryObject<MobEffect> SOUL_PRISON =
            EFFECTS.register("soul_prison", SoulPrisonEffect::new);

    private ModEffects() {}
}
