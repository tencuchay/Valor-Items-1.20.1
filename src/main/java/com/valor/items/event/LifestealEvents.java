package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lifesteal and magic lifesteal.
 * <ul>
 *   <li>valor_items:lifesteal (0.10 = 10%): the physical damage the player deals heals him by that part of the damage
 *       that was really dealt. Physical damage: normal attacks (melee, arrows, tridents) and the physical extra
 *       damage of the relics (Berserk, Dragon's Breath).</li>
 *   <li>valor_items:magic_lifesteal: the same for the magic damage (the vanilla magic damage types).</li>
 * </ul>
 * The damage counted is what the target takes: after its armor, and never more than the health it had.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class LifestealEvents {

    /** A normal attack of the player: a melee hit, or an arrow / trident he shot. */
    static boolean isBasicAttack(DamageSource source, ServerPlayer player) {
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypes.PLAYER_ATTACK) && direct == player) {
            return true;
        }
        return direct instanceof AbstractArrow arrow && arrow.getOwner() == player;
    }

    private static boolean isPhysical(DamageSource source, ServerPlayer player) {
        return isBasicAttack(source, player) || source.is(HerculesEvents.BERSERK)
                || source.is(DragonsBreathEvents.DRAGONS_BREATH);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        double steal;
        if (isPhysical(event.getSource(), player)) {
            steal = player.getAttributeValue(ModAttributes.LIFESTEAL.get());
        } else if (MagicArmorEvents.isMagic(event.getSource())) {
            steal = player.getAttributeValue(ModAttributes.MAGIC_LIFESTEAL.get());
        } else {
            return;
        }
        if (steal <= 0.0D) {
            return;
        }
        LivingEntity target = event.getEntity();
        float dealt = Math.min(event.getAmount(), target.getHealth());
        if (dealt > 0.0F && player.isAlive()) {
            player.heal((float) (dealt * steal));
        }
    }

    private LifestealEvents() {}
}
