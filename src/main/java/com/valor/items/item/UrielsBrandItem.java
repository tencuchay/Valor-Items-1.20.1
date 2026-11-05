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
 * Uriel's Brand - relic for the "items" Curios slot.
 *
 * <p>Passive: +8 attack damage, +0.25 attack speed, +1.5 magic armor.
 * <p>Unique passive "Devil's Bane": part of the attack damage becomes magic armor (see UrielEvents).
 * <p>Unique passive "Magic Guardian" (5 levels, the shield grows): below a part of the health, a magic shield and
 * movement speed (see UrielEvents).
 */
public class UrielsBrandItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 8.0D;
    public static final double ATTACK_SPEED_BONUS = 0.25D;
    public static final double MAGIC_ARMOR_BONUS = 1.5D;

    private static final int MAX_LEVEL = 5;
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    public UrielsBrandItem(Properties properties) {
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
        return List.of(
                new RelicAbility(
                        Component.translatable("ability.valor_items.uriels_brand.bane.name"),
                        level -> Component.translatable("ability.valor_items.uriels_brand.bane.desc",
                                percent(ValorConfig.baneConvertPercent()),
                                DeathSickleItem.formatNumber(ValorConfig.baneMaxMagicArmor())),
                        false, level -> List.of()),
                new RelicAbility(
                        Component.translatable("ability.valor_items.uriels_brand.guardian.name"),
                        level -> Component.translatable("ability.valor_items.uriels_brand.guardian.desc",
                                percent(ValorConfig.guardianThresholdPercent()),
                                percent(ValorConfig.guardianShieldPercent(level)),
                                percent(ValorConfig.guardianSpeedPercent()),
                                DeathSickleItem.formatNumber(ValorConfig.guardianSeconds()),
                                ValorConfig.guardianCooldownSeconds()),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.uriels_brand.guardian.change",
                                percent(ValorConfig.guardianShieldPercent(level)),
                                percent(ValorConfig.guardianShieldPercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.attack_speed",
                        "+" + DeathSickleItem.formatNumber(ATTACK_SPEED_BONUS)),
                Component.translatable("stat.valor_items.magic_armor",
                        "+" + DeathSickleItem.formatNumber(MAGIC_ARMOR_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:uriels_brand_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid,
                "valor_items:uriels_brand_attack_speed", ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(ModAttributes.MAGIC_ARMOR.get(), new AttributeModifier(uuid,
                "valor_items:uriels_brand_magic_armor", MAGIC_ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Uriel's Brand worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.URIELS_BRAND.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof UrielsBrandItem item) {
                            best = Math.max(best, item.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
