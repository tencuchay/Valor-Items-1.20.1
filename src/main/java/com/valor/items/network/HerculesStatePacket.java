package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: the shield of Blood Madness (amount, ticks left of total) and its cooldown. */
public record HerculesStatePacket(float shield, int shieldLeft, int shieldTotal, int cooldownLeft, int cooldownTotal) {
    public static void encode(HerculesStatePacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.shield);
        buf.writeVarInt(p.shieldLeft);
        buf.writeVarInt(p.shieldTotal);
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static HerculesStatePacket decode(FriendlyByteBuf buf) {
        return new HerculesStatePacket(buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt());
    }

    public static void handle(HerculesStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateHercules(p.shield, p.shieldLeft, p.shieldTotal, p.cooldownLeft,
                        p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
