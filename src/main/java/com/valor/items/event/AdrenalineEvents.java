package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.OdinsWillItem;
import com.valor.items.network.AdrenalineStatePacket;
import com.valor.items.network.ModNetwork;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Odin's Will: "Adrenaline". Taking damage gives a stack (one timer: when it ends one stack is lost; every hit taken adds a stack and restarts it); every stack
 * raises the movement speed and the damage dealt by the wearer.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class AdrenalineEvents {
    private static final String STACKS_TAG = "ValorAdrenalineStacks";
    private static final String END_TAG = "ValorAdrenalineEnd";
    private static final String TOTAL_TAG = "ValorAdrenalineTotal";
    private static final UUID SPEED_UUID = UUID.fromString("8b7f4f0e-6f2c-4a3e-9a51-3c2f1d0a7b11");

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        int level = OdinsWillItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        int stacks = Math.min(ValorConfig.adrenalineMaxStacks(), data.getInt(STACKS_TAG) + 1);
        int duration = Math.max(1, (int) Math.round(ValorConfig.adrenalineSeconds() * 20.0D));
        data.putInt(STACKS_TAG, stacks);
        data.putLong(END_TAG, now + duration);
        data.putInt(TOTAL_TAG, duration);
        applySpeed(player, stacks, level);
        sync(player);
    }

    /** The damage dealt by the wearer is raised by every stack. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        int stacks = data.getInt(STACKS_TAG);
        if (stacks <= 0) {
            return;
        }
        int level = OdinsWillItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        double bonus = stacks * ValorConfig.adrenalineDamagePercent(level) / 100.0D;
        event.setAmount((float) (event.getAmount() * (1.0D + bonus)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        if (data.getInt(STACKS_TAG) <= 0) {
            return;
        }
        int level = OdinsWillItem.getEquippedLevel(player);
        if (level <= 0) {
            clear(player);
            return;
        }
        long now = player.level().getGameTime();
        if (data.getLong(END_TAG) <= now) {
            // the time is over: one stack is lost and the timer starts again for the next one
            int stacks = data.getInt(STACKS_TAG) - 1;
            if (stacks <= 0) {
                clear(player);
            } else {
                int duration = Math.max(1, (int) Math.round(ValorConfig.adrenalineSeconds() * 20.0D));
                data.putInt(STACKS_TAG, stacks);
                data.putLong(END_TAG, now + duration);
                data.putInt(TOTAL_TAG, duration);
                applySpeed(player, stacks, level);
                sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player); // the speed modifier is not saved, so the stacks do not survive a relog
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void clear(ServerPlayer player) {
        var data = player.getPersistentData();
        boolean had = data.getInt(STACKS_TAG) > 0;
        data.putInt(STACKS_TAG, 0);
        data.putLong(END_TAG, 0L);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_UUID);
        }
        if (had) {
            sync(player);
        }
    }

    private static void applySpeed(ServerPlayer player, int stacks, int level) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_UUID);
        speed.addTransientModifier(new AttributeModifier(SPEED_UUID, "valor_items:adrenaline_speed",
                stacks * ValorConfig.adrenalineSpeedPercent(level) / 100.0D,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        int left = (int) Math.max(0L, data.getLong(END_TAG) - now);
        int stacks = left > 0 ? data.getInt(STACKS_TAG) : 0;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AdrenalineStatePacket(stacks, left, Math.max(1, data.getInt(TOTAL_TAG))));
        // everybody who sees the player draws him with a red tint while he has stacks
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new com.valor.items.network.AdrenalineAuraPacket(player.getId(), stacks > 0 ? left : 0));
    }

    private AdrenalineEvents() {}
}
