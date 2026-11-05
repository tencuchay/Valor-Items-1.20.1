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
 * Fafnir's Talon - relic for the "items" Curios slot.
 *
 * <p>Passive: +6 attack damage, +0.3 attack speed, +10% lifesteal.
 * <p>Unique passive "Dragon's Breath" (5 levels): normal attacks deal additional physical damage equal to a percent
 * of the current health of the target (see DragonsBreathEvents).
 */
public class FafnirsTalonItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 6.0D;
    public static final double ATTACK_SPEED_BONUS = 0.3D;
    /** Added to the lifesteal attribute: 0.10 = 10%. */
    public static final double LIFESTEAL_BONUS = 0.10D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public FafnirsTalonItem(Properties properties) {
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
                Component.translatable("ability.valor_items.fafnirs_talon.dragon.name"),
                level -> Component.translatable("ability.valor_items.fafnirs_talon.dragon.desc",
                        percent(ValorConfig.dragonPercent(level)),
                        DeathSickleItem.formatNumber(ValorConfig.dragonCooldownSeconds())),
                true,
                level -> List.of(Component.translatable("ability.valor_items.fafnirs_talon.dragon.change",
                        percent(ValorConfig.dragonPercent(level)),
                        percent(ValorConfig.dragonPercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.attack_speed",
                        "+" + DeathSickleItem.formatNumber(ATTACK_SPEED_BONUS)),
                Component.translatable("stat.valor_items.lifesteal",
                        "+" + DeathSickleItem.formatNumber(LIFESTEAL_BONUS * 100.0D) + "%"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:fafnirs_talon_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:fafnirs_talon_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(ModAttributes.LIFESTEAL.get(), new AttributeModifier(uuid,
                "valor_items:fafnirs_talon_lifesteal", LIFESTEAL_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** The lifesteal is shown as a percent (+10%) instead of the raw value (+0.1). */
    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        String name = Component.translatable("attribute.valor_items.lifesteal").getString();
        List<Component> result = new ArrayList<>();
        for (Component line : tooltips) {
            if (!line.getString().contains(name)) {
                result.add(line);
            }
        }
        result.add(Component.translatable("stat.valor_items.lifesteal",
                "+" + DeathSickleItem.formatNumber(LIFESTEAL_BONUS * 100.0D) + "%").withStyle(ChatFormatting.BLUE));
        return result;
    }

    /** Highest level of the Fafnir's Talon worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.FAFNIRS_TALON.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof FafnirsTalonItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
