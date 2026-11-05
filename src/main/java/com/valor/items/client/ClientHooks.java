package com.valor.items.client;

import com.valor.items.relic.RelicLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client only helpers. Always call through DistExecutor from common code. */
@OnlyIn(Dist.CLIENT)
public final class ClientHooks {

    public static void openRelicScreen(RelicLocation location) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && location != null) {
            minecraft.setScreen(new RelicScreen(location));
        }
    }

    public static void openRelicScreenForSlot(Slot slot, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        openRelicScreen(RelicLocation.fromSlot(player, slot, stack));
    }

    /** Keybind: open the UI of the first relic worn in a Curios slot. */
    public static void openEquippedRelicScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        RelicLocation location = RelicLocation.findFirstEquippedRelic(player);
        if (location == null) {
            player.displayClientMessage(Component.translatable("message.valor_items.no_relic_equipped"), true);
            return;
        }
        openRelicScreen(location);
    }

    private ClientHooks() {}
}
