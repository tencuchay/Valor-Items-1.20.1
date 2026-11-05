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
 * Fenrir's Fang - relic for the "items" Curios slot.
 *
 * <p>Passive: +18 attack damage.
 * <p>Unique passive "Conquer" (5 levels): extra damage against targets below half of their health
 * (see ConquerEvents).
 */
public class FenrirsFangItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 18.0D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public FenrirsFangItem(Properties properties) {
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
                Component.translatable("ability.valor_items.fenrirs_fang.conquer.name"),
                level -> Component.translatable("ability.valor_items.fenrirs_fang.conquer.desc",
                        percent(ValorConfig.conquerPercent(level)),
                        percent(ValorConfig.conquerThresholdPercent())),
                true,
                level -> List.of(Component.translatable("ability.valor_items.fenrirs_fang.conquer.change",
                        percent(ValorConfig.conquerPercent(level)),
                        percent(ValorConfig.conquerPercent(level + 1))))));
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
                "valor_items:fenrirs_fang_attack_damage", ATTACK_DAMAGE_BONUS,
                AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Highest level of the Fenrir's Fang worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.FENRIRS_FANG.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof FenrirsFangItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
