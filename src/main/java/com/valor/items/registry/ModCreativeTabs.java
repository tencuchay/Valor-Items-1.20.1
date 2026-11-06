package com.valor.items.registry;

import com.valor.items.ValorItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ValorItems.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ValorItems.MODID))
                    .icon(() -> new ItemStack(ModItems.BLITZ_BLADE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BLITZ_BLADE.get());
                        output.accept(ModItems.DEATH_SICKLE.get());
                        output.accept(ModItems.MAIL_OF_PAIN.get());
                        output.accept(ModItems.MANTLE_OF_RA.get());
                        output.accept(ModItems.ODINS_WILL.get());
                        output.accept(ModItems.AMULET_OF_LONGEVITY.get());
                        output.accept(ModItems.GAIAS_STANDARD.get());
                        output.accept(ModItems.DEVILS_HANDSHAKE.get());
                        output.accept(ModItems.FENRIRS_FANG.get());
                        output.accept(ModItems.CLAVES_SANCTI.get());
                        output.accept(ModItems.STIKKS_STING.get());
                        output.accept(ModItems.HERCULES_MADNESS.get());
                        output.accept(ModItems.FAFNIRS_TALON.get());
                        output.accept(ModItems.URIELS_BRAND.get());
                        output.accept(ModItems.RANKBREAKER.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
