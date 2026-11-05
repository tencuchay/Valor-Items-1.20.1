package com.valor.items.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.valor.items.config.ValorConfig;
import com.valor.items.registry.ModItems;
import com.valor.items.relic.RelicAbility;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Mail of Pain - relic for the "items" Curios slot.
 *
 * <p>Passive: +4.5 attack damage, +3 armor, +7 max health.
 * <p>Unique passive "Riposte": reflects a part of the damage taken as true damage.
 * <p>Active "Countersiege": raises the reflected part for a few seconds (see MailOfPainEvents).
 */
public class MailOfPainItem extends ValorItem {
    public static final String SLOT_ID = "items";

    public static final double ATTACK_DAMAGE_BONUS = 4.5D;
    public static final double ARMOR_BONUS = 3.0D;
    public static final double MAX_HEALTH_BONUS = 7.0D;

    public MailOfPainItem(Properties properties) {
        super(properties);
    }

    private static final int MAX_LEVEL = 5;
    /** Experience levels to upgrade from level 1->2, 2->3, 3->4, 4->5. */
    private static final int[] UPGRADE_COST = {10, 20, 30, 40};

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getUpgradeCost(int level) {
        return UPGRADE_COST[net.minecraft.util.Mth.clamp(level - 1, 0, UPGRADE_COST.length - 1)];
    }

    private static String percent(double value) {
        return DeathSickleItem.formatNumber(value) + "%";
    }

    @Override
    public String getSlotId() {
        return SLOT_ID;
    }

    @Override
    public List<RelicAbility> getAbilities() {
        return List.of(
                new RelicAbility(
                        Component.translatable("ability.valor_items.mail_of_pain.riposte.name"),
                        level -> Component.translatable("ability.valor_items.mail_of_pain.riposte.desc",
                                percent(ValorConfig.mailRipostePercent(level))),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.mail_of_pain.riposte.change",
                                percent(ValorConfig.mailRipostePercent(level)),
                                percent(ValorConfig.mailRipostePercent(level + 1))))),
                new RelicAbility(
                        Component.translatable("ability.valor_items.mail_of_pain.countersiege.name"),
                        level -> Component.translatable("ability.valor_items.mail_of_pain.countersiege.desc",
                                percent(ValorConfig.mailCountersiegePercent(level)),
                                DeathSickleItem.formatNumber(ValorConfig.mailCountersiegeSeconds()),
                                ValorConfig.mailCooldownSeconds(),
                                ValorItem.activeKeys()),
                        true,
                        level -> List.of(Component.translatable("ability.valor_items.mail_of_pain.countersiege.change",
                                percent(ValorConfig.mailCountersiegePercent(level)),
                                percent(ValorConfig.mailCountersiegePercent(level + 1))))));
    }

    @Override
    public List<Component> getStatLines() {
        return List.of(
                Component.translatable("stat.valor_items.attack_damage",
                        "+" + DeathSickleItem.formatNumber(ATTACK_DAMAGE_BONUS)),
                Component.translatable("stat.valor_items.armor", "+" + DeathSickleItem.formatNumber(ARMOR_BONUS)),
                Component.translatable("stat.valor_items.max_health",
                        "+" + DeathSickleItem.formatNumber(MAX_HEALTH_BONUS)));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                                        ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid,
                "valor_items:mail_of_pain_attack_damage", ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ARMOR, new AttributeModifier(uuid,
                "valor_items:mail_of_pain_armor", ARMOR_BONUS, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid,
                "valor_items:mail_of_pain_max_health", MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    /** Is a Mail of Pain worn by the entity? */
    public static boolean isEquipped(LivingEntity entity) {
        return getEquippedLevel(entity) > 0;
    }

    /** Highest level of the Mail of Pain worn by the entity, 0 if none. */
    public static int getEquippedLevel(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> {
                    int best = 0;
                    for (SlotResult result : handler.findCurios(ModItems.MAIL_OF_PAIN.get())) {
                        ItemStack stack = result.stack();
                        if (stack.getItem() instanceof MailOfPainItem mail) {
                            best = Math.max(best, mail.getLevel(stack));
                        }
                    }
                    return best;
                })
                .orElse(0);
    }
}
