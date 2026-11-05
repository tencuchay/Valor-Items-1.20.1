package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.AmuletOfLongevityItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Amulet of Longevity: every few seconds the wearer recovers a percent of his max health; the percent is higher when
 * nothing attacked him for a few seconds. An attack is a hit of another entity, even one that did no damage.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class LongevityEvents {
    private static final String LAST_ATTACKED_TAG = "ValorLongevityAttacked";

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() != null
                && event.getSource().getEntity() != player) {
            player.getPersistentData().putLong(LAST_ATTACKED_TAG, player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator()) {
            return;
        }
        int interval = Math.max(1, (int) Math.round(ValorConfig.longevityIntervalSeconds() * 20.0D));
        if (player.tickCount % interval != 0) {
            return;
        }
        int level = AmuletOfLongevityItem.getEquippedLevel(player);
        if (level <= 0 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long now = player.level().getGameTime();
        long safeTicks = Math.round(ValorConfig.longevitySafeSeconds() * 20.0D);
        boolean safe = now - player.getPersistentData().getLong(LAST_ATTACKED_TAG) >= safeTicks;
        double percent = safe ? ValorConfig.longevityRestPercent(level) : ValorConfig.longevityPercent(level);
        player.heal((float) (player.getMaxHealth() * percent / 100.0D));
    }

    private LongevityEvents() {}
}
