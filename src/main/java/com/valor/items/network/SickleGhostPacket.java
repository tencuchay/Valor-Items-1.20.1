package com.valor.items.network;

import com.valor.items.client.GhostRender;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> every client that sees the player: this player is immortal (see-through) for that many ticks. */
public record SickleGhostPacket(int entityId, int ticks) {
    public static void encode(SickleGhostPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeVarInt(p.ticks);
    }

    public static SickleGhostPacket decode(FriendlyByteBuf buf) {
        return new SickleGhostPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SickleGhostPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GhostRender.mark(p.entityId, p.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
