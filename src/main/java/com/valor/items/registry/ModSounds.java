package com.valor.items.registry;

import com.valor.items.ValorItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ValorItems.MODID);

    /** Played on every shocked enemy of Chain Lightning. */
    public static final RegistryObject<SoundEvent> LIGHTNING_CHAIN = SOUNDS.register("lightning_chain",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ValorItems.MODID, "lightning_chain")));

    private ModSounds() {}
}
