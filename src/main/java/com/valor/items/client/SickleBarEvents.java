package com.valor.items.client;

import com.valor.items.ValorItems;
import com.valor.items.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** While the Death Sickle makes the player immortal, the experience bar becomes a purple bar that runs out. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class SickleBarEvents {
    private static final int BAR_WIDTH = 182;

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        double left = SickleHud.activeTicksLeft(mc.level.getGameTime() + event.getPartialTick());
        if (left <= 0.0D) {
            return;
        }
        event.setCanceled(true);

        GuiGraphics g = event.getGuiGraphics();
        int x = event.getWindow().getGuiScaledWidth() / 2 - BAR_WIDTH / 2;
        int y = event.getWindow().getGuiScaledHeight() - 27;
        int filled = (int) Math.round(BAR_WIDTH * Math.min(1.0D, left / SickleHud.activeTotal()));

        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + 4, 0xFF000000);        // outline
        g.fill(x, y, x + BAR_WIDTH, y + 3, 0xFF2A1540);                   // empty part
        if (filled > 0) {
            g.fill(x, y, x + filled, y + 3, 0xFF9B4DEB);                  // purple, shrinking
            g.fill(x, y, x + filled, y + 1, 0xFFD3A5FF);                  // light top edge
        }
        // the sickle above the middle of the bar
        g.renderItem(new ItemStack(ModItems.DEATH_SICKLE.get()), x + BAR_WIDTH / 2 - 8, y - 18);
    }

    private SickleBarEvents() {}
}
