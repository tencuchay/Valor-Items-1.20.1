package com.valor.items.client;

import com.valor.items.ValorItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** While the player is encased in ice (Arctic Orb) the movement keys and the attack / use keys do nothing. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class ArcticClientEvents {

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (SickleHud.isArcticFrozen()) {
            var input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (SickleHud.isArcticFrozen()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static boolean wasFrozen;
    private static float lockedYaw;
    private static float lockedPitch;
    /** Game time at which the freeze began on this client (for the fade-in of the cold overlay). */
    private static long freezeStart;
    /** The cold overlay needs this many ticks to reach full strength (fast, but not at once). */
    private static final float FADE_TICKS = 10.0F;
    /** When the freeze ended on this client and how strong the overlay was then (for the fade-out). */
    private static long freezeEndTime = -1000L;
    private static float lastAlpha;

    /** Keeps the view where it was when the ice appeared (the mouse does nothing). */
    private static void lockLook(Minecraft mc) {
        var player = mc.player;
        if (player == null) {
            wasFrozen = false;
            return;
        }
        boolean frozen = SickleHud.isArcticFrozen();
        if (frozen && !wasFrozen) {
            lockedYaw = player.getYRot();
            lockedPitch = player.getXRot();
            freezeStart = mc.level == null ? 0L : mc.level.getGameTime();
        }
        if (wasFrozen && !frozen) {
            freezeEndTime = mc.level == null ? 0L : mc.level.getGameTime();
        }
        wasFrozen = frozen;
        if (frozen) {
            player.setYRot(lockedYaw);
            player.setXRot(lockedPitch);
            player.yRotO = lockedYaw;
            player.xRotO = lockedPitch;
            player.yHeadRot = lockedYaw;
            player.yHeadRotO = lockedYaw;
            player.yBodyRot = lockedYaw;
            player.yBodyRotO = lockedYaw;
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        lockLook(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderTick(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
        lockLook(Minecraft.getInstance());
    }

    /** The camera itself is held too, whatever the mouse did since the last tick. */
    @SubscribeEvent
    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (wasFrozen && SickleHud.isArcticFrozen()) {
            boolean mirrored = Minecraft.getInstance().options.getCameraType().isMirrored();
            event.setYaw(lockedYaw + (mirrored ? 180.0F : 0.0F));
            event.setPitch(mirrored ? -lockedPitch : lockedPitch);
        }
    }

    @SubscribeEvent
    public static void onUseItem(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide && SickleHud.isArcticFrozen()
                && event.getEntity() == Minecraft.getInstance().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackClient(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide && SickleHud.isArcticFrozen()) {
            event.setCanceled(true);
        }
    }

    private static final net.minecraft.resources.ResourceLocation FREEZE_OVERLAY =
            new net.minecraft.resources.ResourceLocation("textures/misc/powder_snow_outline.png");

    /** The cold (powder snow) overlay of Minecraft, in the first person view while the player is frozen. */
    public static void renderFreezeOverlay(net.minecraft.client.gui.GuiGraphics graphics, int width, int height,
                                           float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float time = mc.level.getGameTime() + partialTick;
        float alpha;
        if (SickleHud.isArcticFrozen()) {
            // fades in
            alpha = Math.min(1.0F, Math.max(0.0F, (time - freezeStart) / FADE_TICKS));
            lastAlpha = alpha;
        } else {
            // fades out from where it was
            alpha = lastAlpha * (1.0F - (time - freezeEndTime) / FADE_TICKS);
            if (alpha <= 0.0F) {
                return;
            }
        }
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(FREEZE_OVERLAY, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private ArcticClientEvents() {}
}
