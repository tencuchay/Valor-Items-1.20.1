package com.valor.items.client;

import com.valor.items.ValorItems;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.UseActivePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeybinds.USE_ACTIVE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new UseActivePacket(0));
            }
        }
        while (ModKeybinds.USE_ACTIVE_2.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new UseActivePacket(1));
            }
        }
        while (ModKeybinds.OPEN_RELIC.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientHooks.openEquippedRelicScreen();
            }
        }
    }

    private ClientForgeEvents() {}
}
