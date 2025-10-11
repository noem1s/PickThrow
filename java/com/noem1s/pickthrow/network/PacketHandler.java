// File: src/main/java/com/noem1s/pickthrow/network/PacketHandler.java
package com.noem1s.pickthrow.network;

import com.noem1s.pickthrow.Pickthrow;
import com.noem1s.pickthrow.network.packets.ActivateItemPacket;
import com.noem1s.pickthrow.network.packets.AnimateHandPacket;
import com.noem1s.pickthrow.network.packets.ThrowItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            // CORRECTED: Using the non-deprecated method
            ResourceLocation.fromNamespaceAndPath(Pickthrow.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(ActivateItemPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateItemPacket::encode).decoder(ActivateItemPacket::decode).consumerMainThread(ActivateItemPacket::handle).add();
        INSTANCE.messageBuilder(ThrowItemPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ThrowItemPacket::encode).decoder(ThrowItemPacket::decode).consumerMainThread(ThrowItemPacket::handle).add();
        INSTANCE.messageBuilder(AnimateHandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AnimateHandPacket::encode).decoder(AnimateHandPacket::decode).consumerMainThread(AnimateHandPacket::handle).add();
    }

    public static void sendToServer(Object msg) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }
}