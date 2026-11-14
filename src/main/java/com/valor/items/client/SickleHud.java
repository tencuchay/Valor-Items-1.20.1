package com.valor.items.client;

import com.valor.items.ValorItems;
import com.valor.items.config.ValorConfig;
import com.valor.items.item.DeathSickleItem;
import com.valor.items.item.DevilsHandshakeItem;
import com.valor.items.item.GaiasStandardItem;
import com.valor.items.item.HerculesMadnessItem;
import com.valor.items.item.MailOfPainItem;
import com.valor.items.item.OdinsWillItem;
import com.valor.items.item.ArcticOrbItem;
import com.valor.items.item.StikksStingItem;
import com.valor.items.item.UrielsBrandItem;
import com.valor.items.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shows the Death Sickle and its cooldown on the screen. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SickleHud {
    private static long activeEnd;
    private static int activeTotal = 1;
    private static long cooldownEnd;
    private static int cooldownTotal = 1;

    public static void update(int activeLeft, int activeTotalTicks, int cooldownLeft, int cooldownTotalTicks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        activeEnd = now + activeLeft;
        activeTotal = Math.max(1, activeTotalTicks);
        cooldownEnd = now + cooldownLeft;
        cooldownTotal = Math.max(1, cooldownTotalTicks);
    }

    // ---- Mail of Pain: Countersiege ----
    private static long mailActiveEnd;
    private static long mailCooldownEnd;
    private static int mailCooldownTotal = 1;

    public static void updateMail(int activeLeft, int activeTotalTicks, int cooldownLeft, int cooldownTotalTicks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        mailActiveEnd = now + activeLeft;
        mailActiveTotal = Math.max(1, activeTotalTicks);
        mailCooldownEnd = now + cooldownLeft;
        mailCooldownTotal = Math.max(1, cooldownTotalTicks);
    }

    // ---- Gaia's Standard: Restore ----
    private static long gaiaCooldownEnd;
    private static int gaiaCooldownTotal = 1;

    public static void updateGaia(int cooldownLeft, int cooldownTotalTicks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        gaiaCooldownEnd = now + cooldownLeft;
        gaiaCooldownTotal = Math.max(1, cooldownTotalTicks);
    }

    // ---- Devil's Handshake: Flame Wraith ----
    private static int flameStacks;
    private static long flameEnd;
    private static int flameTotal = 1;

    public static void updateFlame(int stacks, int ticksLeft, int ticksTotal) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        flameStacks = stacks;
        flameEnd = now + ticksLeft;
        flameTotal = Math.max(1, ticksTotal);
    }

    // ---- Stikk's Sting: Wind's Blessing ----
    private static int windStrength;
    private static long windEnd;
    private static int windTotal = 1;

    public static void updateWind(int strength, int ticksLeft, int ticksTotal) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        windStrength = strength;
        windEnd = now + ticksLeft;
        windTotal = Math.max(1, ticksTotal);
    }

    // ---- Hercules' Madness: Blood Madness ----
    private static float herculesShield;
    private static long herculesShieldEnd;
    private static int herculesShieldTotal = 1;
    private static long herculesCooldownEnd;
    private static int herculesCooldownTotal = 1;

    public static void updateHercules(float shield, int shieldLeft, int shieldTotal, int cooldownLeft, int cooldownTotal) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        herculesShield = shield;
        herculesShieldEnd = now + shieldLeft;
        herculesShieldTotal = Math.max(1, shieldTotal);
        herculesCooldownEnd = now + cooldownLeft;
        herculesCooldownTotal = Math.max(1, cooldownTotal);
    }

    // ---- Arctic Orb: Impervious ----
    private static long arcticFreezeEnd;
    private static long arcticActiveEnd;
    private static int arcticActiveTotal = 1;
    private static long arcticCooldownEnd;
    private static int arcticCooldownTotal = 1;
    private static boolean[][] arcticMask;

    public static void updateArctic(int freezeLeft, int activeLeft, int activeTotalTicks, int cooldownLeft,
                                    int cooldownTotalTicks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        arcticFreezeEnd = now + freezeLeft;
        arcticActiveEnd = now + activeLeft;
        arcticActiveTotal = Math.max(1, activeTotalTicks);
        arcticCooldownEnd = now + cooldownLeft;
        arcticCooldownTotal = Math.max(1, cooldownTotalTicks);
    }

    /** True while the player is encased in the ice of Arctic Orb (cannot move or attack). */
    public static boolean isArcticFrozen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && arcticFreezeEnd > mc.level.getGameTime();
    }

    // ---- Uriel's Brand: Magic Guardian ----
    private static long urielCooldownEnd;
    private static int urielCooldownTotal = 1;

    public static void updateUriel(int cooldownLeft, int cooldownTotalTicks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        urielCooldownEnd = now + cooldownLeft;
        urielCooldownTotal = Math.max(1, cooldownTotalTicks);
    }

    // ---- Odin's Will: Adrenaline ----
    private static int adrenalineStacks;
    private static long adrenalineEnd;
    private static int adrenalineTotal = 1;

    public static void updateAdrenaline(int stacks, int ticksLeft, int ticksTotal) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        adrenalineStacks = stacks;
        adrenalineEnd = now + ticksLeft;
        adrenalineTotal = Math.max(1, ticksTotal);
    }

    private static final ResourceLocation FRAME = new ResourceLocation(ValorItems.MODID, "textures/gui/hud_frame.png");
    private static final int FRAME_SIZE = 22;

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("death_sickle_cooldown", (gui, graphics, partialTick, width, height) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.options.hideGui || !ValorConfig.showSickleHud()) {
                return;
            }
            long now = mc.level.getGameTime();
            // passives: one row starting at the left end of the hotbar, just above the hotbar (4 px gap)
            boolean showSickle = DeathSickleItem.getEquippedLevel(mc.player) > 0;
            boolean showGaia = gaiaCooldownEnd > now && GaiasStandardItem.getEquippedLevel(mc.player) > 0;
            boolean showAdr = adrenalineStacks > 0 && adrenalineEnd > now && OdinsWillItem.getEquippedLevel(mc.player) > 0;
            boolean showFlame = flameStacks > 0 && flameEnd > now && DevilsHandshakeItem.getEquippedLevel(mc.player) > 0;
            boolean showWind = windStrength > 0 && windEnd > now && StikksStingItem.getEquippedLevel(mc.player) > 0;
            boolean showUriel = urielCooldownEnd > now && UrielsBrandItem.getEquippedLevel(mc.player) > 0;
            int count = (showSickle ? 1 : 0) + (showGaia ? 1 : 0) + (showAdr ? 1 : 0) + (showFlame ? 1 : 0)
                    + (showWind ? 1 : 0) + (showUriel ? 1 : 0);
            int step = ICON_SIZE + 4;
            int baseX = width / 2 - 91 + ValorConfig.hudOffsetX();
            int passiveY = height - 22 - 4 - ICON_SIZE - ValorConfig.hudOffsetY();
            int slot = 0;
            if (showSickle) {
                draw(graphics, mc.font, baseX + slot * step, passiveY, now);
                slot++;
            }
            if (showGaia) {
                drawGaia(graphics, baseX + slot * step, passiveY, now);
                slot++;
            }
            if (showAdr) {
                drawAdrenaline(graphics, mc.font, baseX + slot * step, passiveY, now, partialTick);
                slot++;
            }
            if (showFlame) {
                drawFlame(graphics, mc.font, baseX + slot * step, passiveY, now, partialTick);
                slot++;
            }
            if (showWind) {
                drawWind(graphics, mc.font, baseX + slot * step, passiveY, now, partialTick);
                slot++;
            }
            if (showUriel) {
                drawUriel(graphics, baseX + slot * step, passiveY, now);
            }
            // active: near the bottom right corner of the screen, so that it is easy to tell from the passive
            int activeSlot = 0;
            for (var kind : com.valor.items.relic.ActiveRelics.worn(mc.player)) {
                int x = width - FRAME_SIZE - ValorConfig.activeHudOffsetX();
                int y = height - FRAME_SIZE - ValorConfig.activeHudOffsetY() - activeSlot * (FRAME_SIZE + 4);
                switch (kind) {
                    case MAIL_OF_PAIN -> drawMail(graphics, x, y, now);
                    case HERCULES_MADNESS -> drawHercules(graphics, mc.font, x, y, now);
                    case ARCTIC_ORB -> drawArctic(graphics, x, y, now);
                }
                activeSlot++;
            }
        });
    }

    /** Remaining ticks of the immortality (0 when not active), counted with the partial tick. */
    static double activeTicksLeft(double now) {
        return Math.max(0.0D, activeEnd - now);
    }

    static int activeTotal() {
        return activeTotal;
    }

    /** Size of the icon on the screen, in pixels (the item itself is 16). */
    private static final int ICON_SIZE = 12;

    private static long mailActiveTotal = 1;

    /** Which pixels of the Mail of Pain icon are not transparent (the cooldown only darkens those). */
    private static boolean[][] mailMask;

    private static boolean[][] herculesMask;

    private static boolean[][] mailMask() {
        if (mailMask == null) {
            mailMask = loadMask("mail_of_pain");
        }
        return mailMask;
    }

    private static boolean[][] herculesMask() {
        if (herculesMask == null) {
            herculesMask = loadMask("hercules_madness");
        }
        return herculesMask;
    }

    private static boolean[][] loadMask(String item) {
        boolean[][] mask = new boolean[16][16];
        for (boolean[] row : mask) {
            java.util.Arrays.fill(row, true);
        }
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(
                    new ResourceLocation(ValorItems.MODID, "textures/item/" + item + ".png"));
            if (resource.isPresent()) {
                try (var stream = resource.get().open(); var image = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                    for (int px = 0; px < 16 && px < image.getWidth(); px++) {
                        for (int py = 0; py < 16 && py < image.getHeight(); py++) {
                            mask[px][py] = ((image.getPixelRGBA(px, py) >>> 24) & 0xFF) > 0;
                        }
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            // keep the full mask
        }
        return mask;
    }

    /** The active ability of the Mail of Pain: the frame, the icon, the running border and the cooldown. */
    private static void drawMail(GuiGraphics g, int x, int y, long now) {
        g.blit(FRAME, x, y, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        int ix = x + (FRAME_SIZE - 16) / 2;
        int iy = y + (FRAME_SIZE - 16) / 2;
        g.renderItem(new ItemStack(ModItems.MAIL_OF_PAIN.get()), ix, iy);

        long cooldownLeft = mailCooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / mailCooldownTotal;
            double c = 7.5D;
            boolean[][] mask = mailMask();
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            for (int px = 0; px < 16; px++) {
                for (int py = 0; py < 16; py++) {
                    if (!mask[px][py]) {
                        continue;
                    }
                    double angle = Math.atan2(px - c, -(py - c));
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(ix + px, iy + py, ix + px + 1, iy + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }

        // while Countersiege lasts, a gold line around the frame is eaten clockwise from the top
        long activeLeft = mailActiveEnd - now;
        if (activeLeft > 0) {
            int ring = FRAME_SIZE + 2;
            int perimeter = 4 * ring - 4;
            double elapsed = 1.0D - (double) activeLeft / mailActiveTotal;
            int start = (int) Math.round(elapsed * perimeter);
            for (int i = start; i < perimeter; i++) {
                int k = (i + ring / 2) % perimeter; // the line starts at the middle of the top edge
                int rx;
                int ry;
                if (k < ring) {                        // top, left to right
                    rx = k;
                    ry = 0;
                } else if (k < 2 * ring - 1) {         // right, going down
                    rx = ring - 1;
                    ry = k - ring + 1;
                } else if (k < 3 * ring - 2) {         // bottom, right to left
                    rx = ring - 1 - (k - (2 * ring - 2));
                    ry = ring - 1;
                } else {                               // left, going up
                    rx = 0;
                    ry = ring - 1 - (k - (3 * ring - 3));
                }
                g.fill(x - 1 + rx, y - 1 + ry, x + rx, y + ry, 0xFFF2C14E);
            }
        }
    }

    /** Blood Madness: the same frame as the Mail of Pain, the cooldown darkens it clockwise; while the shield lasts a line runs around it. */
    private static void drawHercules(GuiGraphics g, Font font, int x, int y, long now) {
        g.blit(FRAME, x, y, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        int ix = x + (FRAME_SIZE - 16) / 2;
        int iy = y + (FRAME_SIZE - 16) / 2;
        g.renderItem(new ItemStack(ModItems.HERCULES_MADNESS.get()), ix, iy);

        long cooldownLeft = herculesCooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / herculesCooldownTotal;
            double c = 7.5D;
            boolean[][] mask = herculesMask();
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            for (int px = 0; px < 16; px++) {
                for (int py = 0; py < 16; py++) {
                    if (!mask[px][py]) {
                        continue;
                    }
                    double angle = Math.atan2(px - c, -(py - c));
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(ix + px, iy + py, ix + px + 1, iy + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }

        long shieldLeft = herculesShieldEnd - now;
        if (shieldLeft > 0 && herculesShield > 0.0F) {
            int ring = FRAME_SIZE + 2;
            int perimeter = 4 * ring - 4;
            double elapsed = 1.0D - (double) shieldLeft / herculesShieldTotal;
            int start = (int) Math.round(elapsed * perimeter);
            for (int i = start; i < perimeter; i++) {
                int k = (i + ring / 2) % perimeter;
                int rx;
                int ry;
                if (k < ring) {
                    rx = k;
                    ry = 0;
                } else if (k < 2 * ring - 1) {
                    rx = ring - 1;
                    ry = k - ring + 1;
                } else if (k < 3 * ring - 2) {
                    rx = ring - 1 - (k - (2 * ring - 2));
                    ry = ring - 1;
                } else {
                    rx = 0;
                    ry = ring - 1 - (k - (3 * ring - 3));
                }
                g.fill(x - 1 + rx, y - 1 + ry, x + rx, y + ry, 0xFFE0453C);
            }
        }
    }

    /** Impervious: the same frame as the other actives; the cooldown darkens it clockwise, an ice-blue line runs while it lasts. */
    private static void drawArctic(GuiGraphics g, int x, int y, long now) {
        g.blit(FRAME, x, y, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        int ix = x + (FRAME_SIZE - 16) / 2;
        int iy = y + (FRAME_SIZE - 16) / 2;
        g.renderItem(new ItemStack(ModItems.ARCTIC_ORB.get()), ix, iy);
        if (arcticMask == null) {
            arcticMask = loadMask("arctic_orb");
        }

        long cooldownLeft = arcticCooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / arcticCooldownTotal;
            double c = 7.5D;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            for (int px = 0; px < 16; px++) {
                for (int py = 0; py < 16; py++) {
                    if (!arcticMask[px][py]) {
                        continue;
                    }
                    double angle = Math.atan2(px - c, -(py - c));
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(ix + px, iy + py, ix + px + 1, iy + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }

        long activeLeft = arcticActiveEnd - now;
        if (activeLeft > 0) {
            int ring = FRAME_SIZE + 2;
            int perimeter = 4 * ring - 4;
            double elapsed = 1.0D - (double) activeLeft / arcticActiveTotal;
            int start = (int) Math.round(elapsed * perimeter);
            for (int i = start; i < perimeter; i++) {
                int k = (i + ring / 2) % perimeter;
                int rx;
                int ry;
                if (k < ring) {
                    rx = k;
                    ry = 0;
                } else if (k < 2 * ring - 1) {
                    rx = ring - 1;
                    ry = k - ring + 1;
                } else if (k < 3 * ring - 2) {
                    rx = ring - 1 - (k - (2 * ring - 2));
                    ry = ring - 1;
                } else {
                    rx = 0;
                    ry = ring - 1 - (k - (3 * ring - 3));
                }
                g.fill(x - 1 + rx, y - 1 + ry, x + rx, y + ry, 0xFF7FD6F2);
            }
        }
    }

    /** Magic Guardian: purple plate; shown only while the cooldown runs: dark at first, lighting up clockwise. */
    private static void drawUriel(GuiGraphics g, int x, int y, long now) {
        g.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xFF3B0A30);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float scale = ICON_SIZE / 16.0F;
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(new ItemStack(ModItems.URIELS_BRAND.get()), 0, 0);
        g.pose().popPose();

        long cooldownLeft = urielCooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / urielCooldownTotal;
            double c = (ICON_SIZE - 1) / 2.0D;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            for (int px = 0; px < ICON_SIZE; px++) {
                for (int py = 0; py < ICON_SIZE; py++) {
                    double angle = Math.atan2(px - c, -(py - c));
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }
    }

    /** Restore: green plate; shown only while the cooldown runs: dark at first, lighting up clockwise until it is over. */
    private static void drawGaia(GuiGraphics g, int x, int y, long now) {
        g.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xFF014526);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float scale = ICON_SIZE / 16.0F;
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(new ItemStack(ModItems.GAIAS_STANDARD.get()), 0, 0);
        g.pose().popPose();

        long cooldownLeft = gaiaCooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / gaiaCooldownTotal;
            double c = (ICON_SIZE - 1) / 2.0D;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            for (int px = 0; px < ICON_SIZE; px++) {
                for (int py = 0; py < ICON_SIZE; py++) {
                    double angle = Math.atan2(px - c, -(py - c));
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }
    }

    /** Icon with the number of stacks; it darkens clockwise while the time runs out and disappears at the end. */
    private static void drawAdrenaline(GuiGraphics g, Font font, int x, int y, long now, float partialTick) {
        drawStacks(g, font, x, y, now, partialTick, new ItemStack(ModItems.ODINS_WILL.get()), 0xFF520500,
                adrenalineStacks, adrenalineEnd, adrenalineTotal);
    }

    /** Devil's Handshake: the same, with its own colour. */
    private static void drawFlame(GuiGraphics g, Font font, int x, int y, long now, float partialTick) {
        drawStacks(g, font, x, y, now, partialTick, new ItemStack(ModItems.DEVILS_HANDSHAKE.get()), 0xFF331A07,
                flameStacks, flameEnd, flameTotal);
    }

    /** Stikk's Sting: the number shows 2 while a critical hit doubles the effect. */
    private static void drawWind(GuiGraphics g, Font font, int x, int y, long now, float partialTick) {
        drawStacks(g, font, x, y, now, partialTick, new ItemStack(ModItems.STIKKS_STING.get()), 0xFF4F0101,
                windStrength, windEnd, windTotal);
    }

    private static void drawStacks(GuiGraphics g, Font font, int x, int y, long now, float partialTick,
                                   ItemStack icon, int plateColor, int stacks, long end, int total) {
        g.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, plateColor);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float scale = ICON_SIZE / 16.0F;
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();

        double left = Math.max(0.0D, end - now - partialTick);
        double progress = 1.0D - left / total; // 0 = just refreshed, 1 = gone
        double c = (ICON_SIZE - 1) / 2.0D;
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        for (int px = 0; px < ICON_SIZE; px++) {
            for (int py = 0; py < ICON_SIZE; py++) {
                double angle = Math.atan2(px - c, -(py - c));
                if (angle < 0) {
                    angle += Math.PI * 2;
                }
                if (angle / (Math.PI * 2) < progress) {
                    g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xC00A0A18);
                }
            }
        }
        g.pose().popPose();

        if (stacks > 1) {
            String text = String.valueOf(stacks);
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 400.0F);
            g.drawString(font, text, x + ICON_SIZE - font.width(text) + 1, y + ICON_SIZE - 7, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
    }

    private static void draw(GuiGraphics g, Font font, int x, int y, long now) {
        // square purple plate
        g.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xFF431352);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float scale = ICON_SIZE / 16.0F;
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(new ItemStack(ModItems.DEATH_SICKLE.get()), 0, 0);
        g.pose().popPose();

        // the cooldown starts the moment the passive triggers: the icon goes dark and lights up clockwise
        long cooldownLeft = cooldownEnd - now;
        if (cooldownLeft > 0) {
            double progress = 1.0D - (double) cooldownLeft / cooldownTotal;
            double c = (ICON_SIZE - 1) / 2.0D;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F); // items are drawn high up in depth: the overlay must be above them
            for (int px = 0; px < ICON_SIZE; px++) {
                for (int py = 0; py < ICON_SIZE; py++) {
                    double angle = Math.atan2(px - c, -(py - c)); // 0 = up, clockwise
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle / (Math.PI * 2) >= progress) {
                        g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xC00A0A18);
                    }
                }
            }
            g.pose().popPose();
        }
    }

    private SickleHud() {}
}
