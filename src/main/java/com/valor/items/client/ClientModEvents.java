package com.valor.items.client;

import com.valor.items.ValorItems;
import net.minecraftforge.api.distmarker.Dist;
import com.valor.items.registry.ModParticles;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ValorItems.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.OPEN_RELIC);
        event.register(ModKeybinds.USE_ACTIVE);
        event.register(ModKeybinds.USE_ACTIVE_2);
    }

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerBelow(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "arctic_freeze",
                (gui, graphics, partialTick, width, height) -> ArcticClientEvents.renderFreezeOverlay(graphics, width, height, partialTick));
    }

    @SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.valor.items.registry.ModEntities.ARCTIC_ICE.get(), ArcticIceRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CHAIN_LIGHTNING.get(), ChainLightningParticle.Provider::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_SPARK.get(), LightningSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RA_FIRE.get(), sprites -> new RaFlameParticle.Provider(sprites, 0.22F));
        event.registerSpriteSet(ModParticles.RA_FLAME.get(), sprites -> new RaFlameParticle.Provider(sprites, 0.34F));
    }

    private ClientModEvents() {}
}
