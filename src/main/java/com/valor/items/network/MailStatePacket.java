package com.valor.items.network;

import com.valor.items.client.SickleHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: what is left of Countersiege and of its cooldown, in ticks. */
public record MailStatePacket(int activeLeft, int activeTotal, int cooldownLeft, int cooldownTotal) {
    public static void encode(MailStatePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.activeLeft);
        buf.writeVarInt(p.activeTotal);
        buf.writeVarInt(p.cooldownLeft);
        buf.writeVarInt(p.cooldownTotal);
    }

    public static MailStatePacket decode(FriendlyByteBuf buf) {
        return new MailStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MailStatePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SickleHud.updateMail(p.activeLeft, p.activeTotal, p.cooldownLeft, p.cooldownTotal)));
        ctx.get().setPacketHandled(true);
    }
}
