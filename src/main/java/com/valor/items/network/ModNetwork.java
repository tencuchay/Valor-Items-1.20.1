package com.valor.items.network;

import com.valor.items.ValorItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ValorItems.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, UpgradeRelicPacket.class,
                UpgradeRelicPacket::encode, UpgradeRelicPacket::decode, UpgradeRelicPacket::handle);
        CHANNEL.registerMessage(1, SickleStatePacket.class,
                SickleStatePacket::encode, SickleStatePacket::decode, SickleStatePacket::handle);
        CHANNEL.registerMessage(2, SickleGhostPacket.class,
                SickleGhostPacket::encode, SickleGhostPacket::decode, SickleGhostPacket::handle);
        CHANNEL.registerMessage(3, SickleOrbitPacket.class,
                SickleOrbitPacket::encode, SickleOrbitPacket::decode, SickleOrbitPacket::handle);
        CHANNEL.registerMessage(4, MailStatePacket.class,
                MailStatePacket::encode, MailStatePacket::decode, MailStatePacket::handle);
        CHANNEL.registerMessage(5, UseActivePacket.class,
                UseActivePacket::encode, UseActivePacket::decode, UseActivePacket::handle);
        CHANNEL.registerMessage(6, SoulPrisonPacket.class,
                SoulPrisonPacket::encode, SoulPrisonPacket::decode, SoulPrisonPacket::handle);
        CHANNEL.registerMessage(7, AdrenalineStatePacket.class,
                AdrenalineStatePacket::encode, AdrenalineStatePacket::decode, AdrenalineStatePacket::handle);
        CHANNEL.registerMessage(8, AdrenalineAuraPacket.class,
                AdrenalineAuraPacket::encode, AdrenalineAuraPacket::decode, AdrenalineAuraPacket::handle);
        CHANNEL.registerMessage(9, GaiaStatePacket.class,
                GaiaStatePacket::encode, GaiaStatePacket::decode, GaiaStatePacket::handle);
        CHANNEL.registerMessage(10, MailAuraPacket.class,
                MailAuraPacket::encode, MailAuraPacket::decode, MailAuraPacket::handle);
        CHANNEL.registerMessage(11, FlameStatePacket.class,
                FlameStatePacket::encode, FlameStatePacket::decode, FlameStatePacket::handle);
        CHANNEL.registerMessage(13, HerculesStatePacket.class,
                HerculesStatePacket::encode, HerculesStatePacket::decode, HerculesStatePacket::handle);
        CHANNEL.registerMessage(14, UrielStatePacket.class,
                UrielStatePacket::encode, UrielStatePacket::decode, UrielStatePacket::handle);
        CHANNEL.registerMessage(12, WindStatePacket.class,
                WindStatePacket::encode, WindStatePacket::decode, WindStatePacket::handle);
    }

    private ModNetwork() {}
}
