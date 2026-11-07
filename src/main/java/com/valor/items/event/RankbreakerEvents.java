package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.RankbreakerItem;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Armor mechanics, armor penetration, lethality, and the Rankbreaker.
 * <ul>
 *   <li>Hyperbolic armor formula: damage = rawDamage * (K / (K + effectiveArmor)), with K = 6.0.</li>
 *   <li>Protection enchantment (Protection): converted to +2.5% armor per level (Protection IV = +10% per piece).</li>
 *   <li>Penetration order: effectiveArmor = (baseArmor * (1 + protBonus)) * (1 - %pen) - lethality (min 0).</li>
 *   <li>"Break": lethality attribute from equipped Rankbreaker(s), stacks across equipped copies.</li>
 *   <li>"Wildwalk": movement speed when no enemy has hurt the wearer for some seconds (one fixed modifier).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class RankbreakerEvents {
    private static final String LAST_HIT_TAG = "ValorWildwalkLastHit";
    private static final String RAW_DAMAGE_TAG = "ValorRawDamage";
    private static final UUID BREAK_UUID = UUID.fromString("5e1b7c93-2a48-4d6f-b0e3-8c9a4f2d1e67");
    private static final UUID WILDWALK_UUID = UUID.fromString("c8a4d2e6-9f13-4b57-a6d0-2e7b1c5f3a89");
    private static final double FORMULA_K = 6.0D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        int level = RankbreakerItem.getEquippedLevel(player);
        updateBreak(player);
        updateWildwalk(player, level > 0);
    }

    private static void updateBreak(ServerPlayer player) {
        AttributeInstance pen = player.getAttribute(ModAttributes.LETHALITY.get());
        if (pen == null) {
            return;
        }
        double amount = RankbreakerItem.getTotalLethality(player);
        if (amount <= 0.0D) {
            pen.removeModifier(BREAK_UUID);
            return;
        }
        AttributeModifier current = pen.getModifier(BREAK_UUID);
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-6D) {
            return;
        }
        pen.removeModifier(BREAK_UUID);
        pen.addTransientModifier(new AttributeModifier(BREAK_UUID, "valor_items:break", amount,
                AttributeModifier.Operation.ADDITION));
    }

    private static void updateWildwalk(ServerPlayer player, boolean equipped) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        long now = player.level().getGameTime();
        boolean active = equipped && now - player.getPersistentData().getLong(LAST_HIT_TAG)
                >= ValorConfig.wildwalkSeconds() * 20L;
        AttributeModifier current = speed.getModifier(WILDWALK_UUID);
        if (!active) {
            if (current != null) {
                speed.removeModifier(WILDWALK_UUID);
            }
            return;
        }
        double amount = ValorConfig.wildwalkSpeedPercent() / 100.0D;
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-9D) {
            return;
        }
        speed.removeModifier(WILDWALK_UUID);
        speed.addTransientModifier(new AttributeModifier(WILDWALK_UUID, "valor_items:wildwalk", amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** Damage from an enemy (any living entity other than the player himself) restarts the Wildwalk timer. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
            player.getPersistentData().putLong(LAST_HIT_TAG, player.level().getGameTime());
        }
    }

    /** Record the incoming pre-armor physical damage amount. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        var source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_ARMOR) || MagicArmorEvents.isMagic(source)) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }
        event.getEntity().getPersistentData().putFloat(RAW_DAMAGE_TAG, event.getAmount());
    }

    /**
     * Custom hyperbolic armor & protection formula:
     * Overrides vanilla armor and enchantment damage reductions with the Direction 1 formula.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getPersistentData().contains(RAW_DAMAGE_TAG)) {
            return;
        }
        float rawDamage = target.getPersistentData().getFloat(RAW_DAMAGE_TAG);
        target.getPersistentData().remove(RAW_DAMAGE_TAG);

        if (rawDamage <= 0.0F) {
            return;
        }

        var source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_ARMOR) || MagicArmorEvents.isMagic(source)) {
            return;
        }

        // 1. Base armor
        float baseArmor = target.getArmorValue();

        // 2. Protection enchantment bonus: +2.5% armor per level (Protection IV = +10% per piece)
        int totalProt = 0;
        for (ItemStack stack : target.getArmorSlots()) {
            if (!stack.isEmpty()) {
                totalProt += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
            }
        }
        double totalArmor = baseArmor * (1.0D + totalProt * 0.025D);

        // 3. Attacker's % Armor Penetration & Lethality
        double percentPen = 0.0D;
        double lethality = 0.0D;
        if (source.getEntity() instanceof LivingEntity attacker) {
            AttributeInstance percentAttr = attacker.getAttribute(ModAttributes.ARMOR_PENETRATION_PERCENT.get());
            if (percentAttr != null) {
                percentPen = percentAttr.getValue();
            }
            AttributeInstance lethalityAttr = attacker.getAttribute(ModAttributes.LETHALITY.get());
            if (lethalityAttr != null) {
                lethality = lethalityAttr.getValue();
            }
        }

        // Effective armor after percentage penetration and flat lethality
        double effectiveArmor = totalArmor * Math.max(0.0D, 1.0D - percentPen) - lethality;
        effectiveArmor = Math.max(0.0D, effectiveArmor);

        // 4. Direction 1: Hyperbolic armor formula K / (K + effectiveArmor)
        float finalDamage = (float) (rawDamage * (FORMULA_K / (FORMULA_K + effectiveArmor)));

        // 5. Keep Resistance potion effect if active on the target
        if (target.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            int resLevel = target.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
            float resMultiplier = Math.max(0.0F, 1.0F - resLevel * 0.2F);
            finalDamage *= resMultiplier;
        }

        event.setAmount(finalDamage);
    }

    private RankbreakerEvents() {}
}
