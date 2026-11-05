package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.StikksStingItem;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.WindStatePacket;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Stikk's Sting: "Wind's Blessing". A normal attack that hits (melee or ranged) gives attack speed (added to the attack
 * speed attribute: 10% = +0.10) and movement speed for a few seconds; a critical hit doubles both. A new hit restarts the time and never lowers a doubled effect
 * that is still running. It does not stack, however many Stikk's Sting are worn.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class WindsBlessingEvents {
    private static final String END_TAG = "ValorWindEnd";
    private static final String MULT_TAG = "ValorWindMult";
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("3e6b8c14-52d7-4f0a-a1b9-6c4d2e7f9a58");
    private static final UUID MOVE_SPEED_UUID = UUID.fromString("9a2f7d41-0b3c-4e85-8d16-5f1c3b7e2a90");

    /** Called by CombatEvents for every basic attack of the player that hits. */
    static void onBasicHit(ServerPlayer player, boolean crit) {
        int level = StikksStingItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        int mult = crit ? 2 : 1;
        if (data.getLong(END_TAG) > now) {
            mult = Math.max(mult, data.getInt(MULT_TAG));
        }
        int duration = Math.max(1, (int) Math.round(ValorConfig.windSeconds() * 20.0D));
        data.putLong(END_TAG, now + duration);
        data.putInt(MULT_TAG, mult);

        sync(player, mult, duration, duration);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
            attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_UUID, "valor_items:winds_blessing_attack_speed",
                    mult * ValorConfig.windAttackSpeedPercent(level) / 100.0D, AttributeModifier.Operation.ADDITION));
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOVE_SPEED_UUID);
            speed.addTransientModifier(new AttributeModifier(MOVE_SPEED_UUID, "valor_items:winds_blessing_movement_speed",
                    mult * ValorConfig.windMoveSpeedPercent(level) / 100.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        long end = data.getLong(END_TAG);
        if (end == 0L) {
            return;
        }
        if (end <= player.level().getGameTime() || StikksStingItem.getEquippedLevel(player) <= 0) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void sync(ServerPlayer player, int strength, int ticksLeft, int ticksTotal) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new WindStatePacket(strength, ticksLeft, ticksTotal));
    }

    private static void clear(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.getLong(END_TAG) != 0L) {
            sync(player, 0, 0, 1);
        }
        data.putLong(END_TAG, 0L);
        data.putInt(MULT_TAG, 0);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOVE_SPEED_UUID);
        }
    }

    private WindsBlessingEvents() {}
}
