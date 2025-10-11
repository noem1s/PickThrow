// File: src/main/java/com/noem1s/pickthrow/network/packets/ThrowItemPacket.java
package com.noem1s.pickthrow.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThrowItemPacket {

    private static final long MAX_CHARGE_TIME = 2500L;
    private static final float BASE_SPEED = 0.5F;
    private static final float MAX_ADDITIONAL_SPEED = 1.0F;

    private final long chargeTime;

    public ThrowItemPacket(long chargeTime) {
        this.chargeTime = chargeTime;
    }

    public static void encode(ThrowItemPacket msg, FriendlyByteBuf buffer) {
        buffer.writeLong(msg.chargeTime);
    }

    public static ThrowItemPacket decode(FriendlyByteBuf buffer) {
        return new ThrowItemPacket(buffer.readLong());
    }

    public static void handle(ThrowItemPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) return;

            float chargePercent = Math.min(msg.chargeTime, MAX_CHARGE_TIME) / (float) MAX_CHARGE_TIME;
            float velocity = BASE_SPEED + (chargePercent * MAX_ADDITIONAL_SPEED);

            // Reverted to original logic: Split 1 item from the stack to create a new entity.
            ItemStack thrownStack = heldItem.split(1);

            ItemEntity itemEntity = new ItemEntity(player.level(),
                    player.getX(), player.getEyeY() - 0.3, player.getZ(),
                    thrownStack
            );

            Vec3 lookVec = player.getLookAngle();
            itemEntity.setDeltaMovement(lookVec.scale(velocity));

            itemEntity.setPickUpDelay(40);
            player.level().addFreshEntity(itemEntity);
        });
        context.setPacketHandled(true);
    }
}