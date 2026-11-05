package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mana.
 * <ul>
 *   <li>valor_items:max_mana: 400 for every player, +10 for every experience level reached.</li>
 *   <li>Losing levels never takes mana away. The highest level ever reached is remembered, and mana is only added
 *       again when the player goes above it.</li>
 *   <li>valor_items:mana: the base value of this attribute is the mana the player has now. The mana is never above
 *       max_mana.</li>
 *   <li>valor_items:mana_regen: mana regenerated every 5 seconds (40 for every player), a fifth of it every second.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ManaEvents {
    /** Mana added by every experience level reached for the first time. */
    public static final double MANA_PER_LEVEL = 10.0D;

    private static final String PEAK_TAG = "ValorManaPeakLevel";
    private static final String INIT_TAG = "ValorManaInit";
    private static final UUID LEVEL_UUID = UUID.fromString("b8e41d72-5a3c-4f69-9d0e-2c7a6b1f4e83");

    // ---- for the relics ----

    public static double getMana(Player player) {
        AttributeInstance mana = player.getAttribute(ModAttributes.MANA.get());
        return mana == null ? 0.0D : mana.getBaseValue();
    }

    public static double getMaxMana(Player player) {
        return player.getAttributeValue(ModAttributes.MAX_MANA.get());
    }

    public static void setMana(Player player, double value) {
        AttributeInstance mana = player.getAttribute(ModAttributes.MANA.get());
        if (mana != null) {
            mana.setBaseValue(Math.max(0.0D, Math.min(value, getMaxMana(player))));
        }
    }

    public static void addMana(Player player, double amount) {
        setMana(player, getMana(player) + amount);
    }

    /** Takes the mana if the player has enough of it. */
    public static boolean consume(Player player, double amount) {
        if (getMana(player) < amount) {
            return false;
        }
        setMana(player, getMana(player) - amount);
        return true;
    }

    // ---- levels ----

    private static void applyLevelBonus(Player player, int peak) {
        AttributeInstance maxMana = player.getAttribute(ModAttributes.MAX_MANA.get());
        if (maxMana == null) {
            return;
        }
        double bonus = peak * MANA_PER_LEVEL;
        AttributeModifier current = maxMana.getModifier(LEVEL_UUID);
        if (current != null && current.getAmount() == bonus) {
            return;
        }
        maxMana.removeModifier(LEVEL_UUID);
        maxMana.addTransientModifier(new AttributeModifier(LEVEL_UUID, "valor_items:mana_per_level", bonus,
                AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        if (!data.getBoolean(INIT_TAG)) {
            // the first time: the levels he already has count, and the mana starts full
            data.putInt(PEAK_TAG, player.experienceLevel);
            data.putBoolean(INIT_TAG, true);
            applyLevelBonus(player, player.experienceLevel);
            setMana(player, getMaxMana(player));
            return;
        }
        int peak = data.getInt(PEAK_TAG);
        if (player.experienceLevel > peak) {
            int gained = player.experienceLevel - peak;
            data.putInt(PEAK_TAG, player.experienceLevel);
            applyLevelBonus(player, player.experienceLevel);
            addMana(player, gained * MANA_PER_LEVEL); // the new mana is also there to use
        } else if (player.tickCount % 20 == 0) {
            applyLevelBonus(player, peak); // the modifier is not saved: it comes back after a relog
            setMana(player, getMana(player)); // never above the maximum
            if (player.isAlive() && !player.isSpectator()) {
                // mana_regen is per 5 seconds: a fifth of it every second
                addMana(player, player.getAttributeValue(ModAttributes.MANA_REGEN.get()) / 5.0D);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        var now = event.getEntity().getPersistentData();
        now.putInt(PEAK_TAG, old.getInt(PEAK_TAG));
        now.putBoolean(INIT_TAG, old.getBoolean(INIT_TAG));
        applyLevelBonus(event.getEntity(), old.getInt(PEAK_TAG));
        if (event.isWasDeath()) {
            setMana(event.getEntity(), getMaxMana(event.getEntity())); // back to full after dying
        } else {
            setMana(event.getEntity(), getMana(event.getOriginal()));
        }
    }

    private ManaEvents() {}
}
