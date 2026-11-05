package com.valor.items.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.valor.items.config.ValorConfig;
import com.valor.items.registry.ModAttributes;
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
 * Amulet of Longevity - relic for the "items" Curios slot.
 *
 * <p>Passive: +1.5 armor, +1 magic armor, +15 max health.
 * <p>Unique passive "Longevity" (5 levels): heals a percent of the max health every 2 seconds, more when the wearer
 * was not attacked for 3 seconds (see LongevityEvents).
 */
public class AmuletOfLongevityItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ARMOR_BONUS = 1.5D;
    public static final double MAGIC_ARMOR_BONUS = 1.0D;
    public static final double MAX_HEALTH_BONUS = 15.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public AmuletOfLongevityItem(Properties properties) {
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
                Component.translatable("ability.valor_items.amulet_of_longevity.longevity.name"),
                level -> Component.translatable("ability.valor_items.amulet_of_longevity.longevity.desc",
                        percent(ValorConfig.longevityPercent(level)),
                        DeathSickleItem.formatNumber(ValorConfig.longevityIntervalSeconds()),
                        DeathSickleItem.formatNumber(ValorConfig.longevitySafeSeconds()),
                        percent(ValorConfig.longevityRestPercent(level))),
                true,
                level -> List.of(
                        Component.translatable("ability.valor_items.amulet_of_longevity.longevity.change",
                                percent(ValorConfig.longevityPercent(level)),
                                percent(ValorConfig.longevityPercent(level + 1))),
                        Component.translatable("ability.valor_items.amulet_of_longevity.longevity.change_rest",
                                percent(ValorConfig.longevityRestPercent(level)),
                                percent(ValorConfig.longevityRestPercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.armor", "+" + DeathSickleItem.formatNumber(ARMOR_BONUS)),
                Component.translatable("stat.valor_items.magic_armor",
                        "+" + DeathSickleItem.formatNumber(MAGIC_ARMOR_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid,
                "valor_items:amulet_of_longevity_armor", ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(ModAttributes.MAGIC_ARMOR.get(), new AttributeModifier(uuid,
                "valor_items:amulet_of_longevity_magic_armor", MAGIC_ARMOR_BONUS,
                AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:amulet_of_longevity_max_health", MAX_HEALTH_BONUS,
                AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Amulet of Longevity worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.AMULET_OF_LONGEVITY.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof AmuletOfLongevityItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
