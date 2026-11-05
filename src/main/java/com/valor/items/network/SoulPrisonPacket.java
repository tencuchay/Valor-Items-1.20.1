package com.valor.items.network;

import com.valor.items.client.SoulPrisonOverlay;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> every client that sees the entity: it has Soul Prison for that many ticks (mobs' effects are not synced). */
public record SoulPrisonPacket(int entityId, int ticks) {
    public static void encode(SoulPrisonPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeVarInt(p.ticks);
    }

    public static SoulPrisonPacket decode(FriendlyByteBuf buf) {
        return new SoulPrisonPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SoulPrisonPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SoulPrisonOverlay.mark(p.entityId, p.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
