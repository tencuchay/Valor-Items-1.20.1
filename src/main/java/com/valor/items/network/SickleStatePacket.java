package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: what is left of the immortality and of the cooldown of the Death Sickle, in ticks. */
public record SickleStatePacket(int activeLeft, int activeTotal, int cooldownLeft, int cooldownTotal) {
    public static void encode(SickleStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.activeLeft);
        buf.writeVarInt(p.activeTotal);
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static SickleStatePacket decode(FriendlyByteBuf buf) {
        return new SickleStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SickleStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.update(p.activeLeft, p.activeTotal, p.cooldownLeft, p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
