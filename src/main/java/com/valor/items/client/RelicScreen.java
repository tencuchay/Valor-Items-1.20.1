package com.valor.items.client;

import com.valor.items.item.ValorItem;
import com.valor.items.network.ModNetwork;
import com.valor.items.network.UpgradeRelicPacket;
import com.valor.items.relic.RelicAbility;
import com.valor.items.relic.RelicLocation;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The UI of a relic: shows its abilities and lets the player spend experience levels to upgrade it.
 * The list of abilities scrolls with the mouse wheel when it does not fit.
 */
@OnlyIn(Dist.CLIENT)
public class RelicScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int MAX_HEIGHT = 270;
    private static final int PADDING = 12;
    /** Space used above the scrolling list: icon, name, level and pips. */
    private static final int HEADER_HEIGHT = 64;
    /** Space used below the scrolling list: cost line and button. */
    private static final int FOOTER_HEIGHT = 56;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLL_STEP = 14;

    private final RelicLocation location;
    private UpgradeButton upgradeButton;
    private int panelHeight = MAX_HEIGHT;
    private int scroll = 0;
    private int maxScroll = 0;

    public RelicScreen(RelicLocation location) {
        super(Component.translatable("gui.valor_items.relic_title"));
        this.location = location;
    }

    @Override
    protected void init() {
        this.panelHeight = Math.min(MAX_HEIGHT, this.height - 16);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - panelHeight) / 2;
        this.upgradeButton = this.addRenderableWidget(
                new UpgradeButton(left + WIDTH / 2 - 55, top + panelHeight - PADDING - 20, 110, 20,
                        Component.translatable("gui.valor_items.upgrade"), button ->
                                ModNetwork.CHANNEL.sendToServer(new UpgradeRelicPacket(location))));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta) * SCROLL_STEP, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        ItemStack stack = player == null ? ItemStack.EMPTY : location.get(player);
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ValorItem relic)) {
            this.onClose();
            return;
        }

        this.renderBackground(graphics);

        int left = (this.width - WIDTH) / 2;
        int top = (this.height - panelHeight) / 2;
        int level = relic.getLevel(stack);
        int max = relic.getMaxLevel();

        // Panel
        graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + panelHeight + 2, 0xFF000000);
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + panelHeight + 1, 0xFF6B4FBB);
        graphics.fill(left, top, left + WIDTH, top + panelHeight, 0xF0191426);

        // Icon, name, level
        graphics.pose().pushPose();
        graphics.pose().translate(left + PADDING, top + PADDING, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();

        graphics.drawString(this.font, stack.getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                left + PADDING + 40, top + PADDING + 2, 0xFFFFFFFF, true);
        graphics.drawString(this.font,
                Component.translatable("gui.valor_items.slot",
                        Component.translatable("curios.identifier." + relic.getSlotId())),
                left + PADDING + 40, top + PADDING + 14, 0xFFAAAAAA, false);
        graphics.drawString(this.font, Component.translatable("gui.valor_items.level", level, max),
                left + PADDING + 40, top + PADDING + 26, 0xFFFFAA00, false);

        // Level pips
        int pipY = top + PADDING + 40;
        for (int i = 0; i < max; i++) {
            int pipX = left + PADDING + i * 14;
            graphics.fill(pipX, pipY, pipX + 12, pipY + 4, i < level ? 0xFFFFC53D : 0xFF3A3450);
        }

        // Scrolling list of abilities, clipped to its own area
        int areaLeft = left + PADDING;
        int areaTop = top + HEADER_HEIGHT;
        int areaBottom = top + panelHeight - FOOTER_HEIGHT;
        int areaRight = left + WIDTH - PADDING;
        int textWidth = areaRight - areaLeft - SCROLLBAR_WIDTH - 4;

        graphics.enableScissor(areaLeft - 2, areaTop, areaRight + 2, areaBottom);
        int y = areaTop - scroll + 6; // a little air under the separator line
        for (Component stat : relic.getStatLines()) {
            graphics.drawString(this.font, stat, areaLeft, y, 0xFF9BE08B, false);
            y += 11;
        }
        if (!relic.getStatLines().isEmpty()) {
            y += 5;
        }
        for (RelicAbility ability : relic.getAbilities()) {
            ChatFormatting nameColor = ability.upgradable() ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
            graphics.drawString(this.font, ability.name().copy().withStyle(nameColor), areaLeft, y, 0xFFFFFFFF, false);
            y += 11;
            y = drawWrapped(graphics, ability.description().apply(level), areaLeft, y, textWidth, 0xFFCCCCCC);
            if (ability.upgradable() && level < max) {
                y = drawWrapped(graphics, Component.translatable("gui.valor_items.next_level"),
                        areaLeft, y + 2, textWidth, 0xFF6FD66F);
                for (Component change : ability.changes().apply(level)) {
                    y = drawWrapped(graphics, Component.literal("  ").append(change), areaLeft, y, textWidth, 0xFF6FD66F);
                }
            }
            y += 6;
        }
        graphics.disableScissor();

        // Content height -> how far we can scroll
        int contentHeight = y + scroll - areaTop;
        int viewHeight = areaBottom - areaTop;
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        if (maxScroll > 0) {
            int barX = areaRight - SCROLLBAR_WIDTH;
            int barHeight = Math.max(12, viewHeight * viewHeight / contentHeight);
            int barY = areaTop + (int) ((long) (viewHeight - barHeight) * scroll / maxScroll);
            graphics.fill(barX, areaTop, barX + SCROLLBAR_WIDTH, areaBottom, 0xFF2A2440);
            graphics.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0xFF8B6FE0);
        }

        // Separator lines above and below the list
        graphics.fill(areaLeft, areaTop - 1, areaRight, areaTop, 0xFF3A3450);
        graphics.fill(areaLeft, areaBottom, areaRight, areaBottom + 1, 0xFF3A3450);

        // Cost + button state
        boolean maxed = level >= max;
        boolean canAfford = true;
        Component costLine;
        if (maxed) {
            costLine = Component.translatable("gui.valor_items.max_level").withStyle(ChatFormatting.GOLD);
        } else {
            int cost = relic.getUpgradeCost(level);
            canAfford = player.getAbilities().instabuild || player.experienceLevel >= cost;
            costLine = Component.translatable("gui.valor_items.cost", cost)
                    .withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED);
        }
        graphics.drawCenteredString(this.font, costLine, left + WIDTH / 2, areaBottom + 8, 0xFFFFFFFF);
        this.upgradeButton.active = !maxed && canAfford;

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }
}
