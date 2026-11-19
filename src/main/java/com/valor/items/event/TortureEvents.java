package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.BerithsAgonyItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Berith's Agony: "Torture" (Thống Khổ). A normal attack that hits marks the target: once every second, for the
 * duration of the passive (3 s), the target takes magic damage equal to a percent of its CURRENT health.
 * The magic damage is credited to the wearer (magic armor / magic penetration / magic lifesteal apply).
 * A new hit refreshes the duration but never stacks, and keeps the one-second rhythm of the damage.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class TortureEvents {
    private static final int INTERVAL_TICKS = 20;

    private static final class Torture {
        final LivingEntity target;
        UUID owner;
        int level;
        long nextTick;
        int procsLeft;

        Torture(LivingEntity target) {
            this.target = target;
        }
    }

    /** Tortured targets by UUID. Only touched on the server thread. */
    private static final Map<UUID, Torture> ACTIVE = new HashMap<>();

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !LifestealEvents.isBasicAttack(event.getSource(), player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || target instanceof ArmorStand || event.getAmount() <= 0.0F) {
            return;
        }
        int level = BerithsAgonyItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        int procs = Math.max(1, (int) Math.round(ValorConfig.tortureSeconds() * 20.0D / INTERVAL_TICKS));
        Torture torture = ACTIVE.get(target.getUUID());
        if (torture == null || torture.target != target || torture.procsLeft <= 0) {
            torture = new Torture(target);
            torture.nextTick = now + INTERVAL_TICKS;
            ACTIVE.put(target.getUUID(), torture);
        }
        // refresh: the latest attacker and level are used, the rhythm of the damage is kept
        torture.owner = player.getUUID();
        torture.level = level;
        torture.procsLeft = procs;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        var server = event.getServer();
        Iterator<Torture> it = ACTIVE.values().iterator();
        // collect first: dealing the damage must not modify the map while iterating
        java.util.List<Torture> due = new java.util.ArrayList<>();
        while (it.hasNext()) {
            Torture torture = it.next();
            LivingEntity target = torture.target;
            if (target.isRemoved() || !target.isAlive() || torture.procsLeft <= 0) {
                it.remove();
                continue;
            }
            if (target.level().getGameTime() >= torture.nextTick) {
                due.add(torture);
            }
        }
        for (Torture torture : due) {
            LivingEntity target = torture.target;
            ServerPlayer owner = server.getPlayerList().getPlayer(torture.owner);
            if (owner == null) {
                torture.procsLeft = 0;
                continue;
            }
            torture.procsLeft--;
            torture.nextTick += INTERVAL_TICKS;
            // the health the target has now
            float damage = (float) (target.getHealth() * ValorConfig.torturePercent(torture.level) / 100.0D);
            BlitzBladeEvents.dealMagicDamage(target, owner, damage, true);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }

    private TortureEvents() {}
}
