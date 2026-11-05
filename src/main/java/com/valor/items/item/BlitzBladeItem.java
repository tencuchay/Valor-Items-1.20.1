package com.valor.items.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
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
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Blitz Blade - relic for the "items" Curios slot.
 *
 * <p>Passive: +0.35 attack speed and +8% movement speed.
 * <p>Unique passive "Chain Lightning" (5 levels): every basic attack, melee or ranged,
 * deals extra magic damage (vanilla magic damage type) equal to a percentage of the damage of that hit.
 */
public class BlitzBladeItem extends ValorItem {
    public static final String SLOT_ID = "items";

    /** Added to the attack speed attribute (ADDITION). */
    public static final double ATTACK_SPEED_BONUS = 0.35D;
    /** Movement speed multiplier on the base value (MULTIPLY_BASE): 0.08 = +8%. */
    public static final double MOVEMENT_SPEED_BONUS = 0.08D;

    /** Extra magic damage on the target, in percent of the damage of the hit, for levels 1..5. */
    private static final double[] BONUS_DAMAGE_PERCENT = {5.0, 6.25, 7.5, 8.75, 10.0};
    /** Magic damage dealt to each secondary target every 3rd attack, in percent of the damage of the hit. */
    private static final double[] SPREAD_DAMAGE_PERCENT = {15.0, 18.75, 22.5, 26.25, 30.0};
    /** Cooldown of the chain, in ticks (6 ticks = 0.3 seconds). */
    public static final int CHAIN_COOLDOWN_TICKS = 6;
    /** Experience levels to upgrade from level 1->2, 2->3, 3->4, 4->5. */
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public BlitzBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxLevel() {
        return BONUS_DAMAGE_PERCENT.length;
    }

    @Override
    public int getUpgradeCost(int level) {
        return UPGRADE_COST[Mth.clamp(level - 1, 0, UPGRADE_COST.length - 1)];
    }

    @Override
    public String getSlotId() {
        return SLOT_ID;
    }

    /** 5.0 -> "5%", 6.25 -> "6.25%". */
    private static String formatPercent(double value) {
        return (value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value)) + "%";
    }

    public static double getBonusDamagePercent(int level) {
        return BONUS_DAMAGE_PERCENT[Mth.clamp(level, 1, BONUS_DAMAGE_PERCENT.length) - 1];
    }

    public static double getSpreadDamagePercent(int level) {
        return SPREAD_DAMAGE_PERCENT[Mth.clamp(level, 1, SPREAD_DAMAGE_PERCENT.length) - 1];
    }

    @Override
    public List<RelicAbility> getAbilities() {
        return List.of(
                new RelicAbility(
                        Component.translatable("ability.valor_items.blitz_blade.chain_lightning.name"),
                        level -> Component.translatable("ability.valor_items.blitz_blade.chain_lightning.desc",
                                formatPercent(getBonusDamagePercent(level)), formatPercent(getSpreadDamagePercent(level)),
                                CHAIN_COOLDOWN_TICKS / 20.0D),
                        true,
                        level -> List.of(
                                Component.translatable("ability.valor_items.blitz_blade.chain_lightning.change.bonus",
                                        formatPercent(getBonusDamagePercent(level)),
                                        formatPercent(getBonusDamagePercent(level + 1))),
                                Component.translatable("ability.valor_items.blitz_blade.chain_lightning.change.spread",
                                        formatPercent(getSpreadDamagePercent(level)),
                                        formatPercent(getSpreadDamagePercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_speed", "+" + ATTACK_SPEED_BONUS),
                Component.translatable("stat.valor_items.movement_speed",
                        "+" + Math.round(MOVEMENT_SPEED_BONUS * 100) + "%"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:blitz_blade_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid,
                "valor_items:blitz_blade_movement_speed", MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE));
        return modifiers;
    }

    /**
     * Level of the Blitz Blade worn by the entity, or 0 if none is worn.
     * If several are worn, the highest level is used.
     */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.BLITZ_BLADE.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof BlitzBladeItem blade) {
                            best = Math.max(best, blade.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
