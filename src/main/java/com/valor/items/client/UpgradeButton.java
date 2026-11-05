package com.valor.items.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The flat golden button of the relic UI (#edc16f), drawn to match the panel instead of the vanilla texture. */
@OnlyIn(Dist.CLIENT)
public class UpgradeButton extends Button {
    private static final int GOLD = 0xFFEDC16F;
    private static final int GOLD_HOVER = 0xFFF6D68F;
    private static final int GOLD_LIGHT = 0xFFFAE3B0;
    private static final int GOLD_DARK = 0xFFB8903F;
    private static final int OUTLINE = 0xFF000000;
    private static final int TEXT = 0xFFFFFFFF;

    private static final int OFF_FILL = 0xFF2A2440;
    private static final int OFF_OUTLINE = 0xFF3A3450;
    private static final int OFF_TEXT = 0xFF7A7590;

    public UpgradeButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0 + this.getWidth();
        int y1 = y0 + this.getHeight();

        if (!this.active) {
            g.fill(x0, y0, x1, y1, OFF_OUTLINE);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, OFF_FILL);
        } else {
            g.fill(x0, y0, x1, y1, OUTLINE);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, this.isHoveredOrFocused() ? GOLD_HOVER : GOLD);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, GOLD_LIGHT);       // top highlight
            g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, GOLD_LIGHT);       // left highlight
            g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, GOLD_DARK);        // bottom shade
            g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, GOLD_DARK);        // right shade
        }

        int color = this.active ? TEXT : OFF_TEXT;
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        g.drawString(font, this.getMessage(), x0 + (this.getWidth() - font.width(this.getMessage())) / 2,
                y0 + (this.getHeight() - 8) / 2, color, this.active); // white text with a shadow
    }
}
