package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.DevilsHandshakeItem;
import com.valor.items.network.FlameStatePacket;
import com.valor.items.network.ModNetwork;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Devil's Handshake: "Flame Wraith". A normal attack that hits gives a stack (up to 5); every stack raises the attack
 * speed, the movement speed and the damage dealt. One timer: when it ends a stack is lost and it starts again; every
 * hit adds a stack and restarts it.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class FlameWraithEvents {
    private static final String STACKS_TAG = "ValorFlameStacks";
    private static final String END_TAG = "ValorFlameEnd";
    private static final String TOTAL_TAG = "ValorFlameTotal";
    private static final UUID SPEED_UUID = UUID.fromString("5d0c1c1e-3a52-4c5b-8e7b-0f9a4b6c2d31");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("c4a7e0b2-91d3-4f68-b2a5-7e3d1a9c5f42");

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        int level = DevilsHandshakeItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        var data = player.getPersistentData();
        int stacks = data.getInt(STACKS_TAG);
        // the damage dealt grows with the stacks he already has
        if (stacks > 0) {
            event.setAmount((float) (event.getAmount() * (1.0D + stacks * ValorConfig.flamePercent(level) / 100.0D)));
        }
        // only a normal attack (a melee hit of the player himself or a projectile he shot) gives a stack
        boolean melee = event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getDirectEntity() == player;
        boolean ranged = event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() == player;
        if ((!melee && !ranged) || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        long now = player.level().getGameTime();
        stacks = Math.min(ValorConfig.flameMaxStacks(), stacks + 1);
        int duration = Math.max(1, (int) Math.round(ValorConfig.flameSeconds() * 20.0D));
        data.putInt(STACKS_TAG, stacks);
        data.putLong(END_TAG, now + duration);
        data.putInt(TOTAL_TAG, duration);
        applyModifiers(player, stacks, level);
        sync(player);
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
        int level = DevilsHandshakeItem.getEquippedLevel(player);
        if (level <= 0) {
            clear(player);
            return;
        }
        long now = player.level().getGameTime();
        if (data.getLong(END_TAG) <= now) {
            int stacks = data.getInt(STACKS_TAG) - 1;
            if (stacks <= 0) {
                clear(player);
            } else {
                int duration = Math.max(1, (int) Math.round(ValorConfig.flameSeconds() * 20.0D));
                data.putInt(STACKS_TAG, stacks);
                data.putLong(END_TAG, now + duration);
                data.putInt(TOTAL_TAG, duration);
                applyModifiers(player, stacks, level);
                sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player); // the modifiers are not saved, so the stacks do not survive a relog
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
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
        }
        if (had) {
            sync(player);
        }
    }

    private static void applyModifiers(ServerPlayer player, int stacks, int level) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_UUID);
            speed.addTransientModifier(new AttributeModifier(SPEED_UUID, "valor_items:flame_wraith_speed",
                    stacks * ValorConfig.flamePercent(level) / 100.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
            attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_UUID,
                    "valor_items:flame_wraith_attack_speed", stacks * ValorConfig.flameAttackSpeed(level),
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        int left = (int) Math.max(0L, data.getLong(END_TAG) - now);
        int stacks = left > 0 ? data.getInt(STACKS_TAG) : 0;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FlameStatePacket(stacks, left, Math.max(1, data.getInt(TOTAL_TAG))));
    }

    private FlameWraithEvents() {}
}
