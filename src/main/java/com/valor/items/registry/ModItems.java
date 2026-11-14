package com.valor.items.registry;

import com.valor.items.ValorItems;
import com.valor.items.item.AmuletOfLongevityItem;
import com.valor.items.item.ArcticOrbItem;
import com.valor.items.item.BlitzBladeItem;
import com.valor.items.item.ClavesSanctiItem;
import com.valor.items.item.DeathSickleItem;
import com.valor.items.item.DevilsHandshakeItem;
import com.valor.items.item.FafnirsTalonItem;
import com.valor.items.item.FenrirsFangItem;
import com.valor.items.item.GaiasStandardItem;
import com.valor.items.item.HerculesMadnessItem;
import com.valor.items.item.MailOfPainItem;
import com.valor.items.item.MantleOfRaItem;
import com.valor.items.item.OdinsWillItem;
import com.valor.items.item.RankbreakerItem;
import com.valor.items.item.UrielsBrandItem;
import com.valor.items.item.StikksStingItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ValorItems.MODID);

    public static final RegistryObject<Item> BLITZ_BLADE = ITEMS.register("blitz_blade",
            () -> new BlitzBladeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> DEATH_SICKLE = ITEMS.register("death_sickle",
            () -> new DeathSickleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MAIL_OF_PAIN = ITEMS.register("mail_of_pain",
            () -> new MailOfPainItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MANTLE_OF_RA = ITEMS.register("mantle_of_ra",
            () -> new MantleOfRaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ODINS_WILL = ITEMS.register("odins_will",
            () -> new OdinsWillItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> AMULET_OF_LONGEVITY = ITEMS.register("amulet_of_longevity",
            () -> new AmuletOfLongevityItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> GAIAS_STANDARD = ITEMS.register("gaias_standard",
            () -> new GaiasStandardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> DEVILS_HANDSHAKE = ITEMS.register("devils_handshake",
            () -> new DevilsHandshakeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> FENRIRS_FANG = ITEMS.register("fenrirs_fang",
            () -> new FenrirsFangItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> CLAVES_SANCTI = ITEMS.register("claves_sancti",
            () -> new ClavesSanctiItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> STIKKS_STING = ITEMS.register("stikks_sting",
            () -> new StikksStingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> HERCULES_MADNESS = ITEMS.register("hercules_madness",
            () -> new HerculesMadnessItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> FAFNIRS_TALON = ITEMS.register("fafnirs_talon",
            () -> new FafnirsTalonItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> URIELS_BRAND = ITEMS.register("uriels_brand",
            () -> new UrielsBrandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> RANKBREAKER = ITEMS.register("rankbreaker",
            () -> new RankbreakerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> ARCTIC_ORB = ITEMS.register("arctic_orb",
            () -> new ArcticOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {}
}
