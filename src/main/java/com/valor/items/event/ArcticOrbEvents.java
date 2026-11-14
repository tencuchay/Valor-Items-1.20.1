package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.entity.ArcticIceEntity;
import com.valor.items.item.ArcticOrbItem;
import com.valor.items.network.ArcticStatePacket;
import com.valor.items.network.ModNetwork;
import com.valor.items.registry.ModAttributes;
import com.valor.items.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Arctic Orb - "Impervious" (active): for a few seconds the player is encased in ice: no damage (except what ignores
 * invulnerability, like /kill and the void), no harmful effects, no knockback, but he cannot move, attack, use items
 * or use active skills. When the ice goes, a shield (valor_items:shield) of some points plus a part of the magic damage
 * stays for a short time.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ArcticOrbEvents {
    private static final String COOLDOWN_END_TAG = "ValorArcticCooldownEnd";
    private static final String COOLDOWN_TOTAL_TAG = "ValorArcticCooldownTotal";
    private static final String FREEZE_END_TAG = "ValorArcticFreezeEnd";
    private static final String SHIELD_END_TAG = "ValorArcticShieldEnd";
    private static final String ACTIVE_TOTAL_TAG = "ValorArcticActiveTotal";
    private static final String SHIELD_TICKS_TAG = "ValorArcticShieldTicks";
    private static final String FREEZE_X_TAG = "ValorArcticX";
    private static final String FREEZE_Z_TAG = "ValorArcticZ";
    private static final String FREEZE_Y_TAG = "ValorArcticY";
    private static final String NO_GRAVITY_TAG = "ValorArcticNoGravity";
    private static final String HERCULES_SHIELD_END_TAG = "ValorHerculesShieldEnd";

    public static boolean isFrozen(Player player) {
        return player.getPersistentData().getLong(FREEZE_END_TAG) > player.level().getGameTime();
    }

    // ---- the active ability ----

    public static void tryActivate(ServerPlayer player) {
        int level = ArcticOrbItem.getEquippedLevel(player);
        if (level <= 0 || !player.isAlive() || isFrozen(player)) {
            return;
        }
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.getLong(COOLDOWN_END_TAG) > now) {
            player.displayClientMessage(Component.translatable("message.valor_items.skill_on_cooldown")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        int freezeTicks = Math.max(1, (int) Math.round(ValorConfig.arcticFreezeSeconds(level) * 20.0D));
        int shieldTicks = Math.max(1, (int) Math.round(ValorConfig.arcticShieldSeconds() * 20.0D));
        int cooldown = Math.max(1, ValorConfig.arcticCooldownSeconds() * 20);
        data.putLong(FREEZE_END_TAG, now + freezeTicks);
        data.putLong(SHIELD_END_TAG, 0L);
        data.putInt(ACTIVE_TOTAL_TAG, freezeTicks);
        data.putInt(SHIELD_TICKS_TAG, shieldTicks);
        data.putLong(COOLDOWN_END_TAG, now + cooldown);
        data.putInt(COOLDOWN_TOTAL_TAG, cooldown);
        data.putDouble(FREEZE_X_TAG, player.getX());
        data.putDouble(FREEZE_Z_TAG, player.getZ());
        data.putDouble(FREEZE_Y_TAG, player.getY());
        player.stopUsingItem();
        player.setSprinting(false);
        // the player stops where he is, even in the air: no gravity until the ice is gone
        player.setNoGravity(true);
        data.putBoolean(NO_GRAVITY_TAG, true);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.hurtMarked = true;

        ArcticIceEntity ice = new ArcticIceEntity(ModEntities.ARCTIC_ICE.get(), player.level());
        ice.setRelicLevel(level);
        ice.setOwnerId(player.getId());
        // the ice is turned the way the player looks at that moment and stays so
        ice.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        player.level().addFreshEntity(ice);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // snow flying out when the ice appears
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.7D, 0.9D, 0.7D, 0.08D);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        long freezeEnd = data.getLong(FREEZE_END_TAG);
        if (freezeEnd != 0L) {
            if (now >= freezeEnd || !player.isAlive()) {
                endFreeze(player, player.isAlive());
            } else {
                // cannot move: whatever the client does, the player is put back
                player.stopUsingItem();
                player.setSprinting(false);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.fallDistance = 0.0F;
                double dx = player.getX() - data.getDouble(FREEZE_X_TAG);
                double dy = player.getY() - data.getDouble(FREEZE_Y_TAG);
                double dz = player.getZ() - data.getDouble(FREEZE_Z_TAG);
                if (dx * dx + dy * dy + dz * dz > 0.0004D) {
                    player.connection.teleport(data.getDouble(FREEZE_X_TAG), data.getDouble(FREEZE_Y_TAG),
                            data.getDouble(FREEZE_Z_TAG), player.getYRot(), player.getXRot());
                }
            }
        }
        long shieldEnd = data.getLong(SHIELD_END_TAG);
        if (shieldEnd != 0L && (now >= shieldEnd || !player.isAlive())) {
            data.putLong(SHIELD_END_TAG, 0L);
            // do not take away a shield that belongs to Hercules' Madness
            if (data.getLong(HERCULES_SHIELD_END_TAG) == 0L) {
                var shield = player.getAttribute(ModAttributes.SHIELD.get());
                if (shield != null) {
                    shield.setBaseValue(0.0D);
                }
            }
            sync(player);
        }
    }

    private static void releaseGravity(ServerPlayer player) {
        var data = player.getPersistentData();
        if (data.getBoolean(NO_GRAVITY_TAG)) {
            data.putBoolean(NO_GRAVITY_TAG, false);
            player.setNoGravity(false);
            player.fallDistance = 0.0F;
        }
    }

    /** The ice is gone: the player can move again and the shield starts. */
    private static void endFreeze(ServerPlayer player, boolean giveShield) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        data.putLong(FREEZE_END_TAG, 0L);
        releaseGravity(player);
        if (giveShield) {
            var magic = player.getAttribute(ModAttributes.MAGIC_DAMAGE.get());
            double amount = ValorConfig.arcticShieldBase()
                    + (magic == null ? 0.0D : magic.getValue()) * ValorConfig.arcticShieldMagicPercent() / 100.0D;
            var shield = player.getAttribute(ModAttributes.SHIELD.get());
            if (shield != null) {
                shield.setBaseValue(Math.max(0.0D, shield.getBaseValue()) + amount);
            }
            data.putLong(SHIELD_END_TAG, now + Math.max(1, (int) Math.round(ValorConfig.arcticShieldSeconds() * 20.0D)));
        }
        sync(player);
    }

    // ---- immunity ----

    /** Nothing hurts the frozen player, except what ignores invulnerability (the void, /kill). */
    /** A frozen player cannot hurt anything either (any weapon, any mod's attack, relic damage). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttacker(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()
                && isFrozen(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackerHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()
                && isFrozen(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player && isFrozen(player)) {
            event.setCanceled(true);
        }
    }

    /** No harmful effect can be put on the frozen player. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && isFrozen(player)
                && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ---- cannot attack, use items or skills ----

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && isFrozen(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ---- state ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = player.getPersistentData();
            data.putLong(FREEZE_END_TAG, 0L);
            releaseGravity(player);
            if (data.getLong(SHIELD_END_TAG) != 0L && data.getLong(HERCULES_SHIELD_END_TAG) == 0L) {
                var shield = player.getAttribute(ModAttributes.SHIELD.get());
                if (shield != null) {
                    shield.setBaseValue(0.0D);
                }
            }
            data.putLong(SHIELD_END_TAG, 0L);
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
        int freezeLeft = (int) Math.max(0L, data.getLong(FREEZE_END_TAG) - now);
        // the line on the HUD only counts the immunity (not the shield)
        int activeLeft = freezeLeft;
        int cooldownLeft = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - now);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ArcticStatePacket(freezeLeft, activeLeft, Math.max(1, data.getInt(ACTIVE_TOTAL_TAG)), cooldownLeft,
                        Math.max(1, data.getInt(COOLDOWN_TOTAL_TAG))));
    }

    private ArcticOrbEvents() {}
}
