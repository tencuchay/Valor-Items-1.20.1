package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.DeathSickleItem;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.SickleGhostPacket;
import com.valor.items.network.SickleOrbitPacket;
import com.valor.items.network.SickleStatePacket;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Death Sickle unique passive "Insatiable": a hit that would kill the wearer leaves them at 1 HP and makes them
 * immortal for a few seconds (immune to all damage and harmful effects, faster); then a long cooldown.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class DeathSickleEvents {
    private static final String ACTIVE_END_TAG = "ValorSickleActiveEnd";
    private static final String COOLDOWN_END_TAG = "ValorSickleCooldownEnd";
    private static final String ACTIVE_TOTAL_TAG = "ValorSickleActiveTotal";
    private static final String COOLDOWN_TOTAL_TAG = "ValorSickleCooldownTotal";

    private static final UUID MOVE_UUID = UUID.fromString("5d0a7f7e-3c1b-4d6a-9a3e-6b1d2f4c8a01");
    private static final UUID ATTACK_UUID = UUID.fromString("5d0a7f7e-3c1b-4d6a-9a3e-6b1d2f4c8a02");

    private static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getLong(ACTIVE_END_TAG) > player.level().getGameTime();
    }

    private static boolean isActive(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof ServerPlayer player && isActive(player);
    }

    /** While immortal nothing can hurt the wearer (except what bypasses invulnerability: void, /kill). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (isActive(event.getEntity()) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** The hit that would kill: survive with 1 HP and start the immortality. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float health = player.getHealth();
        if (health - event.getAmount() > 0.0F) {
            return;
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN_END_TAG) > now) {
            return;
        }
        int level = DeathSickleItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        event.setAmount(Math.max(0.0F, health - 1.0F));
        activate(player, level, now);
    }

    private static void activate(ServerPlayer player, int level, long now) {
        int duration = Math.max(1, (int) Math.round(ValorConfig.sickleDurationSeconds(level) * 20.0D));
        int cooldown = Math.max(duration, ValorConfig.sickleCooldownSeconds() * 20);
        var data = player.getPersistentData();
        data.putLong(ACTIVE_END_TAG, now + duration);
        data.putInt(ACTIVE_TOTAL_TAG, duration);
        data.putLong(COOLDOWN_END_TAG, now + cooldown);
        data.putInt(COOLDOWN_TOTAL_TAG, cooldown);

        setModifier(player, Attributes.MOVEMENT_SPEED, MOVE_UUID, "valor_items:death_sickle_move",
                ValorConfig.sickleMovementSpeedPercent() / 100.0D, AttributeModifier.Operation.MULTIPLY_BASE);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_UUID, "valor_items:death_sickle_attack",
                ValorConfig.sickleAttackSpeedBonus(), AttributeModifier.Operation.ADDITION);
        clearHarmful(player);
        player.clearFire();

        if (player.level() instanceof ServerLevel level2) {
            level2.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        ORBIT_SENT.put(player.getUUID(), player.getId() * 2);
        sendOrbit(player, false); // cooldown started: the sickle stops circling
        // everybody who sees the player (and the player) draws them see-through
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SickleGhostPacket(player.getId(), duration));
        sync(player);
    }

    private static void setModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                    UUID uuid, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
            instance.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
        }
    }

    private static void removeModifiers(ServerPlayer player) {
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            move.removeModifier(MOVE_UUID);
        }
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attack != null) {
            attack.removeModifier(ATTACK_UUID);
        }
    }

    private static void clearHarmful(ServerPlayer player) {
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    /** No harmful effect can be applied while immortal. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (isActive(event.getEntity())
                && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 10 == 0) {
            updateOrbit(player);
        }
        if (isActive(player)) {
            if (player.isOnFire()) {
                player.clearFire();
            }
        } else if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null
                && (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(MOVE_UUID) != null
                || player.getAttribute(Attributes.ATTACK_SPEED).getModifier(ATTACK_UUID) != null)) {
            removeModifiers(player);
        }
    }

    /** player -> entity id * 2 + (1 if the orbiting sickle was shown): what the clients were last told. */
    private static final java.util.Map<UUID, Integer> ORBIT_SENT = new java.util.HashMap<>();

    /** The sickle circles around the wearer while it is ready (worn and off cooldown). */
    private static void updateOrbit(ServerPlayer player) {
        boolean ready = !isActive(player)
                && player.getPersistentData().getLong(COOLDOWN_END_TAG) <= player.level().getGameTime()
                && DeathSickleItem.getEquippedLevel(player) > 0;
        int state = player.getId() * 2 + (ready ? 1 : 0);
        Integer sent = ORBIT_SENT.get(player.getUUID());
        // also repeated every 5 seconds, for players that just came into range
        if (sent == null || sent != state || (ready && player.tickCount % 100 == 0)) {
            ORBIT_SENT.put(player.getUUID(), state);
            sendOrbit(player, ready);
        }
    }

    private static void sendOrbit(ServerPlayer player, boolean show) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SickleOrbitPacket(player.getId(), show));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ORBIT_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeModifiers(player); // the buffs are not saved; the immortality ends with the session
            player.getPersistentData().putLong(ACTIVE_END_TAG, 0L);
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

    private static void sync(ServerPlayer player) {
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        int activeLeft = (int) Math.max(0L, data.getLong(ACTIVE_END_TAG) - now);
        int cooldownLeft = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - now);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SickleStatePacket(activeLeft, data.getInt(ACTIVE_TOTAL_TAG),
                        cooldownLeft, data.getInt(COOLDOWN_TOTAL_TAG)));
    }

    private DeathSickleEvents() {}
}
