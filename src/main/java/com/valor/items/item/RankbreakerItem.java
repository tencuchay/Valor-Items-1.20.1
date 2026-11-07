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
 * Rankbreaker - relic for the "items" Curios slot.
 *
 * <p>Passive: +10 attack damage.
 * <p>Unique passive "Wildwalk": movement speed while no enemy has hurt the wearer for a few seconds.
 * <p>Unique passive "Break": armor penetration, grows with the level (see RankbreakerEvents).
 */
public class RankbreakerItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 10.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public RankbreakerItem(Properties properties) {
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
                        Component.translatable("ability.valor_items.rankbreaker.wildwalk.name"),
                        level -> Component.translatable("ability.valor_items.rankbreaker.wildwalk.desc",
                                DeathSickleItem.formatNumber(ValorConfig.wildwalkSpeedPercent()) + "%",
                                ValorConfig.wildwalkSeconds()),
                        false, level -> List.of()),
                new RelicAbility(
                        Component.translatable("ability.valor_items.rankbreaker.break.name"),
                        level -> Component.translatable("ability.valor_items.rankbreaker.break.desc",
                                DeathSickleItem.formatNumber(ValorConfig.breakArmorPenetration(level))),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.rankbreaker.break.change",
                                DeathSickleItem.formatNumber(ValorConfig.breakArmorPenetration(level)),
                                DeathSickleItem.formatNumber(ValorConfig.breakArmorPenetration(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(Component.translatable("stat.valor_items.attack_damage",
                "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:rankbreaker_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Rankbreaker worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.RANKBREAKER.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof RankbreakerItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
