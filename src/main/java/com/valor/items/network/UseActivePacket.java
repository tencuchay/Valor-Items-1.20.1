package com.valor.items.network;

import com.valor.items.event.HerculesEvents;
import com.valor.items.event.MailOfPainEvents;
import com.valor.items.item.HerculesMadnessItem;
import com.valor.items.item.MailOfPainItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: the player pressed the key of an active ability. {@code slot} is 0 for the first key and 1 for
 * the second; the worn actives are numbered in the order they are drawn on the screen (from the bottom up).
 */
public record UseActivePacket(int slot) {
    public static void encode(UseActivePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.slot);
    }

    public static UseActivePacket decode(FriendlyByteBuf buf) {
        return new UseActivePacket(buf.readVarInt());
    }

    public static void handle(UseActivePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            // the same relics, in the same order, as the icons of SickleHud (at most two)
            List<com.valor.items.relic.ActiveRelics.Kind> actives = com.valor.items.relic.ActiveRelics.worn(player);
            // a frozen player cannot use skills
            if (com.valor.items.event.ArcticOrbEvents.isFrozen(player)) {
                return;
            }
            if (p.slot >= 0 && p.slot < actives.size()) {
                switch (actives.get(p.slot)) {
                    case MAIL_OF_PAIN -> MailOfPainEvents.tryActivate(player);
                    case HERCULES_MADNESS -> HerculesEvents.tryActivate(player);
                    case ARCTIC_ORB -> com.valor.items.event.ArcticOrbEvents.tryActivate(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
