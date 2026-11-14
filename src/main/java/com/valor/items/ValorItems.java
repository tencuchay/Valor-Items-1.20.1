package com.valor.items;

import com.mojang.logging.LogUtils;
import com.valor.items.config.ValorConfig;
import com.valor.items.network.ModNetwork;
import com.valor.items.registry.ModAttributes;
import com.valor.items.registry.ModCreativeTabs;
import com.valor.items.registry.ModEffects;
import com.valor.items.registry.ModItems;
import com.valor.items.registry.ModParticles;
import com.valor.items.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ValorItems.MODID)
public class ValorItems {
    public static final String MODID = "valor_items";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ValorItems() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        com.valor.items.registry.ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModAttributes.ATTRIBUTES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ValorConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ValorConfig.CLIENT_SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
