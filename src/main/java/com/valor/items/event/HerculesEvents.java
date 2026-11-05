package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.HerculesMadnessItem;
import com.valor.items.network.HerculesStatePacket;
import com.valor.items.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Hercules' Madness.
 * <ul>
 *   <li>"Berserk": a normal attack deals extra damage equal to a percent of the max health (never a crit; it is
 *       added after the critical hit is decided). Below half of the health the extra damage grows, more the lower
 *       the health is.</li>
 *   <li>"Blood Madness": the player loses a part of the current health little by little (never dying of it), then
 *       gets a shield for a few seconds.</li>
 *   <li>The shield: a damage that is not true damage is stopped by the shield first; only what goes past it hurts
 *       the health. True damage ignores the shield.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class HerculesEvents {
    private static final String COOLDOWN_END_TAG = "ValorHerculesCooldownEnd";
    private static final String COOLDOWN_TOTAL_TAG = "ValorHerculesCooldownTotal";
    /** Persistent data of the player: shield points absorbed in a tick and that game time (for other mods). */
    public static final String ABSORBED_TAG = "ValorShieldAbsorbed";
    public static final String ABSORBED_TICK_TAG = "ValorShieldAbsorbedTick";
    private static final String RAMP_END_TAG = "ValorHerculesRampEnd";
    private static final String RAMP_STEP_TAG = "ValorHerculesRampStep";
    /** The shield starts with this part of its full value and grows to the full value while the health drains. */
    private static final double START_FRACTION = 0.10D;
    private static final String DRAIN_END_TAG = "ValorHerculesDrainEnd";
    private static final String DRAIN_COST_TAG = "ValorHerculesDrainCost";
    private static final String DRAIN_TOTAL_TAG = "ValorHerculesDrainTotal";
    private static final String DRAIN_DONE_TAG = "ValorHerculesDrainDone";
    /** The health is lost in this many equal pulses over the drain time. */
    private static final int DRAIN_PULSES = 4;

    /** Damage of Berserk: a normal damage of the player (armor applies), it never crits. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> BERSERK =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    new net.minecraft.resources.ResourceLocation(ValorItems.MODID, "berserk"));

    /** Prevents the extra damage from triggering itself. */
    private static boolean applying = false;
    private static final String LEVEL_TAG = "ValorHerculesLevel";

    /** The shield is the base value of the valor_items:shield attribute: the points that are left of it. */
    private static float getShield(ServerPlayer player) {
        var attribute = player.getAttribute(com.valor.items.registry.ModAttributes.SHIELD.get());
        return attribute == null ? 0.0F : (float) attribute.getBaseValue();
    }

    private static void setShield(ServerPlayer player, float value) {
        var attribute = player.getAttribute(com.valor.items.registry.ModAttributes.SHIELD.get());
        if (attribute != null) {
            attribute.setBaseValue(Math.max(0.0F, value));
        }
    }
    private static final String SHIELD_END_TAG = "ValorHerculesShieldEnd";
    private static final String SHIELD_TOTAL_TAG = "ValorHerculesShieldTotal";

    // ---- Berserk ----

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (applying) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        Entity direct = source.getDirectEntity();
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) && direct == player;
        boolean ranged = direct instanceof AbstractArrow arrow && arrow.getOwner() == player;
        if (!melee && !ranged) {
            return;
        }
        int level = HerculesMadnessItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        double extra = player.getMaxHealth() * ValorConfig.berserkPercent(level) / 100.0D;
        double ratio = player.getHealth() / player.getMaxHealth();
        double threshold = ValorConfig.berserkThresholdPercent() / 100.0D;
        if (ratio < threshold) {
            // from the minimum at the threshold to the maximum at an empty health bar
            double lost = Math.min(1.0D, Math.max(0.0D, (threshold - ratio) / threshold));
            double growth = ValorConfig.berserkLowMinPercent()
                    + (ValorConfig.berserkLowMaxPercent() - ValorConfig.berserkLowMinPercent()) * lost;
            extra *= 1.0D + growth / 100.0D;
        }
        // like Chain Lightning, a separate hit on the same target; the growth only applies to this extra damage
        var damageTypes = player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
        applying = true;
        try {
            BlitzBladeEvents.dealDamage(event.getEntity(), new DamageSource(damageTypes.getHolderOrThrow(BERSERK), player),
                    (float) extra, false);
        } finally {
            applying = false;
        }
    }

    // ---- Blood Madness ----

    /** The active ability: called when the player presses its key. */
    public static void tryActivate(ServerPlayer player) {
        int level = HerculesMadnessItem.getEquippedLevel(player);
        if (level <= 0 || !player.isAlive()) {
            return;
        }
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.getLong(COOLDOWN_END_TAG) > now) {
            player.displayClientMessage(Component.translatable("message.valor_items.skill_on_cooldown")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        int drainTicks = Math.max(DRAIN_PULSES, (int) Math.round(ValorConfig.madnessDrainSeconds() * 20.0D));
        // it never kills: at least one health point is left
        double cost = Math.min(player.getHealth() * ValorConfig.madnessDrainPercent() / 100.0D,
                Math.max(0.0D, player.getHealth() - 1.0D));
        int cooldown = Math.max(1, ValorConfig.madnessCooldownSeconds() * 20);
        data.putLong(DRAIN_END_TAG, now + drainTicks);
        data.putDouble(DRAIN_COST_TAG, cost);
        data.putInt(DRAIN_TOTAL_TAG, drainTicks);
        data.putInt(DRAIN_DONE_TAG, 0);
        data.putInt(LEVEL_TAG, level);
        data.putLong(COOLDOWN_END_TAG, now + cooldown);
        data.putInt(COOLDOWN_TOTAL_TAG, cooldown);
        // the shield is there at once with a part of its value and grows to the full value in the time of the drain;
        // it lasts for the shield time counted from now
        int shieldTicks = Math.max(1, (int) Math.round(ValorConfig.madnessShieldSeconds() * 20.0D));
        double full = player.getMaxHealth() * ValorConfig.madnessShieldPercent(level) / 100.0D;
        setShield(player, (float) (full * START_FRACTION));
        data.putLong(SHIELD_END_TAG, now + shieldTicks);
        data.putInt(SHIELD_TOTAL_TAG, shieldTicks);
        data.putLong(RAMP_END_TAG, now + drainTicks);
        data.putFloat(RAMP_STEP_TAG, (float) (full * (1.0D - START_FRACTION) / drainTicks));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_HURT,
                SoundSource.PLAYERS, 0.5F, 0.7F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        long drainEnd = data.getLong(DRAIN_END_TAG);
        if (drainEnd != 0L) {
            if (!player.isAlive() || !HerculesMadnessItem.isEquipped(player)) {
                data.putLong(DRAIN_END_TAG, 0L);
            } else {
                // the health is set directly (no hurt animation, no screen shake), in 4 equal pulses
                int total = Math.max(1, data.getInt(DRAIN_TOTAL_TAG));
                long elapsed = Math.max(0L, now - (drainEnd - total));
                int due = (int) Math.min(DRAIN_PULSES, elapsed * DRAIN_PULSES / total);
                int done = data.getInt(DRAIN_DONE_TAG);
                if (due > done) {
                    float lost = (float) (data.getDouble(DRAIN_COST_TAG) / DRAIN_PULSES * (due - done));
                    player.setHealth(Math.max(1.0F, player.getHealth() - lost));
                    data.putInt(DRAIN_DONE_TAG, due);
                }
                if (now >= drainEnd) {
                    data.putLong(DRAIN_END_TAG, 0L);
                }
            }
        }
        long rampEnd = data.getLong(RAMP_END_TAG);
        if (rampEnd != 0L) {
            // the shield grows a little every tick (what it already took is not given back)
            setShield(player, getShield(player) + data.getFloat(RAMP_STEP_TAG));
            if (now >= rampEnd) {
                data.putLong(RAMP_END_TAG, 0L);
                rampEnd = 0L;
            }
        }
        long shieldEnd = data.getLong(SHIELD_END_TAG);
        if (shieldEnd != 0L && (now >= shieldEnd || (rampEnd == 0L && getShield(player) <= 0.0F)
                || !HerculesMadnessItem.isEquipped(player))) {
            clearShield(player);
        }
    }

    private static void clearShield(ServerPlayer player) {
        var data = player.getPersistentData();
        setShield(player, 0.0F);
        data.putLong(SHIELD_END_TAG, 0L);
        data.putLong(RAMP_END_TAG, 0L);
        sync(player);
    }

    // ---- the shield ----

    /**
     * The damage after the armor and the other reductions: the shield takes it first. True damage (and what even
     * ignores invulnerability, like /kill and the void) goes straight to the health.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        float shield = getShield(player);
        long shieldEnd = data.getLong(SHIELD_END_TAG);
        if (shield <= 0.0F || (shieldEnd != 0L && shieldEnd <= player.level().getGameTime())) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(MailOfPainEvents.TRUE_DAMAGE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        float absorbed = Math.min(shield, amount);
        setShield(player, shield - absorbed);
        event.setAmount(amount - absorbed);
        // for other mods (damage indicators): how much the shield took in this tick (added up if there are several hits)
        long tick = player.level().getGameTime();
        float total = data.getLong(ABSORBED_TICK_TAG) == tick ? data.getFloat(ABSORBED_TAG) + absorbed : absorbed;
        data.putFloat(ABSORBED_TAG, total);
        data.putLong(ABSORBED_TICK_TAG, tick);
        sync(player);
    }

    // ---- state ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putLong(DRAIN_END_TAG, 0L);
            clearShield(player);
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
        int shieldLeft = (int) Math.max(0L, data.getLong(SHIELD_END_TAG) - now);
        int cooldownLeft = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - now);
        float shield = shieldLeft > 0 ? getShield(player) : 0.0F;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new HerculesStatePacket(shield, shieldLeft, Math.max(1, data.getInt(SHIELD_TOTAL_TAG)), cooldownLeft,
                        Math.max(1, data.getInt(COOLDOWN_TOTAL_TAG))));
    }

    private HerculesEvents() {}
}
