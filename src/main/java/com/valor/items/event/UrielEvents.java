package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.UrielsBrandItem;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.UrielStatePacket;
import net.minecraftforge.network.PacketDistributor;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Uriel's Brand.
 * <ul>
 *   <li>"Devil's Bane": a part of the attack damage becomes magic armor, up to a maximum. It is one modifier with a
 *       fixed id, so wearing several Uriel's Brand never stacks it.</li>
 *   <li>"Magic Guardian": when the health falls below a part of the max health, a magic shield (valor_items:magic_shield:
 *       it stops magic damage only; true damage ignores it) and movement speed for a few seconds, then a cooldown.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class UrielEvents {
    private static final String COOLDOWN_END_TAG = "ValorUrielCooldownEnd";
    private static final String COOLDOWN_TOTAL_TAG = "ValorUrielCooldownTotal";
    private static final String GUARD_END_TAG = "ValorUrielGuardEnd";
    private static final UUID BANE_UUID = UUID.fromString("a3d5e7f1-4b62-4c98-8e0a-1f7b9d2c6a35");
    private static final UUID SPEED_UUID = UUID.fromString("6c2f8a04-d1e7-47b3-9a5c-3e8d0f4b7c19");

    private static float getShield(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(ModAttributes.MAGIC_SHIELD.get());
        return attribute == null ? 0.0F : (float) attribute.getBaseValue();
    }

    private static void setShield(ServerPlayer player, float value) {
        AttributeInstance attribute = player.getAttribute(ModAttributes.MAGIC_SHIELD.get());
        if (attribute != null) {
            attribute.setBaseValue(Math.max(0.0F, value));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        updateBane(player);
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        long guardEnd = data.getLong(GUARD_END_TAG);
        if (guardEnd != 0L) {
            if (now >= guardEnd || !UrielsBrandItem.isEquipped(player)) {
                endGuard(player);
            }
            return;
        }
        // Magic Guardian triggers when the health is below the threshold and the cooldown is over
        int level = UrielsBrandItem.getEquippedLevel(player);
        if (player.isAlive() && level > 0 && data.getLong(COOLDOWN_END_TAG) <= now
                && player.getHealth() < player.getMaxHealth() * ValorConfig.guardianThresholdPercent() / 100.0D) {
            int duration = Math.max(1, (int) Math.round(ValorConfig.guardianSeconds() * 20.0D));
            int cooldown = Math.max(duration, ValorConfig.guardianCooldownSeconds() * 20);
            data.putLong(GUARD_END_TAG, now + duration);
            data.putLong(COOLDOWN_END_TAG, now + cooldown);
            data.putInt(COOLDOWN_TOTAL_TAG, cooldown);
            setShield(player, (float) (player.getMaxHealth() * ValorConfig.guardianShieldPercent(level) / 100.0D));
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SPEED_UUID);
                speed.addTransientModifier(new AttributeModifier(SPEED_UUID, "valor_items:magic_guardian_speed",
                        ValorConfig.guardianSpeedPercent() / 100.0D, AttributeModifier.Operation.MULTIPLY_BASE));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.6F, 1.5F);
            sync(player);
        }
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        int left = (int) Math.max(0L, data.getLong(COOLDOWN_END_TAG) - player.level().getGameTime());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new UrielStatePacket(left, Math.max(1, data.getInt(COOLDOWN_TOTAL_TAG))));
    }

    private static void endGuard(ServerPlayer player) {
        player.getPersistentData().putLong(GUARD_END_TAG, 0L);
        setShield(player, 0.0F);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_UUID);
        }
    }

    /** Devil's Bane: magic armor from the attack damage (the whole value of the attribute, weapon included). */
    private static void updateBane(ServerPlayer player) {
        AttributeInstance magicArmor = player.getAttribute(ModAttributes.MAGIC_ARMOR.get());
        if (magicArmor == null) {
            return;
        }
        if (!UrielsBrandItem.isEquipped(player)) {
            magicArmor.removeModifier(BANE_UUID);
            return;
        }
        double amount = Math.min(ValorConfig.baneMaxMagicArmor(),
                player.getAttributeValue(Attributes.ATTACK_DAMAGE) * ValorConfig.baneConvertPercent() / 100.0D);
        AttributeModifier current = magicArmor.getModifier(BANE_UUID);
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-6D) {
            return;
        }
        magicArmor.removeModifier(BANE_UUID);
        magicArmor.addTransientModifier(new AttributeModifier(BANE_UUID, "valor_items:devils_bane", amount,
                AttributeModifier.Operation.ADDITION));
    }

    /**
     * The magic shield takes the magic damage (after the magic armor) before the normal shield (HIGH) does; what
     * is left goes on.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float shield = getShield(player);
        if (shield <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (!MagicArmorEvents.isMagic(source) || source.is(MailOfPainEvents.TRUE_DAMAGE)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        float absorbed = Math.min(shield, amount);
        setShield(player, shield - absorbed);
        event.setAmount(amount - absorbed);
        // for other mods (damage indicators): the same numbers as the normal shield
        var data = player.getPersistentData();
        long tick = player.level().getGameTime();
        float total = data.getLong(HerculesEvents.ABSORBED_TICK_TAG) == tick
                ? data.getFloat(HerculesEvents.ABSORBED_TAG) + absorbed : absorbed;
        data.putFloat(HerculesEvents.ABSORBED_TAG, total);
        data.putLong(HerculesEvents.ABSORBED_TICK_TAG, tick);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endGuard(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endGuard(player);
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

    private UrielEvents() {}
}
