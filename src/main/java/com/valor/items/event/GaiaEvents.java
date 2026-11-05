package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.GaiasStandardItem;
import com.valor.items.network.GaiaStatePacket;
import com.valor.items.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gaia's Standard: "Restore" heals a percent of the max health over a few seconds after taking damage (with a
 * cooldown); "Bloodline" makes all the healing of the wearer stronger for every 10% of health lost.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class GaiaEvents {
    private static final String RESTORE_END_TAG = "ValorGaiaRestoreEnd";
    private static final String RESTORE_PERCENT_TAG = "ValorGaiaRestorePercent";
    private static final String RESTORE_PULSES_TAG = "ValorGaiaRestorePulses";
    private static final int PULSES = 4;
    private static final String RESTORE_TOTAL_TAG = "ValorGaiaRestoreTotal";
    private static final String COOLDOWN_END_TAG = "ValorGaiaCooldownEnd";
    private static final String COOLDOWN_TOTAL_TAG = "ValorGaiaCooldownTotal";

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        int level = GaiasStandardItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (data.getLong(COOLDOWN_END_TAG) > now) {
            return;
        }
        int duration = Math.max(1, (int) Math.round(ValorConfig.gaiaRestoreSeconds() * 20.0D));
        int cooldown = Math.max(duration, (int) Math.round(ValorConfig.gaiaCooldownSeconds() * 20.0D));
        data.putLong(RESTORE_END_TAG, now + duration);
        data.putInt(RESTORE_TOTAL_TAG, duration);
        data.putInt(RESTORE_PULSES_TAG, 0);
        data.putDouble(RESTORE_PERCENT_TAG, ValorConfig.gaiaRestorePercent(level));
        data.putLong(COOLDOWN_END_TAG, now + cooldown);
        data.putInt(COOLDOWN_TOTAL_TAG, cooldown);
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        int left = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - player.level().getGameTime());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new GaiaStatePacket(left, Math.max(1, data.getInt(COOLDOWN_TOTAL_TAG))));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** The cooldown survives death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        var now = event.getEntity().getPersistentData();
        now.putLong(COOLDOWN_END_TAG, old.getLong(COOLDOWN_END_TAG));
        now.putInt(COOLDOWN_TOTAL_TAG, old.getInt(COOLDOWN_TOTAL_TAG));
    }

    /** The recovery comes in 4 equal pulses spread over its duration (the last one when it ends). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        var data = player.getPersistentData();
        long end = data.getLong(RESTORE_END_TAG);
        if (end == 0L) {
            return;
        }
        if (GaiasStandardItem.getEquippedLevel(player) <= 0) {
            data.putLong(RESTORE_END_TAG, 0L);
            return;
        }
        long now = player.level().getGameTime();
        int total = Math.max(1, data.getInt(RESTORE_TOTAL_TAG));
        long elapsed = Math.max(0L, now - (end - total));
        int due = (int) Math.min(PULSES, elapsed * PULSES / total);
        int done = data.getInt(RESTORE_PULSES_TAG);
        if (due > done) {
            double amount = player.getMaxHealth() * data.getDouble(RESTORE_PERCENT_TAG) / 100.0D / PULSES * (due - done);
            player.heal((float) amount);
            data.putInt(RESTORE_PULSES_TAG, due);
        }
        if (now >= end) {
            data.putLong(RESTORE_END_TAG, 0L);
        }
    }

    /** Bloodline: +X% healing for every 10% of the max health lost (only while the item is worn). */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || GaiasStandardItem.getEquippedLevel(player) <= 0) {
            return;
        }
        double lost = 1.0D - player.getHealth() / player.getMaxHealth();
        double step = Math.max(1.0D, ValorConfig.gaiaBloodlineStepPercent()) / 100.0D;
        int steps = (int) Math.floor(Math.max(0.0D, lost) / step + 1.0E-9D);
        if (steps <= 0) {
            return;
        }
        double multiplier = 1.0D + steps * ValorConfig.gaiaBloodlineBonusPercent() / 100.0D;
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    private GaiaEvents() {}
}
