package com.valor.items.network;

import com.valor.items.client.ThornLayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> every client that sees the player: Countersiege is active, so thorns grow around him for that long. */
public record MailAuraPacket(int entityId, int ticks) {
    public static void encode(MailAuraPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeVarInt(p.ticks);
    }

    public static MailAuraPacket decode(FriendlyByteBuf buf) {
        return new MailAuraPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MailAuraPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ThornLayer.mark(p.entityId, p.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
