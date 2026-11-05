package com.valor.items.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.valor.items.ValorItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While a player is immortal thanks to the Death Sickle, the body, the armor and the items in both hands are drawn
 * 75% transparent: everything the player draws goes through a buffer that swaps every render type for its translucent
 * version and multiplies the alpha of every vertex.
 */
@Mod.EventBusSubscriber(modid = ValorItems.MODID, value = Dist.CLIENT)
public final class GhostRender {
    /** Opacity while immortal: 0.25 = 75% transparent. */
    private static final float ALPHA = 0.25F;

    private static final Map<Integer, Long> GHOSTS = new ConcurrentHashMap<>();
    private static final Map<RenderType, RenderType> TRANSLUCENT = new ConcurrentHashMap<>();
    /** Render types that must stay as they are (glint, outlines, lines, unknown textures). */
    private static final java.util.Set<RenderType> UNCHANGED = ConcurrentHashMap.newKeySet();
    private static boolean rendering;

    public static void mark(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            GHOSTS.put(entityId, mc.level.getGameTime() + ticks);
        }
    }

    private static boolean isGhost(Player player) {
        Minecraft mc = Minecraft.getInstance();
        Long end = GHOSTS.get(player.getId());
        if (end == null || mc.level == null) {
            return false;
        }
        if (end <= mc.level.getGameTime()) {
            GHOSTS.remove(player.getId());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (rendering || !(event.getEntity() instanceof AbstractClientPlayer player) || !isGhost(player)) {
            return;
        }
        event.setCanceled(true);
        rendering = true;
        try {
            float yaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());
            event.getRenderer().render(player, yaw, event.getPartialTick(), event.getPoseStack(),
                    new GhostBufferSource(event.getMultiBufferSource()), event.getPackedLight());
        } finally {
            rendering = false;
        }
    }

    /** First person: the arms and the items in both hands. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (rendering || mc.player == null || !isGhost(mc.player) || !initHandRenderer()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return; // the first call below draws both hands
        }
        if (!(event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource real)) {
            return;
        }
        rendering = true;
        try {
            handMethod.invoke(handRenderer, event.getPartialTick(), event.getPoseStack(),
                    new GhostBufferSource(real), mc.player, event.getPackedLight());
        } catch (ReflectiveOperationException e) {
            ValorItems.LOGGER.warn("Could not draw the see-through hands", e);
        } finally {
            rendering = false;
        }
    }

    // ---- first person hand renderer, found by type so that no obfuscated name is needed ----

    private static Object handRenderer;
    private static Method handMethod;
    private static boolean handFailed;

    private static boolean initHandRenderer() {
        if (handFailed) {
            return false;
        }
        if (handRenderer != null && handMethod != null) {
            return true;
        }
        try {
            GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
            for (Field field : GameRenderer.class.getDeclaredFields()) {
                if (field.getType() == ItemInHandRenderer.class) {
                    field.setAccessible(true);
                    handRenderer = field.get(gameRenderer);
                }
            }
            for (Method method : ItemInHandRenderer.class.getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 5 && p[0] == float.class && p[1] == PoseStack.class
                        && p[2] == MultiBufferSource.BufferSource.class && p[3] == LocalPlayer.class
                        && p[4] == int.class) {
                    method.setAccessible(true);
                    handMethod = method;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            handRenderer = null;
        }
        if (handRenderer == null || handMethod == null) {
            handFailed = true;
            ValorItems.LOGGER.warn("Could not find the first person hand renderer: hands stay opaque");
            return false;
        }
        return true;
    }

    // ---- render type -> translucent render type of the same texture ----

    static RenderType translucent(RenderType type) {
        return TRANSLUCENT.computeIfAbsent(type, GhostRender::computeTranslucent);
    }

    private static RenderType computeTranslucent(RenderType type) {
        try {
            // toString() is "RenderType[<name>:CompositeState[...]]": only look at the name, the state text
            // mentions "outline" for every type
            String text = type.toString().toLowerCase();
            int start = text.indexOf('[') + 1;
            int end = text.indexOf(':', start);
            String name = end > start ? text.substring(start, end) : text;
            if (name.contains("glint") || name.contains("outline") || name.startsWith("line")) {
                UNCHANGED.add(type);
                return type;
            }
            ResourceLocation texture = textureOf(type);
            if (texture == null) {
                UNCHANGED.add(type);
                return type;
            }
            return RenderType.entityTranslucentCull(texture);
        } catch (Throwable t) {
            UNCHANGED.add(type);
            return type;
        }
    }

    /** RenderType -> CompositeState -> texture state shard -> texture, all found by type. */
    private static ResourceLocation textureOf(RenderType type) throws ReflectiveOperationException {
        Class<?> stateClass = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState");
        Class<?> textureShardClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard");
        Object state = null;
        for (Class<?> c = type.getClass(); c != null && state == null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() == stateClass) {
                    field.setAccessible(true);
                    state = field.get(type);
                    break;
                }
            }
        }
        if (state == null) {
            return null;
        }
        for (Field field : stateClass.getDeclaredFields()) {
            if (textureShardClass.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object shard = field.get(state);
                if (shard == null) {
                    return null;
                }
                for (Method method : textureShardClass.getDeclaredMethods()) {
                    if (method.getParameterCount() == 0 && method.getReturnType() == Optional.class) {
                        method.setAccessible(true);
                        Object result = method.invoke(shard);
                        if (result instanceof Optional<?> optional && optional.isPresent()
                                && optional.get() instanceof ResourceLocation location) {
                            return location;
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // ---- the see-through buffer ----

    private static final class GhostBufferSource extends MultiBufferSource.BufferSource {
        private final MultiBufferSource delegate;

        GhostBufferSource(MultiBufferSource delegate) {
            super(new com.mojang.blaze3d.vertex.BufferBuilder(256), java.util.Map.of());
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            RenderType translucent = translucent(type);
            VertexConsumer consumer = delegate.getBuffer(translucent);
            // types that are already translucent (items in hand) still need the alpha
            return UNCHANGED.contains(type) ? consumer : new AlphaConsumer(consumer);
        }

        @Override
        public void endBatch(RenderType type) {
            if (delegate instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(translucent(type));
            }
        }

        @Override
        public void endBatch() {
            // the real source is flushed by the game itself
        }
    }

    private static final class AlphaConsumer implements VertexConsumer {
        private final VertexConsumer inner;

        AlphaConsumer(VertexConsumer inner) {
            this.inner = inner;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            inner.color(r, g, b, Math.round(a * ALPHA));
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
            inner.defaultColor(r, g, b, Math.round(a * ALPHA));
        }

        @Override
        public void unsetDefaultColor() {
            inner.unsetDefaultColor();
        }
    }

    private GhostRender() {}
}
