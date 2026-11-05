package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: strength (1 or 2 when doubled by a crit) of Wind's Blessing and the ticks they have left (out of total). */
public record WindStatePacket(int strength, int ticksLeft, int ticksTotal) {
    public static void encode(WindStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.strength);
        buf.writeVarInt(p.ticksLeft);
        buf.writeVarInt(p.ticksTotal);
    }

    public static WindStatePacket decode(FriendlyByteBuf buf) {
        return new WindStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(WindStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateWind(p.strength, p.ticksLeft, p.ticksTotal)));
        ctx.get().setPacketHandled(true);
    }
}
