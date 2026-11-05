package com.valor.items.network;

import com.valor.items.client.AdrenalineTint;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> every client that sees the player: he is drawn with a red tint for that many ticks (0 = no tint). */
public record AdrenalineAuraPacket(int entityId, int ticks) {
    public static void encode(AdrenalineAuraPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeVarInt(p.ticks);
    }

    public static AdrenalineAuraPacket decode(FriendlyByteBuf buf) {
        return new AdrenalineAuraPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(AdrenalineAuraPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AdrenalineTint.mark(p.entityId, p.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
