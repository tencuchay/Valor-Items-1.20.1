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
 * Berith's Agony (Mặt Nạ Berith) - relic for the "items" Curios slot.
 *
 * <p>Passive: +14 magic damage, +6 max health.
 * <p>Unique passive "Torture" (Thống Khổ): a normal attack that hits makes the enemy take magic damage equal to a
 * percent of its current health every second for 3 seconds (see TortureEvents).
 */
public class BerithsAgonyItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double MAGIC_DAMAGE_BONUS = 14.0D;
    public static final double MAX_HEALTH_BONUS = 6.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public BerithsAgonyItem(Properties properties) {
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
                Component.translatable("ability.valor_items.beriths_agony.torture.name"),
                level -> Component.translatable("ability.valor_items.beriths_agony.torture.desc",
                        percent(ValorConfig.torturePercent(level)),
                        DeathSickleItem.formatNumber(ValorConfig.tortureSeconds())),
                true,
                level -> List.of(Component.translatable("ability.valor_items.beriths_agony.torture.change",
                        percent(ValorConfig.torturePercent(level)),
                        percent(ValorConfig.torturePercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.magic_damage",
                        "+" + DeathSickleItem.formatNumber(MAGIC_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(ModAttributes.MAGIC_DAMAGE.get(), new AttributeModifier(uuid,
                "valor_items:beriths_agony_magic_damage", MAGIC_DAMAGE_BONUS,
                AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:beriths_agony_max_health", MAX_HEALTH_BONUS,
                AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Berith's Agony worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.BERITHS_AGONY.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof BerithsAgonyItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
