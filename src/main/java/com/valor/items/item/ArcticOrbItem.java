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
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Arctic Orb - relic for the "items" Curios slot.
 *
 * <p>Passive: +22 magic damage, +12% magic lifesteal.
 * <p>Unique active "Impervious": immune and unable to move for a few seconds, then a shield (see ArcticOrbEvents).
 */
public class ArcticOrbItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double MAGIC_DAMAGE_BONUS = 22.0D;
    /** Added to the magic lifesteal attribute: 0.12 = 12%. */
    public static final double MAGIC_LIFESTEAL_BONUS = 0.12D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public ArcticOrbItem(Properties properties) {
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
        return List.of(new RelicAbility(
                Component.translatable("ability.valor_items.arctic_orb.impervious.name"),
                level -> Component.translatable("ability.valor_items.arctic_orb.impervious.desc",
                        DeathSickleItem.formatNumber(ValorConfig.arcticFreezeSeconds(level)),
                        DeathSickleItem.formatNumber(ValorConfig.arcticShieldBase()),
                        DeathSickleItem.formatNumber(ValorConfig.arcticShieldMagicPercent()) + "%",
                        DeathSickleItem.formatNumber(ValorConfig.arcticShieldSeconds()),
                        ValorConfig.arcticCooldownSeconds(),
                        ValorItem.activeKeys()),
                true,
                level -> List.of(Component.translatable("ability.valor_items.arctic_orb.impervious.change",
                        DeathSickleItem.formatNumber(ValorConfig.arcticFreezeSeconds(level)),
                        DeathSickleItem.formatNumber(ValorConfig.arcticFreezeSeconds(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.magic_damage",
                        "+" + DeathSickleItem.formatNumber(MAGIC_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.magic_lifesteal",
                        "+" + DeathSickleItem.formatNumber(MAGIC_LIFESTEAL_BONUS * 100.0D) + "%"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(ModAttributes.MAGIC_DAMAGE.get(), new AttributeModifier(uuid,
                "valor_items:arctic_orb_magic_damage", MAGIC_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(ModAttributes.MAGIC_LIFESTEAL.get(), new AttributeModifier(uuid,
                "valor_items:arctic_orb_magic_lifesteal", MAGIC_LIFESTEAL_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** The magic lifesteal is shown as a percent (+12%) instead of the raw value (+0.12). */
    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        String name = Component.translatable("attribute.valor_items.magic_lifesteal").getString();
        List<Component> result = new ArrayList<>();
        for (Component line : tooltips) {
            if (!line.getString().contains(name)) {
                result.add(line);
            }
        }
        result.add(Component.translatable("stat.valor_items.magic_lifesteal",
                "+" + DeathSickleItem.formatNumber(MAGIC_LIFESTEAL_BONUS * 100.0D) + "%").withStyle(ChatFormatting.BLUE));
        return result;
    }

    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Arctic Orb worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.ARCTIC_ORB.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof ArcticOrbItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
