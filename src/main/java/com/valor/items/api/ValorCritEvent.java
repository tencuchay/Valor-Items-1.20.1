package com.valor.items.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted on the Forge event bus (server side) when a basic attack of a player is a critical hit of Valor Items.
 * It is posted during LivingHurtEvent, before the damage is applied; {@link #getDamage()} is the damage with the
 * critical hit already applied (before armor).
 */
public class ValorCritEvent extends Event {
    /** Persistent data of the target: game time of the last critical hit it received (long). */
    public static final String LAST_CRIT_TAG = "ValorLastCritTick";

    private final ServerPlayer attacker;
    private final LivingEntity target;
    private final float damage;
    private final boolean ranged;

    public ValorCritEvent(ServerPlayer attacker, LivingEntity target, float damage, boolean ranged) {
        this.attacker = attacker;
        this.target = target;
        this.damage = damage;
        this.ranged = ranged;
    }

    public ServerPlayer getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }
    public float getDamage() { return damage; }
    public boolean isRanged() { return ranged; }
}
