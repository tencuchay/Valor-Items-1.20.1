package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The health_regen attribute: health regenerated every 5 seconds, from any number of sources. A fifth of the value is
 * healed every second.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class HealthRegenEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0 || !player.isAlive() || player.isSpectator()
                || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        double regen = player.getAttributeValue(ModAttributes.HEALTH_REGEN.get());
        if (regen > 0.0D) {
            player.heal((float) (regen / 5.0D));
        }
    }

    private HealthRegenEvents() {}
}
