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
 * Gaia's Standard - relic for the "items" Curios slot.
 *
 * <p>Passive: +2 magic armor, +12 max health.
 * <p>Unique passive "Restore" (5 levels): when taking damage, recover a percent of the max health over a few seconds.
 * <p>Unique passive "Bloodline" (no levels): the more health is lost, the stronger the healing (see GaiaEvents).
 */
public class GaiasStandardItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double MAGIC_ARMOR_BONUS = 2.0D;
    public static final double MAX_HEALTH_BONUS = 12.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public GaiasStandardItem(Properties properties) {
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
        return List.of(
                new RelicAbility(
                        Component.translatable("ability.valor_items.gaias_standard.restore.name"),
                        level -> Component.translatable("ability.valor_items.gaias_standard.restore.desc",
                                percent(ValorConfig.gaiaRestorePercent(level)),
                                DeathSickleItem.formatNumber(ValorConfig.gaiaRestoreSeconds()),
                                DeathSickleItem.formatNumber(ValorConfig.gaiaCooldownSeconds())),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.gaias_standard.restore.change",
                                percent(ValorConfig.gaiaRestorePercent(level)),
                                percent(ValorConfig.gaiaRestorePercent(level + 1))))),
                new RelicAbility(
                        Component.translatable("ability.valor_items.gaias_standard.bloodline.name"),
                        level -> Component.translatable("ability.valor_items.gaias_standard.bloodline.desc",
                                percent(ValorConfig.gaiaBloodlineStepPercent()),
                                percent(ValorConfig.gaiaBloodlineBonusPercent())),
                        false,
                        level -> List.of()));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.magic_armor",
                        "+" + DeathSickleItem.formatNumber(MAGIC_ARMOR_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(ModAttributes.MAGIC_ARMOR.get(), new AttributeModifier(uuid,
                "valor_items:gaias_standard_magic_armor", MAGIC_ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:gaias_standard_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Gaia's Standard worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.GAIAS_STANDARD.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof GaiasStandardItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
