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
 * Odin's Will - relic for the "items" Curios slot.
 *
 * <p>Passive: +2.25 armor, +9 max health, +5% movement speed, +3 attack damage.
 * <p>Unique passive "Adrenaline" (5 levels): every time the wearer takes damage he gains a stack (up to 5) that
 * raises his movement speed and the damage he deals (see AdrenalineEvents).
 */
public class OdinsWillItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ARMOR_BONUS = 2.25D;
    public static final double MAX_HEALTH_BONUS = 9.0D;
    public static final double MOVEMENT_SPEED_BONUS = 0.05D;
    public static final double ATTACK_DAMAGE_BONUS = 3.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public OdinsWillItem(Properties properties) {
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
                Component.translatable("ability.valor_items.odins_will.adrenaline.name"),
                level -> Component.translatable("ability.valor_items.odins_will.adrenaline.desc",
                        percent(ValorConfig.adrenalineSpeedPercent(level)),
                        percent(ValorConfig.adrenalineDamagePercent(level)),
                        ValorConfig.adrenalineMaxStacks(),
                        DeathSickleItem.formatNumber(ValorConfig.adrenalineSeconds())),
                true,
                level -> List.of(
                        Component.translatable("ability.valor_items.odins_will.adrenaline.change_speed",
                                percent(ValorConfig.adrenalineSpeedPercent(level)),
                                percent(ValorConfig.adrenalineSpeedPercent(level + 1))),
                        Component.translatable("ability.valor_items.odins_will.adrenaline.change_damage",
                                percent(ValorConfig.adrenalineDamagePercent(level)),
                                percent(ValorConfig.adrenalineDamagePercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.armor", "+" + DeathSickleItem.formatNumber(ARMOR_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)),
                Component.translatable("stat.valor_items.movement_speed",
                        "+" + Math.round(MOVEMENT_SPEED_BONUS * 100) + "%"),
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid,
                "valor_items:odins_will_armor", ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:odins_will_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid,
                "valor_items:odins_will_movement_speed", MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE));
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:odins_will_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Odin's Will worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.ODINS_WILL.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof OdinsWillItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
