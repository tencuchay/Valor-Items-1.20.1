package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: stacks of Flame Wraith and the ticks they have left (out of total). */
public record FlameStatePacket(int stacks, int ticksLeft, int ticksTotal) {
    public static void encode(FlameStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.stacks);
        buf.writeVarInt(p.ticksLeft);
        buf.writeVarInt(p.ticksTotal);
    }

    public static FlameStatePacket decode(FriendlyByteBuf buf) {
        return new FlameStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FlameStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateFlame(p.stacks, p.ticksLeft, p.ticksTotal)));
        ctx.get().setPacketHandled(true);
    }
}
