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
 * Hercules' Madness - relic for the "items" Curios slot.
 *
 * <p>Passive: +7.5 attack damage, +9 max health.
 * <p>Unique passive "Berserk": normal attacks deal extra damage equal to a percent of the max health, more when the
 * health is low.
 * <p>Unique active "Blood Madness": drains health, then gives a shield (see HerculesEvents).
 */
public class HerculesMadnessItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 7.5D;
    public static final double MAX_HEALTH_BONUS = 9.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public HerculesMadnessItem(Properties properties) {
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
                        Component.translatable("ability.valor_items.hercules_madness.berserk.name"),
                        level -> Component.translatable("ability.valor_items.hercules_madness.berserk.desc",
                                percent(ValorConfig.berserkPercent(level)),
                                percent(ValorConfig.berserkThresholdPercent()),
                                percent(ValorConfig.berserkLowMinPercent()),
                                percent(ValorConfig.berserkLowMaxPercent())),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.hercules_madness.berserk.change",
                                percent(ValorConfig.berserkPercent(level)),
                                percent(ValorConfig.berserkPercent(level + 1))))),
                new RelicAbility(
                        Component.translatable("ability.valor_items.hercules_madness.madness.name"),
                        level -> Component.translatable("ability.valor_items.hercules_madness.madness.desc",
                                percent(ValorConfig.madnessDrainPercent()),
                                percent(ValorConfig.madnessShieldPercent(level)),
                                DeathSickleItem.formatNumber(ValorConfig.madnessShieldSeconds()),
                                ValorConfig.madnessCooldownSeconds(),
                                ValorItem.activeKeys()),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.hercules_madness.madness.change",
                                percent(ValorConfig.madnessShieldPercent(level)),
                                percent(ValorConfig.madnessShieldPercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:hercules_madness_attack_damage", ATTACK_DAMAGE_BONUS,
                AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:hercules_madness_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Hercules' Madness worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.HERCULES_MADNESS.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof HerculesMadnessItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
