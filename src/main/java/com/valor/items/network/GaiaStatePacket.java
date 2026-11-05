package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: the ticks left of the cooldown of Restore (out of total). */
public record GaiaStatePacket(int cooldownLeft, int cooldownTotal) {
    public static void encode(GaiaStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static GaiaStatePacket decode(FriendlyByteBuf buf) {
        return new GaiaStatePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(GaiaStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateGaia(p.cooldownLeft, p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
