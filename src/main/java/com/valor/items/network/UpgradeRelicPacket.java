package com.valor.items.network;

import com.valor.items.item.ValorItem;
import com.valor.items.relic.RelicLocation;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server: "upgrade the relic at this location". The server checks everything. */
public final class UpgradeRelicPacket {
    private final RelicLocation location;

    public UpgradeRelicPacket(RelicLocation location) {
        this.location = location;
    }

    public static void encode(UpgradeRelicPacket packet, FriendlyByteBuf buf) {
        packet.location.write(buf);
    }

    public static UpgradeRelicPacket decode(FriendlyByteBuf buf) {
        return new UpgradeRelicPacket(RelicLocation.read(buf));
    }

    public static void handle(UpgradeRelicPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.apply(player);
            }
        });
        context.setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        ItemStack stack = location.get(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof ValorItem relic)) {
            return;
        }
        int level = relic.getLevel(stack);
        if (level >= relic.getMaxLevel()) {
            return;
        }
        int cost = relic.getUpgradeCost(level);
        boolean free = player.getAbilities().instabuild;
        if (!free) {
            if (player.experienceLevel < cost) {
                player.displayClientMessage(Component.translatable("message.valor_items.not_enough_xp", cost), true);
                return;
            }
            player.giveExperienceLevels(-cost);
        }

        // Write a modified copy back so Curios notices the change and syncs it to the client.
        ItemStack upgraded = stack.copy();
        relic.setLevel(upgraded, level + 1);
        location.set(player, upgraded);

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                0.8F, 1.2F);
        player.displayClientMessage(
                Component.translatable("message.valor_items.upgraded", upgraded.getHoverName(), level + 1), true);
    }
}
