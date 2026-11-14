package com.valor.items.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The ice of Arctic Orb (GeckoLib model "arctic_ice"): it appears on the player, and the animation of its level makes
 * it grow, stay and disappear. It is only a picture: no collision, nothing hits it, and it is never saved.
 */
public class ArcticIceEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Integer> LEVEL =
            SynchedEntityData.defineId(ArcticIceEntity.class, EntityDataSerializers.INT);

    /** Entity id of the player who owns the ice (-1 = none): his own first person view does not draw it. */
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(ArcticIceEntity.class, EntityDataSerializers.INT);

    /** Animation of every relic level (they are in arctic_ice.animation.json). */
    private static final String[] ANIMATIONS = {"spawn_n_disappear_1.5s", "spawn_n_disappear_1.75s",
            "spawn_n_disappear_2s", "spawn_n_disappear_2.5s", "spawn_n_disappear_3s"};
    /** Length of those animations, in ticks (1.75s, 2s, 2.25s, 2.75s, 3.25s). */
    private static final int[] LIFE_TICKS = {35, 40, 45, 55, 65};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArcticIceEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void setRelicLevel(int level) {
        this.entityData.set(LEVEL, Math.max(1, Math.min(5, level)));
    }

    public void setOwnerId(int id) {
        this.entityData.set(OWNER, id);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER);
    }

    public int getRelicLevel() {
        return this.entityData.get(LEVEL);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LEVEL, 1);
        this.entityData.define(OWNER, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= LIFE_TICKS[getRelicLevel() - 1]) {
            if (!this.level().isClientSide) {
                // the ice is gone
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.SNOW_GOLEM_DEATH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
                // the ice shatters: shards of ice fly out
                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                                    net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),
                            this.getX(), this.getY() + 1.0D, this.getZ(), 60, 0.6D, 0.9D, 0.6D, 0.15D);
                }
            }
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "ice", 0, this::animate));
    }

    private PlayState animate(AnimationState<ArcticIceEntity> state) {
        return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(ANIMATIONS[getRelicLevel() - 1]));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
