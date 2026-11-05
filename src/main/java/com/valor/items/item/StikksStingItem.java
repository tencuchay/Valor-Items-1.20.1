package com.valor.items.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.valor.items.config.ValorConfig;
import com.valor.items.registry.ModAttributes;
import com.valor.items.registry.ModItems;
import com.valor.items.relic.RelicAbility;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
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
 * Stikk's Sting - relic for the "items" Curios slot.
 *
 * <p>Passive: +4.5 attack damage, +0.35 attack speed, +20% crit rate, +7.5% movement speed.
 * <p>Unique passive "Wind's Blessing" (5 levels): a normal attack that hits gives attack speed and movement speed
 * for a few seconds; a critical hit doubles the effect (see WindsBlessingEvents).
 */
public class StikksStingItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 4.5D;
    public static final double ATTACK_SPEED_BONUS = 0.35D;
    /** Added to the crit rate attribute: 0.20 = 20%. */
    public static final double CRIT_RATE_BONUS = 0.20D;
    /** Movement speed multiplier on the base value (MULTIPLY_BASE): 0.075 = +7.5%. */
    public static final double MOVEMENT_SPEED_BONUS = 0.075D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public StikksStingItem(Properties properties) {
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
                Component.translatable("ability.valor_items.stikks_sting.wind.name"),
                level -> Component.translatable("ability.valor_items.stikks_sting.wind.desc",
                        percent(ValorConfig.windAttackSpeedPercent(level)),
                        percent(ValorConfig.windMoveSpeedPercent(level)),
                        DeathSickleItem.formatNumber(ValorConfig.windSeconds())),
                true,
                level -> List.of(
                        Component.translatable("ability.valor_items.stikks_sting.wind.change.attack",
                                percent(ValorConfig.windAttackSpeedPercent(level)),
                                percent(ValorConfig.windAttackSpeedPercent(level + 1))),
                        Component.translatable("ability.valor_items.stikks_sting.wind.change.move",
                                percent(ValorConfig.windMoveSpeedPercent(level)),
                                percent(ValorConfig.windMoveSpeedPercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.attack_speed",
                        "+" + DeathSickleItem.formatNumber(ATTACK_SPEED_BONUS)),
                Component.translatable("stat.valor_items.crit_rate",
                        "+" + DeathSickleItem.formatNumber(CRIT_RATE_BONUS * 100.0D) + "%"),
                Component.translatable("stat.valor_items.movement_speed",
                        "+" + DeathSickleItem.formatNumber(MOVEMENT_SPEED_BONUS * 100.0D) + "%"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:stikks_sting_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:stikks_sting_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(ModAttributes.CRIT_RATE.get(), new AttributeModifier(uuid,
                "valor_items:stikks_sting_crit_rate", CRIT_RATE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid,
                "valor_items:stikks_sting_movement_speed", MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE));
        return modifiers;
    }

    /** The crit rate is shown as a percent (+20%) instead of the raw value (+0.2). */
    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        String rateName = Component.translatable("attribute.valor_items.crit_rate").getString();
        List<Component> result = new ArrayList<>();
        for (Component line : tooltips) {
            if (!line.getString().contains(rateName)) {
                result.add(line);
            }
        }
        result.add(Component.translatable("stat.valor_items.crit_rate",
                "+" + DeathSickleItem.formatNumber(CRIT_RATE_BONUS * 100.0D) + "%").withStyle(ChatFormatting.BLUE));
        return result;
    }

    /** Highest level of the Stikk's Sting worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.STIKKS_STING.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof StikksStingItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
