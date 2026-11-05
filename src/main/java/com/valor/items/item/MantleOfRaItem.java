package com.valor.items.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.valor.items.config.ValorConfig;
import com.valor.items.registry.ModItems;
import com.valor.items.relic.RelicAbility;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Mantle of Ra - relic for the "items" Curios slot.
 *
 * <p>Passive: +10 max health, +2.25 armor, +0.6 health regeneration every 5 seconds.
 * <p>Unique passive "Burn" (5 levels): every second, magic damage in percent of the max health of every enemy
 * around the wearer, more against arthropods (see MantleOfRaEvents).
 */
public class MantleOfRaItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double MAX_HEALTH_BONUS = 10.0D;
    public static final double ARMOR_BONUS = 2.25D;
    /** Health regenerated every 5 seconds (applied as a fifth of it every second). */
    public static final double REGEN_PER_5_SECONDS = 0.6D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public MantleOfRaItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getUpgradeCost(int level) {
        return UPGRADE_COST[Mth.clamp(level - 1, 0, UPGRADE_COST.length - 1)];
    }

    @Override
    public String getSlotId() {
        return SLOT_ID;
    }

    private static String percent(double value) {
        return DeathSickleItem.formatNumber(value) + "%";
    }

    @Override
    public List<RelicAbility> getAbilities() {
        return List.of(new RelicAbility(
                Component.translatable("ability.valor_items.mantle_of_ra.burn.name"),
                level -> Component.translatable("ability.valor_items.mantle_of_ra.burn.desc",
                        percent(ValorConfig.burnPercent(level)),
                        DeathSickleItem.formatNumber(ValorConfig.burnRange(level)),
                        percent(ValorConfig.burnArthropodBonusPercent())),
                true,
                level -> List.of(Component.translatable("ability.valor_items.mantle_of_ra.burn.change",
                        percent(ValorConfig.burnPercent(level)),
                        percent(ValorConfig.burnPercent(level + 1))),
                        Component.translatable("ability.valor_items.mantle_of_ra.burn.change_range",
                                DeathSickleItem.formatNumber(ValorConfig.burnRange(level)),
                                DeathSickleItem.formatNumber(ValorConfig.burnRange(level + 1))))),
                new RelicAbility(
                        Component.translatable("ability.valor_items.mantle_of_ra.soul_prison.name"),
                        level -> Component.translatable("ability.valor_items.mantle_of_ra.soul_prison.desc",
                                percent(ValorConfig.soulPrisonReducePercent()),
                                DeathSickleItem.formatNumber(ValorConfig.soulPrisonSeconds())),
                        false,
                        level -> List.of()));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)),
                Component.translatable("stat.valor_items.armor", "+" + DeathSickleItem.formatNumber(ARMOR_BONUS)),
                Component.translatable("stat.valor_items.health_regen",
                        "+" + DeathSickleItem.formatNumber(REGEN_PER_5_SECONDS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:mantle_of_ra_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid,
                "valor_items:mantle_of_ra_armor", ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(com.valor.items.registry.ModAttributes.HEALTH_REGEN.get(), new AttributeModifier(uuid,
                "valor_items:mantle_of_ra_health_regen", REGEN_PER_5_SECONDS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Mantle of Ra worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.MANTLE_OF_RA.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof MantleOfRaItem mantle) {
                            best = Math.max(best, mantle.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
