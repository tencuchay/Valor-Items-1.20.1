package com.valor.items.registry;

import com.valor.items.ValorItems;
import com.valor.items.entity.ArcticIceEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ValorItems.MODID);

    /** The ice that encases the player while Impervious lasts (only a visual). */
    public static final RegistryObject<EntityType<ArcticIceEntity>> ARCTIC_ICE = ENTITIES.register("arctic_ice",
            () -> EntityType.Builder.<ArcticIceEntity>of(ArcticIceEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).noSave().noSummon().fireImmune()
                    .clientTrackingRange(10).updateInterval(20).build("arctic_ice"));

    private ModEntities() {}
}
