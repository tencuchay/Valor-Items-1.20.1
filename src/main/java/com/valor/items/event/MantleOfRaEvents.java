package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.MantleOfRaItem;
import com.valor.items.registry.ModEffects;
import com.valor.items.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mantle of Ra: the "Burn" aura, applied once per second while the relic is worn.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class MantleOfRaEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 20 != 0 || !player.isAlive() || player.isSpectator()) {
            return;
        }
        int relicLevel = MantleOfRaItem.getEquippedLevel(player);
        if (relicLevel <= 0) {
            return;
        }
        double range = ValorConfig.burnRange(relicLevel);

        shootRing(level, player, range);

        // burn
        double percent = ValorConfig.burnPercent(relicLevel) / 100.0D;
        double arthropodMultiplier = 1.0D + ValorConfig.burnArthropodBonusPercent() / 100.0D;
        var targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                entity -> isBurnTarget(entity, player));
        for (LivingEntity target : targets) {
            // percent of the max health of the WEARER, not of the enemy
            double damage = player.getMaxHealth() * percent;
            if (target.getMobType() == MobType.ARTHROPOD) {
                damage *= arthropodMultiplier;
            }
            BlitzBladeEvents.dealMagicDamage(target, player, (float) damage, true);
            // Soul Prison: the effect is refreshed, never stacked
            if (target.isAlive()) {
                int ticks = Math.max(1, (int) Math.round(ValorConfig.soulPrisonSeconds() * 20.0D));
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.SOUL_PRISON.get(),
                        ticks, 0, false, false, true));
                // the effects of mobs are not sent to the clients, so the icon above the head needs its own packet
                com.valor.items.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                        new com.valor.items.network.SoulPrisonPacket(target.getId(), ticks));
            }
            // count 0: the three offsets are the velocity of the particle
            level.sendParticles(ModParticles.RA_FIRE.get(), target.getX(),
                    target.getY() + target.getBbHeight() * 0.4D, target.getZ(), 0, 0.0D, 0.03D, 0.0D, 1.0D);
        }
    }

    /** Life of the flames of the ring, in ticks: the same number as in RaFlameParticle. */
    private static final int RING_LIFETIME_TICKS = 12;
    private static final int RING_FLAMES = 24;

    /** Every second a ring of small and big flames is shot out of the wearer, up to the edge of the aura. */
    private static void shootRing(ServerLevel level, ServerPlayer player, double range) {
        double start = 0.3D;
        double speed = Math.max(0.02D, (range - start) / RING_LIFETIME_TICKS);
        double y = player.getY() + 0.35D;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.3F, 1.0F);
        var random = level.getRandom();
        for (int i = 0; i < RING_FLAMES; i++) {
            // a little random in angle, speed, height and sideways drift, but still a ring
            double angle = (Math.PI * 2.0D) * (i + (random.nextDouble() - 0.5D) * 0.7D) / RING_FLAMES;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            // about a third of the flames lag behind, so the ring has some depth
            double radial = random.nextInt(3) == 0
                    ? speed * (0.35D + random.nextDouble() * 0.3D)
                    : speed * (0.85D + random.nextDouble() * 0.4D);
            double side = (random.nextDouble() - 0.5D) * speed * 0.5D;
            double vx = dx * radial - dz * side;
            double vz = dz * radial + dx * side;
            double vy = (random.nextDouble() - 0.3D) * 0.04D;
            double r = start + random.nextDouble() * 0.2D;
            var type = random.nextBoolean() ? ModParticles.RA_FLAME.get() : ModParticles.RA_FIRE.get();
            level.sendParticles(type, player.getX() + dx * r, y + (random.nextDouble() - 0.5D) * 0.3D,
                    player.getZ() + dz * r, 0, vx, vy, vz, 1.0D);
        }
    }

    /** Enemies (and whatever is fighting the player); never the player, other players, pets or allies. */
    private static boolean isBurnTarget(LivingEntity entity, ServerPlayer player) {
        if (entity == player || !entity.isAlive() || entity.isInvulnerable()) {
            return false;
        }
        if (entity instanceof Player || entity instanceof ArmorStand || entity.isAlliedTo(player)) {
            return false;
        }
        if (entity instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return entity instanceof Enemy || (entity instanceof Mob mob && mob.getTarget() == player);
    }

    private MantleOfRaEvents() {}
}
