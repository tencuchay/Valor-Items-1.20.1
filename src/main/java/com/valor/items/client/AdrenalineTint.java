package com.valor.items.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.valor.items.ValorItems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** While Adrenaline of Odin's Will is active the body and the armor of the player get a #e00f00 tint. */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class AdrenalineTint {
    private static final int TINT_R = 0xE0;
    private static final int TINT_G = 0x0F;
    private static final int TINT_B = 0x00;
    /** How much of the tint is mixed in (0 = nothing, 1 = flat colour). */
    private static final float STRENGTH = 0.5F;

    private static final Map<Integer, Long> ACTIVE = new ConcurrentHashMap<>();
    private static boolean rendering;
    /** True while something that must keep its own colours (the orbiting sickle) asks for its buffers. */
    static boolean suppress;

    public static void mark(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks <= 0 || mc.level == null) {
            ACTIVE.remove(entityId);
        } else {
            ACTIVE.put(entityId, mc.level.getGameTime() + ticks);
        }
    }

    private static boolean isTinted(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        Long end = ACTIVE.get(player.getId());
        if (end == null || mc.level == null) {
            return false;
        }
        if (end <= mc.level.getGameTime()) {
            ACTIVE.remove(player.getId());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (rendering || !(event.getEntity() instanceof AbstractClientPlayer player) || !isTinted(player)) {
            return;
        }
        event.setCanceled(true);
        rendering = true;
        try {
            float yaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());
            event.getRenderer().render(player, yaw, event.getPartialTick(), event.getPoseStack(),
                    new TintBufferSource(event.getMultiBufferSource()), event.getPackedLight());
        } finally {
            rendering = false;
        }
    }

    private static final class TintBufferSource extends MultiBufferSource.BufferSource {
        private final MultiBufferSource delegate;

        TintBufferSource(MultiBufferSource delegate) {
            super(new com.mojang.blaze3d.vertex.BufferBuilder(256), java.util.Map.of());
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            VertexConsumer consumer = delegate.getBuffer(type);
            if (suppress) {
                return consumer;
            }
            String text = type.toString().toLowerCase();
            int start = text.indexOf('[') + 1;
            int end = text.indexOf(':', start);
            String name = end > start ? text.substring(start, end) : text;
            if (name.contains("glint") || name.contains("outline") || name.startsWith("line")) {
                return consumer;
            }
            return new TintConsumer(consumer);
        }

        @Override
        public void endBatch(RenderType type) {
            if (delegate instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(type);
            }
        }

        @Override
        public void endBatch() {
            // the real source is flushed by the game itself
        }
    }

    private static final class TintConsumer implements VertexConsumer {
        private final VertexConsumer inner;

        TintConsumer(VertexConsumer inner) {
            this.inner = inner;
        }

        private static int mix(int value, int tint) {
            return Math.round(value * (1.0F - STRENGTH) + tint * STRENGTH);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            inner.color(mix(r, TINT_R), mix(g, TINT_G), mix(b, TINT_B), a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            inner.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            inner.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            inner.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            inner.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            inner.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            inner.defaultColor(mix(r, TINT_R), mix(g, TINT_G), mix(b, TINT_B), a);
        }

        @Override
        public void unsetDefaultColor() {
            inner.unsetDefaultColor();
        }
    }

    private AdrenalineTint() {}
}
