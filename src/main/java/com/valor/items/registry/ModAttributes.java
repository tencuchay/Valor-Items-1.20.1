package com.valor.items.registry;

import com.valor.items.ValorItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ValorItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ValorItems.MODID);

    /** Magic armor: like armor, but only against magic damage (see MagicArmorEvents). */
    public static final RegistryObject<Attribute> MAGIC_ARMOR = ATTRIBUTES.register("magic_armor",
            () -> new RangedAttribute("attribute.valor_items.magic_armor", 0.0D, 0.0D, 1000.0D).setSyncable(true));

    /** Damage of bows, crossbows and thrown tridents that is added on top of the damage of the projectile. */
    public static final RegistryObject<Attribute> RANGED_ATTACK = ATTRIBUTES.register("ranged_attack",
            () -> new RangedAttribute("attribute.valor_items.ranged_attack", 0.0D, 0.0D, 1024.0D).setSyncable(true));

    /** Chance of a critical hit: 0 = 0%, 1 = 100%. */
    public static final RegistryObject<Attribute> CRIT_RATE = ATTRIBUTES.register("crit_rate",
            () -> new RangedAttribute("attribute.valor_items.crit_rate", 0.0D, 0.0D, 10.0D).setSyncable(true));

    /** Damage multiplier of a critical hit: 2 = 200%. */
    public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register("crit_damage",
            () -> new RangedAttribute("attribute.valor_items.crit_damage", 2.0D, 1.0D, 100.0D).setSyncable(true));

    /**
     * Magic damage ("cong phep"), the magic counterpart of the attack damage: relics that deal magic damage scale from
     * it, and other relics add to it. Default 0 (the player has no magic damage of his own).
     */
    public static final RegistryObject<Attribute> MAGIC_DAMAGE = ATTRIBUTES.register("magic_damage",
            () -> new RangedAttribute("attribute.valor_items.magic_damage", 0.0D, 0.0D, 100000.0D).setSyncable(true));

    /** Health regenerated every 5 seconds (a fifth of it every second, see HealthRegenEvents). */
    public static final RegistryObject<Attribute> HEALTH_REGEN = ATTRIBUTES.register("health_regen",
            () -> new RangedAttribute("attribute.valor_items.health_regen", 0.0D, 0.0D, 100000.0D).setSyncable(true));

    /** The shield ("giap ao"): its base value is the points of shield the entity has now (see HerculesEvents). */
    public static final RegistryObject<Attribute> SHIELD = ATTRIBUTES.register("shield",
            () -> new RangedAttribute("attribute.valor_items.shield", 0.0D, 0.0D, 100000.0D).setSyncable(true));

    /** Maximum mana. The base is 400; every experience level reached for the first time adds 10 (see ManaEvents). */
    public static final RegistryObject<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.valor_items.max_mana", 400.0D, 0.0D, 1000000.0D).setSyncable(true));

    /** Mana: its base value is the mana the entity has now (never above max_mana). */
    public static final RegistryObject<Attribute> MANA = ATTRIBUTES.register("mana",
            () -> new RangedAttribute("attribute.valor_items.mana", 0.0D, 0.0D, 1000000.0D).setSyncable(true));

    /** Mana regenerated every 5 seconds (a fifth of it every second): 40 for every player. */
    public static final RegistryObject<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
            () -> new RangedAttribute("attribute.valor_items.mana_regen", 40.0D, 0.0D, 1000000.0D).setSyncable(true));

    /** Lifesteal: part of the physical damage dealt that is healed back. 0.10 = 10%. */
    public static final RegistryObject<Attribute> LIFESTEAL = ATTRIBUTES.register("lifesteal",
            () -> new RangedAttribute("attribute.valor_items.lifesteal", 0.0D, 0.0D, 100.0D).setSyncable(true));

    /** Magic lifesteal: part of the magic damage dealt that is healed back. 0.10 = 10%. */
    public static final RegistryObject<Attribute> MAGIC_LIFESTEAL = ATTRIBUTES.register("magic_lifesteal",
            () -> new RangedAttribute("attribute.valor_items.magic_lifesteal", 0.0D, 0.0D, 100.0D).setSyncable(true));

    /** Magic shield ("giap ao phep"): like the shield, but only magic damage is stopped by it. Base value = points left. */
    public static final RegistryObject<Attribute> MAGIC_SHIELD = ATTRIBUTES.register("magic_shield",
            () -> new RangedAttribute("attribute.valor_items.magic_shield", 0.0D, 0.0D, 100000.0D).setSyncable(true));

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAGIC_ARMOR.get());
        event.add(EntityType.PLAYER, RANGED_ATTACK.get());
        event.add(EntityType.PLAYER, CRIT_RATE.get());
        event.add(EntityType.PLAYER, CRIT_DAMAGE.get());
        event.add(EntityType.PLAYER, MAGIC_DAMAGE.get());
        event.add(EntityType.PLAYER, HEALTH_REGEN.get());
        event.add(EntityType.PLAYER, SHIELD.get());
        event.add(EntityType.PLAYER, MAX_MANA.get());
        event.add(EntityType.PLAYER, MANA.get());
        event.add(EntityType.PLAYER, MANA_REGEN.get());
        event.add(EntityType.PLAYER, LIFESTEAL.get());
        event.add(EntityType.PLAYER, MAGIC_SHIELD.get());
        event.add(EntityType.PLAYER, MAGIC_LIFESTEAL.get());
    }

    private ModAttributes() {}
}
