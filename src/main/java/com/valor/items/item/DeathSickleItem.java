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
 * Death Sickle - relic for the "items" Curios slot.
 *
 * <p>Passive: +7.5 attack damage, +0.15 attack speed, +5 max health.
 * <p>Unique passive "Insatiable" (5 levels): a hit that would kill the wearer leaves them at 1 HP and makes them
 * immortal for a few seconds (immune to all damage and effects, faster); see DeathSickleEvents.
 */
public class DeathSickleItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 7.5D;
    public static final double ATTACK_SPEED_BONUS = 0.15D;
    public static final double MAX_HEALTH_BONUS = 5.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public DeathSickleItem(Properties properties) {
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

    /** 1.5 -> "1.5", 2.0 -> "2". */
    public static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public List<RelicAbility> getAbilities() {
        return List.of(new RelicAbility(
                Component.translatable("ability.valor_items.death_sickle.insatiable.name"),
                level -> Component.translatable("ability.valor_items.death_sickle.insatiable.desc",
                        formatNumber(ValorConfig.sickleDurationSeconds(level)),
                        formatNumber(ValorConfig.sickleMovementSpeedPercent()) + "%",
                        formatNumber(ValorConfig.sickleAttackSpeedBonus()),
                        ValorConfig.sickleCooldownSeconds()),
                true,
                level -> List.of(Component.translatable("ability.valor_items.death_sickle.insatiable.change.duration",
                        formatNumber(ValorConfig.sickleDurationSeconds(level)),
                        formatNumber(ValorConfig.sickleDurationSeconds(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage", "+" + formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.attack_speed", "+" + formatNumber(ATTACK_SPEED_BONUS)),
                Component.translatable("stat.valor_items.max_health", "+" + formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:death_sickle_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:death_sickle_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:death_sickle_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Death Sickle worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.DEATH_SICKLE.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof DeathSickleItem sickle) {
                            best = Math.max(best, sickle.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
