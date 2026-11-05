package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.api.ValorCritEvent;
import com.valor.items.item.BlitzBladeItem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.valor.items.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.valor.items.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Chain Lightning, the unique passive of the Blitz Blade:
 * <ul>
 *   <li>every basic attack (melee, arrows, tridents) deals extra magic damage equal to a percentage of the hit;</li>
 *   <li>every 3rd basic attack also jumps from the target to the nearest enemy, then from that one to the next, up to 5 jumps, dealing magic damage
 *       equal to a (bigger) percentage of the hit to each of them.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class BlitzBladeEvents {
    /** Every Nth basic attack spreads the lightning. */
    public static final int ATTACKS_PER_CHAIN = 3;
    /** Maximum number of jumps (secondary targets). */
    public static final int MAX_CHAIN_TARGETS = 5;
    /**
     * Maximum distance (in blocks) between the positions of two enemies for the lightning to jump from one to the other.
     * Two enemies standing in the middle of two blocks that have 3 empty blocks between them are 4.0 blocks apart.
     */
    public static final double CHAIN_RANGE = 4.0D;

    /** The count of attacks restarts if the player does not hit anything for this long (3 seconds). */
    public static final int HIT_RESET_TICKS = 60;

    /** Ticks between two jumps of the lightning, so that the chain can be seen going from enemy to enemy. */
    public static final int CHAIN_JUMP_DELAY_TICKS = 3;

    /** Chains that are still jumping. Only touched on the server thread. */
    private static final List<PendingChain> PENDING_CHAINS = new ArrayList<>();

    /** Prevents the extra damage from triggering itself. */
    private static boolean applying = false;

    /** Consecutive basic attacks of a player on one target. The count restarts when the target changes. */
    private static final class HitTracker {
        int hits;
        /** Game tick of the last counted attack: hits on several targets in the same tick are one attack. */
        long lastCountedTick = Long.MIN_VALUE / 2;
        long lastHitTime = Long.MIN_VALUE / 2;
    }

    private static final Map<UUID, HitTracker> HIT_TRACKERS = new HashMap<>();

    /** Game time (ticks) at which each player last triggered the chain. */
    private static final Map<UUID, Long> LAST_CHAIN_TIME = new HashMap<>();

    /** LivingEntity#lastHurt (protected). SRG name f_20898_, remapped automatically in dev. */
    private static final Field LAST_HURT = findLastHurtField();

    private static Field findLastHurtField() {
        try {
            return ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");
        } catch (RuntimeException e) {
            ValorItems.LOGGER.warn("Could not find LivingEntity#lastHurt, Chain Lightning will not restore it", e);
            return null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (applying) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || player == target) {
            return;
        }
        if (!isBasicAttack(source, player)) {
            return;
        }
        int level = BlitzBladeItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }

        // scales with the attack damage attribute of the player, not with the damage of the hit (so never a crit)
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        applying = true;
        try {
            // 1) extra magic damage on the target that was hit
            dealMagicDamage(target, player, (float) (attackDamage * BlitzBladeItem.getBonusDamagePercent(level) / 100.0D));

            // 2) every 3rd attack: the lightning jumps to other enemies
            // (3 attacks in a row on the SAME target; hitting another target restarts the count.
            // If the chain is still on cooldown, it fires on the first attack after the cooldown)
            UUID id = player.getUUID();
            HitTracker tracker = HIT_TRACKERS.computeIfAbsent(id, key -> new HitTracker());
            long now = player.level().getGameTime();
            if (now - tracker.lastHitTime > HIT_RESET_TICKS) { // any target counts
                tracker.hits = 0;
            }
            tracker.lastHitTime = now;
            if (tracker.lastCountedTick == now) {
                return; // another target of the same swing: it does not count as another attack
            }
            tracker.lastCountedTick = now;
            tracker.hits = Math.min(tracker.hits + 1, ATTACKS_PER_CHAIN);
            long last = LAST_CHAIN_TIME.getOrDefault(id, Long.MIN_VALUE / 2);
            if (tracker.hits >= ATTACKS_PER_CHAIN && now - last >= BlitzBladeItem.CHAIN_COOLDOWN_TICKS) {
                tracker.hits = 0;
                LAST_CHAIN_TIME.put(id, now);
                startChainLightning(target, player,
                        (float) (attackDamage * BlitzBladeItem.getSpreadDamagePercent(level) / 100.0D));
            }
        } finally {
            applying = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HIT_TRACKERS.remove(event.getEntity().getUUID());
        LAST_CHAIN_TIME.remove(event.getEntity().getUUID());
        PENDING_CHAINS.removeIf(chain -> chain.playerId.equals(event.getEntity().getUUID()));
    }

    /** One lightning bolt that is still jumping from enemy to enemy. */
    private static final class PendingChain {
        final ServerLevel level;
        final UUID playerId;
        final LivingEntity primary;
        final Set<LivingEntity> alreadyHit = new HashSet<>();
        final float damage;
        LivingEntity current;
        Vec3 currentPos;
        Vec3 currentCenter;
        int jumpsLeft = MAX_CHAIN_TARGETS;
        int delay = CHAIN_JUMP_DELAY_TICKS;

        PendingChain(ServerLevel level, UUID playerId, LivingEntity primary, float damage) {
            this.level = level;
            this.playerId = playerId;
            this.primary = primary;
            this.damage = damage;
            this.current = primary;
            this.currentPos = primary.position();
            this.currentCenter = center(primary);
            this.alreadyHit.add(primary);
        }
    }

    /**
     * Starts the chain: the main target takes the damage right away, then the lightning jumps to the nearest
     * enemy, from it to the next one, and so on (one jump every few ticks, see {@link #onServerTick}).
     */
    private static void startChainLightning(LivingEntity primary, ServerPlayer player, float damage) {
        if (damage <= 0.0F || !(primary.level() instanceof ServerLevel level)) {
            return;
        }
        dealMagicDamage(primary, player, damage);
        shockEffect(level, primary);
        level.playSound(null, primary.getX(), primary.getY(), primary.getZ(),
                ModSounds.LIGHTNING_CHAIN.get(), SoundSource.PLAYERS, 0.3F, 1.25F);
        PENDING_CHAINS.add(new PendingChain(level, player.getUUID(), primary, damage));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_CHAINS.isEmpty()) {
            return;
        }
        Iterator<PendingChain> iterator = PENDING_CHAINS.iterator();
        while (iterator.hasNext()) {
            PendingChain chain = iterator.next();
            if (--chain.delay > 0) {
                continue;
            }
            if (!jump(chain)) {
                iterator.remove();
            }
        }
    }

    /** Performs one jump of the chain. Returns true if the chain can still continue. */
    private static boolean jump(PendingChain chain) {
        ServerPlayer player = chain.level.getServer().getPlayerList().getPlayer(chain.playerId);
        if (player == null || chain.jumpsLeft <= 0) {
            return false;
        }
        // Where the lightning currently is (the last known place if that enemy has just died)
        boolean currentAlive = chain.current.isAlive();
        Vec3 fromPos = currentAlive ? chain.current.position() : chain.currentPos;
        Vec3 fromCenter = currentAlive ? center(chain.current) : chain.currentCenter;
        List<LivingEntity> candidates = chain.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(fromPos, fromPos).inflate(CHAIN_RANGE, CHAIN_RANGE, CHAIN_RANGE),
                entity -> !chain.alreadyHit.contains(entity)
                        && entity.position().distanceToSqr(fromPos) <= CHAIN_RANGE * CHAIN_RANGE
                        && isChainTarget(entity, player, chain.primary));
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = candidate.position().distanceToSqr(fromPos);
            if (distance < best) {
                best = distance;
                next = candidate;
            }
        }
        if (next == null) {
            return false;
        }

        boolean wasApplying = applying;
        applying = true;
        try {
            dealMagicDamage(next, player, chain.damage);
        } finally {
            applying = wasApplying;
        }
        Vec3 toPos = center(next);
        spawnArc(chain.level, fromCenter, toPos);
        shockEffect(chain.level, next);
        chain.level.playSound(null, toPos.x, toPos.y, toPos.z,
                ModSounds.LIGHTNING_CHAIN.get(), SoundSource.PLAYERS, 0.3F, 1.25F);

        chain.alreadyHit.add(next);
        chain.current = next;
        chain.currentPos = next.position();
        chain.currentCenter = toPos;
        chain.jumpsLeft--;
        chain.delay = CHAIN_JUMP_DELAY_TICKS;
        return chain.jumpsLeft > 0;
    }

    private static Vec3 center(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    /** One lightning spark on a shocked enemy. The "speed" x of the particle is the height of the effect. */
    private static void shockEffect(ServerLevel level, LivingEntity entity) {
        Vec3 c = center(entity);
        level.sendParticles(ModParticles.LIGHTNING_SPARK.get(), c.x, c.y, c.z, 0,
                Math.max(1.8D, Math.min(5.0D, entity.getBbHeight() * 2.4D)), entity.getId(), 0.0D, 1.0D);
    }

    /** One bolt between two points. The "speed" of the particle carries the vector from the start to the end. */
    private static void spawnArc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        level.sendParticles(ModParticles.CHAIN_LIGHTNING.get(), start.x, start.y, start.z, 0,
                delta.x, delta.y, delta.z, 1.0D);
    }

    /** Enemies (and whatever the player is fighting) near the main target; never the player, pets or allies. */
    private static boolean isChainTarget(LivingEntity entity, ServerPlayer player, LivingEntity primary) {
        if (entity == primary || entity == player || !entity.isAlive() || entity.isInvulnerable()) {
            return false;
        }
        if (entity instanceof Player || entity instanceof ArmorStand || entity.isAlliedTo(player)) {
            return false;
        }
        if (entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return entity instanceof Enemy
                || (entity instanceof Mob mob && mob.getTarget() == player)
                || entity.getType() == primary.getType();
    }

    /** Vanilla magic damage credited to the player. Ignores the invulnerability frames of the target. */
    private static void dealMagicDamage(LivingEntity target, ServerPlayer player, float amount) {
        dealMagicDamage(target, player, amount, false);
    }

    /** Magic damage of the player (vanilla damage type); with keepVelocity the target is not knocked back. */
    public static void dealMagicDamage(LivingEntity target, ServerPlayer player, float amount, boolean keepVelocity) {
        dealDamage(target, target.level().damageSources().indirectMagic(null, player), amount, keepVelocity);
    }

    /**
     * Extra damage of a relic with the given source. It ignores the invulnerability frames of the target and never
     * takes the critical hit mark of the basic attack (the damage of a relic cannot crit).
     */
    public static void dealDamage(LivingEntity target, DamageSource source, float amount, boolean keepVelocity) {
        if (amount <= 0.0F || !target.isAlive()) {
            return;
        }
        net.minecraft.world.phys.Vec3 velocity = target.getDeltaMovement();
        // Vanilla also remembers the damage of the last hit (lastHurt); keep the value of the outer hit.
        int previousInvulnerableTime = target.invulnerableTime;
        float previousLastHurt = readLastHurt(target);
        target.invulnerableTime = 0;
        var data = target.getPersistentData();
        boolean hadCritMark = data.contains(ValorCritEvent.LAST_CRIT_TAG);
        long critMark = data.getLong(ValorCritEvent.LAST_CRIT_TAG);
        data.remove(ValorCritEvent.LAST_CRIT_TAG);
        try {
            target.hurt(source, amount);
        } finally {
            if (hadCritMark) {
                data.putLong(ValorCritEvent.LAST_CRIT_TAG, critMark);
            }
        }
        target.invulnerableTime = previousInvulnerableTime;
        writeLastHurt(target, previousLastHurt);
        if (keepVelocity) {
            target.setDeltaMovement(velocity);
        }
    }

    private static float readLastHurt(LivingEntity entity) {
        if (LAST_HURT != null) {
            try {
                return LAST_HURT.getFloat(entity);
            } catch (IllegalAccessException ignored) {
                // fall through
            }
        }
        return Float.NaN;
    }

    private static void writeLastHurt(LivingEntity entity, float value) {
        if (LAST_HURT != null && !Float.isNaN(value)) {
            try {
                LAST_HURT.setFloat(entity, value);
            } catch (IllegalAccessException ignored) {
                // nothing else to do
            }
        }
    }

    private static boolean isBasicAttack(DamageSource source, ServerPlayer player) {
        // Melee (also sweeping attacks)
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            return true;
        }
        // Ranged: arrows, tridents and modded arrows fired by the player
        return source.getDirectEntity() instanceof AbstractArrow;
    }

    private BlitzBladeEvents() {}
}
