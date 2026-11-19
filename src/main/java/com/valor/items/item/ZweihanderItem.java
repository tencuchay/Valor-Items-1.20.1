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
 * Zweihander (Thập Tự Kiếm) - relic for the "items" Curios slot.
 *
 * <p>Passive: +16 magic damage, +0.2 attack speed, +8% movement speed.
 * <p>Unique passive "Rapid Blade" (Cấp Nhận): basic attack hits give attack speed, stacking up to 2 times for 4 s.
 * <p>Unique passive "Twilight" (Thánh Quang): basic attacks deal additional magic damage (5 levels), with a short
 * cooldown (see ZweihanderEvents).
 */
public class ZweihanderItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double MAGIC_DAMAGE_BONUS = 16.0D;
    public static final double ATTACK_SPEED_BONUS = 0.2D;
    /** Movement speed multiplier on the base value (MULTIPLY_BASE): 0.08 = +8%. */
    public static final double MOVEMENT_SPEED_BONUS = 0.08D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public ZweihanderItem(Properties properties) {
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

    @Override
    public List<RelicAbility> getAbilities() {
        return List.of(
                new RelicAbility(
                        Component.translatable("ability.valor_items.zweihander.rapid.name"),
                        level -> Component.translatable("ability.valor_items.zweihander.rapid.desc",
                                DeathSickleItem.formatNumber(ValorConfig.rapidAttackSpeed()),
                                ValorConfig.rapidMaxStacks(),
                                DeathSickleItem.formatNumber(ValorConfig.rapidSeconds())),
                        false,
                        level -> List.of()),
                new RelicAbility(
                        Component.translatable("ability.valor_items.zweihander.twilight.name"),
                        level -> Component.translatable("ability.valor_items.zweihander.twilight.desc",
                                DeathSickleItem.formatNumber(ValorConfig.twilightDamage(level)),
                                DeathSickleItem.formatNumber(ValorConfig.twilightMagicPercent()) + "%",
                                DeathSickleItem.formatNumber(ValorConfig.twilightCooldownSeconds())),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.zweihander.twilight.change",
                                DeathSickleItem.formatNumber(ValorConfig.twilightDamage(level)),
                                DeathSickleItem.formatNumber(ValorConfig.twilightDamage(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.magic_damage",
                        "+" + DeathSickleItem.formatNumber(MAGIC_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.attack_speed",
                        "+" + DeathSickleItem.formatNumber(ATTACK_SPEED_BONUS)),
                Component.translatable("stat.valor_items.movement_speed",
                        "+" + DeathSickleItem.formatNumber(MOVEMENT_SPEED_BONUS * 100.0D) + "%"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(ModAttributes.MAGIC_DAMAGE.get(), new AttributeModifier(uuid,
                "valor_items:zweihander_magic_damage", MAGIC_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:zweihander_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid,
                "valor_items:zweihander_movement_speed", MOVEMENT_SPEED_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE));
        return modifiers;
    }

    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Zweihander worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.ZWEIHANDER.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof ZweihanderItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
