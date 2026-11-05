package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.ClavesSanctiItem;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Claves Sancti: "Gift of the Swift" (movement speed for a short time after a critical hit of a ranged weapon) and
 * "Infinite" (extra crit damage from the crit rate, see {@link #updateInfinite(ServerPlayer)}).
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ClavesSanctiEvents {
    private static final String END_TAG = "ValorSwiftEnd";
    private static final UUID INFINITE_RATE_UUID = UUID.fromString("e4a91c36-7d28-4b05-a3f1-8c6d0b2e5f17");
    private static final UUID INFINITE_DAMAGE_UUID = UUID.fromString("1f6b3d89-2c4e-4a70-b5d8-9e0a7c3f6b42");
    private static final UUID SPEED_UUID = UUID.fromString("7b1e4d52-6c0a-4a43-9f1d-2e8b5c3a7d64");

    /** Called by CombatEvents when a projectile of the player lands a critical hit. */
    static void onRangedCrit(ServerPlayer player) {
        int level = ClavesSanctiItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_UUID); // refreshed, not stacked
        speed.addTransientModifier(new AttributeModifier(SPEED_UUID, "valor_items:gift_of_the_swift",
                ValorConfig.swiftPercent(level) / 100.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        int duration = Math.max(1, (int) Math.round(ValorConfig.swiftSeconds() * 20.0D));
        player.getPersistentData().putLong(END_TAG, player.level().getGameTime() + duration);
    }

    /**
     * "Infinite" is two attribute modifiers on the player (with fixed ids, so wearing several Claves Sancti never
     * stacks them): the crit rate, and the crit damage, which is the fixed bonus plus a bit more for every step of
     * crit rate, up to a maximum. They are updated every tick, so the attributes always show the real values.
     */
    private static void updateInfinite(ServerPlayer player) {
        AttributeInstance rate = player.getAttribute(ModAttributes.CRIT_RATE.get());
        AttributeInstance damage = player.getAttribute(ModAttributes.CRIT_DAMAGE.get());
        if (rate == null || damage == null) {
            return;
        }
        if (ClavesSanctiItem.getEquippedLevel(player) <= 0) {
            rate.removeModifier(INFINITE_RATE_UUID);
            damage.removeModifier(INFINITE_DAMAGE_UUID);
            return;
        }
        setModifier(rate, INFINITE_RATE_UUID, "valor_items:infinite_crit_rate",
                ValorConfig.infiniteCritRatePercent() / 100.0D);
        // the crit rate with the modifier above; the crit damage modifier does not change it
        double steps = rate.getValue() * 100.0D / ValorConfig.infiniteRateStepPercent();
        double bonus = Math.min(steps * ValorConfig.infiniteDamagePerStepPercent(),
                ValorConfig.infiniteMaxBonusPercent()) / 100.0D;
        setModifier(damage, INFINITE_DAMAGE_UUID, "valor_items:infinite_crit_damage",
                ValorConfig.infiniteCritDamagePercent() / 100.0D + bonus);
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        AttributeModifier current = attribute.getModifier(id);
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-9D) {
            return;
        }
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        updateInfinite(player);
        var data = player.getPersistentData();
        if (data.getLong(END_TAG) == 0L) {
            return;
        }
        if (data.getLong(END_TAG) <= player.level().getGameTime()
                || ClavesSanctiItem.getEquippedLevel(player) <= 0) {
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

    private static void clear(ServerPlayer player) {
        player.getPersistentData().putLong(END_TAG, 0L);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_UUID);
        }
    }

    private ClavesSanctiEvents() {}
}
