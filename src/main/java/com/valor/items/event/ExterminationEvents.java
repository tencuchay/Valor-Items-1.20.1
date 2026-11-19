package com.valor.items.event;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.SoaringAuraItem;
import com.valor.items.network.ExterminationStatePacket;
import com.valor.items.network.ModNetwork;
import com.valor.items.registry.ModAttributes;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Soaring Aura: "Extermination" (Diệt Thần).
 * Hitting an enemy with a normal attack or skill gives a stack (up to 10).
 * Each stack increases magic armor penetration (magic pierce). Lasts 5 seconds;
 * when the timer ends, one stack is lost and the timer restarts.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID)
public final class ExterminationEvents {
    private static final String STACKS_TAG = "ValorExterminationStacks";
    private static final String END_TAG = "ValorExterminationEnd";
    private static final String TOTAL_TAG = "ValorExterminationTotal";
    private static final UUID PIERCE_UUID = UUID.fromString("4a9e2f18-6c3d-4e5a-8b1f-7e0d3c5a9b24");

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        int level = SoaringAuraItem.getEquippedLevel(player);
        if (level <= 0) {
            return;
        }
        var data = player.getPersistentData();
        int stacks = data.getInt(STACKS_TAG);
        long now = player.level().getGameTime();
        stacks = Math.min(ValorConfig.exterminationMaxStacks(), stacks + 1);
        int duration = Math.max(1, (int) Math.round(ValorConfig.exterminationSeconds() * 20.0D));
        data.putInt(STACKS_TAG, stacks);
        data.putLong(END_TAG, now + duration);
        data.putInt(TOTAL_TAG, duration);
        applyModifier(player, stacks, level);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        var data = player.getPersistentData();
        int stacks = data.getInt(STACKS_TAG);
        if (stacks <= 0) {
            return;
        }
        int level = SoaringAuraItem.getEquippedLevel(player);
        if (level <= 0) {
            clear(player);
            return;
        }
        long now = player.level().getGameTime();
        if (data.getLong(END_TAG) <= now) {
            stacks--;
            if (stacks <= 0) {
                clear(player);
            } else {
                int duration = Math.max(1, (int) Math.round(ValorConfig.exterminationSeconds() * 20.0D));
                data.putInt(STACKS_TAG, stacks);
                data.putLong(END_TAG, now + duration);
                data.putInt(TOTAL_TAG, duration);
                applyModifier(player, stacks, level);
                sync(player);
            }
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

    private static void applyModifier(ServerPlayer player, int stacks, int level) {
        AttributeInstance pierceAttr = player.getAttribute(ModAttributes.MAGIC_PENETRATION.get());
        if (pierceAttr == null) {
            return;
        }
        double amount = stacks * ValorConfig.exterminationPierce(level);
        AttributeModifier current = pierceAttr.getModifier(PIERCE_UUID);
        if (current != null && Math.abs(current.getAmount() - amount) < 1.0E-6D) {
            return;
        }
        pierceAttr.removeModifier(PIERCE_UUID);
        if (amount > 0.0D) {
            pierceAttr.addTransientModifier(new AttributeModifier(PIERCE_UUID,
                    "valor_items:extermination_pierce", amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void clear(ServerPlayer player) {
        var data = player.getPersistentData();
        data.remove(STACKS_TAG);
        data.remove(END_TAG);
        data.remove(TOTAL_TAG);
        AttributeInstance pierceAttr = player.getAttribute(ModAttributes.MAGIC_PENETRATION.get());
        if (pierceAttr != null) {
            pierceAttr.removeModifier(PIERCE_UUID);
        }
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        var data = player.getPersistentData();
        int stacks = data.getInt(STACKS_TAG);
        long end = data.getLong(END_TAG);
        long now = player.level().getGameTime();
        int left = Math.max(0, (int) (end - now));
        int total = data.getInt(TOTAL_TAG);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ExterminationStatePacket(stacks, left, total));
    }

    private ExterminationEvents() {}
}
