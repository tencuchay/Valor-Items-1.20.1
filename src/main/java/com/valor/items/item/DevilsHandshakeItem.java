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
 * Devil's Handshake - relic for the "items" Curios slot.
 *
 * <p>Passive: +5 attack damage, +6 max health, +0.3 attack speed.
 * <p>Unique passive "Flame Wraith" (5 levels): every normal attack that hits gives a stack (up to 5) that raises the
 * attack speed, movement speed and damage dealt (see FlameWraithEvents).
 */
public class DevilsHandshakeItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 5.0D;
    public static final double MAX_HEALTH_BONUS = 6.0D;
    public static final double ATTACK_SPEED_BONUS = 0.3D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public DevilsHandshakeItem(Properties properties) {
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
                Component.translatable("ability.valor_items.devils_handshake.flame_wraith.name"),
                level -> Component.translatable("ability.valor_items.devils_handshake.flame_wraith.desc",
                        percent(ValorConfig.flamePercent(level)),
                        ValorConfig.flameMaxStacks(),
                        DeathSickleItem.formatNumber(ValorConfig.flameSeconds())),
                true,
                level -> List.of(Component.translatable("ability.valor_items.devils_handshake.flame_wraith.change",
                        percent(ValorConfig.flamePercent(level)),
                        percent(ValorConfig.flamePercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)),
                Component.translatable("stat.valor_items.attack_speed",
                        "+" + DeathSickleItem.formatNumber(ATTACK_SPEED_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:devils_handshake_attack_damage", ATTACK_DAMAGE_BONUS,
                AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:devils_handshake_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:devils_handshake_attack_speed", ATTACK_SPEED_BONUS,
                AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Devil's Handshake worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.DEVILS_HANDSHAKE.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof DevilsHandshakeItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
