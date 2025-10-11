// File: src/main/java/com/noem1s/pickthrow/network/packets/ActivateItemPacket.java
package com.noem1s.pickthrow.network.packets;

import com.noem1s.pickthrow.events.PickupEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ActivateItemPacket {
    private final UUID itemEntityUUID;

    public ActivateItemPacket(UUID itemEntityUUID) {
        this.itemEntityUUID = itemEntityUUID;
    }

    public static void encode(ActivateItemPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUUID(msg.itemEntityUUID);
    }

    public static ActivateItemPacket decode(FriendlyByteBuf buffer) {
        return new ActivateItemPacket(buffer.readUUID());
    }

    public static void handle(ActivateItemPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity entity = ((ServerLevel) player.level()).getEntity(msg.itemEntityUUID);

            if (entity instanceof ItemEntity itemEntity) {
                if (player.distanceToSqr(itemEntity) <= PickupEvents.MAX_RANGE_RIGHT_CLICK * PickupEvents.MAX_RANGE_RIGHT_CLICK) {
                    itemEntity.setPickUpDelay(0);
                    PickupEvents.SUCKED_ITEMS.put(itemEntity.getUUID(), player.getUUID());
                }
            }
        });
        context.setPacketHandled(true);
    }
}