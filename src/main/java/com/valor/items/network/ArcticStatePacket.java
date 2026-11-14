package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: Impervious. Ticks of immobility left, ticks of immunity+shield left (of total), cooldown. */
public record ArcticStatePacket(int freezeLeft, int activeLeft, int activeTotal, int cooldownLeft, int cooldownTotal) {
    public static void encode(ArcticStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.freezeLeft);
        buf.writeVarInt(p.activeLeft);
        buf.writeVarInt(p.activeTotal);
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static ArcticStatePacket decode(FriendlyByteBuf buf) {
        return new ArcticStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt());
    }

    public static void handle(ArcticStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateArctic(p.freezeLeft, p.activeLeft, p.activeTotal, p.cooldownLeft,
                        p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
