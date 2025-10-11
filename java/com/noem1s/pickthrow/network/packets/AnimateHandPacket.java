// File: src/main/java/com/noem1s/pickthrow/network/packets/AnimateHandPacket.java
package com.noem1s.pickthrow.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AnimateHandPacket {
    private final boolean startAnimation;

    public AnimateHandPacket(boolean startAnimation) {
        this.startAnimation = startAnimation;
    }

    public static void encode(AnimateHandPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.startAnimation);
    }

    public static AnimateHandPacket decode(FriendlyByteBuf buffer) {
        return new AnimateHandPacket(buffer.readBoolean());
    }

    public static void handle(AnimateHandPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (msg.startAnimation) {
                ItemStack heldItem = player.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    player.startUsingItem(InteractionHand.MAIN_HAND);
                }
            } else {
                player.stopUsingItem();
            }
        });
        context.setPacketHandled(true);
    }
}