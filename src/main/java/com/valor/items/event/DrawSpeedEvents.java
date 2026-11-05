package com.valor.items.event;

import com.valor.items.ValorItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attack speed also speeds up bows (drawing), crossbows (loading) and tridents (charging): the time is divided by
 * attack speed / 4 (4 is the attack speed of a player without equipment). It runs on both the server and the client
 * with the same numbers, so they agree on when the weapon is ready.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class DrawSpeedEvents {
    private static final String ACC_TAG = "ValorDrawAcc";
    private static final double BASE_ATTACK_SPEED = 4.0D;
    private static final double MIN_FACTOR = 0.25D;

    private static boolean isDrawn(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
    }

    @SubscribeEvent
    public static void onStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player && isDrawn(event.getItem().getItem())) {
            event.getEntity().getPersistentData().putDouble(ACC_TAG, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEntityUseItemEvent.Tick event) {
        // mobs (skeletons with bows) have no attack speed attribute: only players are affected
        if (!(event.getEntity() instanceof Player entity) || !isDrawn(event.getItem().getItem())) {
            return;
        }
        // the attack speed of the weapon in the main hand (a sword, a trident) does not count
        double speed = entity.getAttributeValue(Attributes.ATTACK_SPEED)
                - CombatEvents.mainHandAddition(entity, Attributes.ATTACK_SPEED);
        double factor = Math.max(MIN_FACTOR, speed / BASE_ATTACK_SPEED);
        var data = entity.getPersistentData();
        double acc = data.getDouble(ACC_TAG) + (factor - 1.0D);
        int duration = event.getDuration();
        while (acc >= 1.0D && duration > 2) {
            duration--;
            acc -= 1.0D;
        }
        while (acc <= -1.0D) {
            duration++;
            acc += 1.0D;
        }
        data.putDouble(ACC_TAG, acc);
        event.setDuration(duration);
    }

    private DrawSpeedEvents() {}
}
