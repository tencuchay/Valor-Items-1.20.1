package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.ZweihanderItem;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.RapidBladeStatePacket;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Zweihander: two unique passives.
 * <ul>
 *   <li>Rapid Blade (Cấp Nhận): every basic attack that hits gives a stack (up to 2) of attack speed; a hit restarts
 *   the 4 seconds, and when they run out all the stacks end.</li>
 *   <li>Twilight (Thánh Quang): a basic attack that hits deals additional magic damage (base + percent of the magic
 *   damage of the wearer), with a short cooldown. Magic armor applies; it never crits and does not count as a basic
 *   attack, so it cannot trigger the passives again.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ZweihanderEvents {
    private static final String STACKS_TAG = "ValorRapidStacks";
    private static final String END_TAG = "ValorRapidEnd";
    private static final String TOTAL_TAG = "ValorRapidTotal";
    private static final String TWILIGHT_CD_TAG = "ValorTwilightCooldownEnd";
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("b1d47e92-3a5c-4f68-9e20-8c6a1d5f7b03");

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !LifestealEvents.isBasicAttack(event.getSource(), player)) {
            return;
        }
        var target = event.getEntity();
        if (target == player || target instanceof ArmorStand || event.getAmount() <= 0.0F) {
            return;
        }
        int level = ZweihanderItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        var data = player.getPersistentData();

        // Rapid Blade
        int stacks = Math.min(ValorConfig.rapidMaxStacks(), data.getInt(STACKS_TAG) + 1);
        int duration = Math.max(1, (int) Math.round(ValorConfig.rapidSeconds() * 20.0D));
        data.putInt(STACKS_TAG, stacks);
        data.putLong(END_TAG, now + duration);
        data.putInt(TOTAL_TAG, duration);
        applyAttackSpeed(player, stacks);
        sync(player);

        // Twilight
        if (data.getLong(TWILIGHT_CD_TAG) > now) {
            return;
        }
        var magic = player.getAttribute(ModAttributes.MAGIC_DAMAGE.get());
        double damage = ValorConfig.twilightDamage(level)
                + (magic == null ? 0.0D : magic.getValue()) * ValorConfig.twilightMagicPercent() / 100.0D;
        if (damage <= 0.0D) {
            return;
        }
        data.putLong(TWILIGHT_CD_TAG,
                now + Math.max(0, (int) Math.round(ValorConfig.twilightCooldownSeconds() * 20.0D)));
        BlitzBladeEvents.dealMagicDamage(target, player, (float) damage, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        if (data.getInt(STACKS_TAG) <= 0) {
            return;
        }
        if (data.getLong(END_TAG) <= player.level().getGameTime() || ZweihanderItem.getEquippedLevel(player) <= 0) {
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

    private static void applyAttackSpeed(ServerPlayer player, int stacks) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        double amount = stacks * ValorConfig.rapidAttackSpeed();
        AttributeModifier current = attackSpeed.getModifier(ATTACK_SPEED_UUID);
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-6D) {
            return;
        }
        attackSpeed.removeModifier(ATTACK_SPEED_UUID);
        if (amount > 0.0D) {
            attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_UUID,
                    "valor_items:rapid_blade_attack_speed", amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void clear(ServerPlayer player) {
        var data = player.getPersistentData();
        data.remove(STACKS_TAG);
        data.remove(END_TAG);
        data.remove(TOTAL_TAG);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
        }
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        int left = Math.max(0, (int) (data.getLong(END_TAG) - player.level().getGameTime()));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RapidBladeStatePacket(data.getInt(STACKS_TAG), left, data.getInt(TOTAL_TAG)));
    }

    private ZweihanderEvents() {}
}
