package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: the cooldown of Magic Guardian, in ticks (left, total). */
public record UrielStatePacket(int cooldownLeft, int cooldownTotal) {
    public static void encode(UrielStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static UrielStatePacket decode(FriendlyByteBuf buf) {
        return new UrielStatePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(UrielStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateUriel(p.cooldownLeft, p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
