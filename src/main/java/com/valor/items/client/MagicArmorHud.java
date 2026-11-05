package com.valor.items.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.valor.items.ValorItems;
import com.valor.items.registry.ModAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The magic armor bar: the icons of the armor bar of Minecraft, tinted #f394ff, right above the armor bar. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MagicArmorHud {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final float R = 0xF3 / 255.0F;
    private static final float G = 0x94 / 255.0F;
    private static final float B = 0xFF / 255.0F;

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), "magic_armor", MagicArmorHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        var attribute = mc.player.getAttribute(ModAttributes.MAGIC_ARMOR.get());
        int level = attribute == null ? 0 : (int) Math.floor(attribute.getValue());
        if (level <= 0) {
            return;
        }
        int left = width / 2 - 91;
        int top = height - gui.leftHeight;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(R, G, B, 1.0F);
        // only the armor that the player has: no empty icons
        for (int i = 1, slot = 0; slot < 10 && i <= level; i += 2, slot++) {
            int u = i < level ? 34 : 25; // full, or half for the last odd point
            g.blit(ICONS, left + slot * 8, top, u, 9, 9, 9);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        gui.leftHeight += 10;
    }

    private MagicArmorHud() {}
}
