package com.valor.items.network;

import com.valor.items.client.SickleOrbit;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> everybody who sees the player: the Death Sickle of this player is ready (circles around) or not. */
public record SickleOrbitPacket(int entityId, boolean show) {
    public static void encode(SickleOrbitPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeBoolean(p.show);
    }

    public static SickleOrbitPacket decode(FriendlyByteBuf buf) {
        return new SickleOrbitPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SickleOrbitPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleOrbit.set(p.entityId, p.show)));
        ctx.get().setPacketHandled(true);
    }
}
