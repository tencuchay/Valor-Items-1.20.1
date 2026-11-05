package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.api.ValorCritEvent;
import net.minecraftforge.common.MinecraftForge;
import com.valor.items.registry.ModAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The combat rules of the mod:
 * <ul>
 *   <li>the critical hit of vanilla (jumping and falling) is gone;</li>
 *   <li>every basic attack (melee, arrows of bows and crossbows, thrown tridents) can be a critical hit:
 *       the chance is the crit_rate attribute (default 0) and the damage is multiplied by crit_damage
 *       (default 2.0 = 200%);</li>
 *   <li>arrows and tridents deal extra damage: the attack damage that the player has from his equipment (without
 *       the weapon in his main hand) plus the ranged_attack attribute, measured when the projectile is shot.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class CombatEvents {
    private static final String RANGED_BONUS_TAG = "ValorRangedBonus";

    /** Sum of the "addition" modifiers that the item in the main hand gives to an attribute. */
    static double mainHandAddition(LivingEntity entity, Attribute attribute) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        double sum = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += modifier.getAmount();
            }
        }
        return sum;
    }

    /** Vanilla critical hits are turned off. */
    @SubscribeEvent
    public static void onVanillaCrit(CriticalHitEvent event) {
        event.setResult(Event.Result.DENY);
    }

    /** The bonus damage of a projectile is fixed the moment it is shot. */
    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                - player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                - mainHandAddition(player, Attributes.ATTACK_DAMAGE);
        double bonus = Math.max(0.0D, attack) + player.getAttributeValue(ModAttributes.RANGED_ATTACK.get());
        arrow.getPersistentData().putDouble(RANGED_BONUS_TAG, bonus);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        Entity direct = source.getDirectEntity();
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) && direct == player;
        boolean ranged = direct instanceof AbstractArrow arrow && arrow.getOwner() == player;
        if (!melee && !ranged) {
            return;
        }
        float amount = event.getAmount();
        if (ranged) {
            amount += (float) ((AbstractArrow) direct).getPersistentData().getDouble(RANGED_BONUS_TAG);
        }
        double rate = player.getAttributeValue(ModAttributes.CRIT_RATE.get());
        boolean crit = rate > 0.0D && player.getRandom().nextDouble() < rate;
        WindsBlessingEvents.onBasicHit(player, crit);
        if (crit) {
            amount *= (float) player.getAttributeValue(ModAttributes.CRIT_DAMAGE.get());
            if (ranged) {
                ClavesSanctiEvents.onRangedCrit(player);
            }
            // lets other mods (e.g. damage indicators) know that this hit is a critical hit
            event.getEntity().getPersistentData().putLong(ValorCritEvent.LAST_CRIT_TAG,
                    event.getEntity().level().getGameTime());
            MinecraftForge.EVENT_BUS.post(new ValorCritEvent(player, event.getEntity(), amount, ranged));
            LivingEntity target = event.getEntity();
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6D,
                        target.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.25D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        }
        event.setAmount(amount);
    }

    private CombatEvents() {}
}
