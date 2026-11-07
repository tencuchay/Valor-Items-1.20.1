package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.RankbreakerItem;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Armor penetration, lethality, and the Rankbreaker.
 * <ul>
 *   <li>Lethality and armor penetration: the effective armor of the target becomes (A * (1 - %pen)) - L (never below 0)
 *       against the physical damage of the attacker. Minecraft applies the armor after LivingHurtEvent, so the damage
 *       is raised just enough that, after the normal armor of the target, it equals the damage the effective armor
 *       would have let through.</li>
 *   <li>"Break": the lethality attribute, one modifier with a fixed id (copies do not stack).</li>
 *   <li>"Wildwalk": movement speed when no enemy has hurt the wearer for some seconds (one fixed modifier).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class RankbreakerEvents {
    private static final String LAST_HIT_TAG = "ValorWildwalkLastHit";
    private static final UUID BREAK_UUID = UUID.fromString("5e1b7c93-2a48-4d6f-b0e3-8c9a4f2d1e67");
    private static final UUID WILDWALK_UUID = UUID.fromString("c8a4d2e6-9f13-4b57-a6d0-2e7b1c5f3a89");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        int level = RankbreakerItem.getEquippedLevel(player);
        updateBreak(player, level);
        updateWildwalk(player, level > 0);
    }

    private static void updateBreak(ServerPlayer player, int level) {
        AttributeInstance pen = player.getAttribute(ModAttributes.LETHALITY.get());
        if (pen == null) {
            return;
        }
        if (level <= 0) {
            pen.removeModifier(BREAK_UUID);
            return;
        }
        double amount = ValorConfig.breakArmorPenetration(level);
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

    /** Armor penetration and lethality, last of all so that every other change to the damage is already in. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        var source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_ARMOR) || MagicArmorEvents.isMagic(source)) {
            return;
        }
        AttributeInstance lethalityAttr = attacker.getAttribute(ModAttributes.LETHALITY.get());
        double lethality = lethalityAttr == null ? 0.0D : lethalityAttr.getValue();
        AttributeInstance percentAttr = attacker.getAttribute(ModAttributes.ARMOR_PENETRATION_PERCENT.get());
        double percent = percentAttr == null ? 0.0D : percentAttr.getValue();
        LivingEntity target = event.getEntity();
        float armor = target.getArmorValue();
        if ((lethality <= 0.0D && percent <= 0.0D) || armor <= 0.0F || event.getAmount() <= 0.0F) {
            return;
        }
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float amount = event.getAmount();
        double effectiveArmor = armor * Math.max(0.0D, 1.0D - percent) - lethality;
        float wanted = CombatRules.getDamageAfterAbsorb(amount, (float) Math.max(0.0D, effectiveArmor), toughness);
        // find the damage that the real armor turns into "wanted" (the armor formula grows with the damage)
        float low = amount;
        float high = Math.max(amount, wanted * 5.0F + 1.0F);
        for (int i = 0; i < 30; i++) {
            float mid = (low + high) / 2.0F;
            if (CombatRules.getDamageAfterAbsorb(mid, armor, toughness) < wanted) {
                low = mid;
            } else {
                high = mid;
            }
        }
        event.setAmount(high);
    }

    private RankbreakerEvents() {}
}
