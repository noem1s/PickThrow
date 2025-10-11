// File: src/main/java/com/noem1s/pickthrow/events/PickupEvents.java
package com.noem1s.pickthrow.events;

import com.noem1s.pickthrow.Pickthrow;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
// CORRECTED IMPORT: This is the original and correct event, which is cancelable.
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.ChatFormatting;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Pickthrow.MOD_ID)
public class PickupEvents {

    public static final Map<UUID, UUID> SUCKED_ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CROUCH_STATE_MAP = new ConcurrentHashMap<>();

    // --- TUNING KNOBS ---
    public static final double MAX_RANGE_RIGHT_CLICK = 5.0D;
    public static final double AOE_RANGE_CROUCH = 2.5D;
    public static final double SPEED = 0.8D;

    // CORRECTED METHOD: Using the proper, cancelable EntityItemPickupEvent
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerPickup(EntityItemPickupEvent event) {
        // If the item is in our "suck" map, allow the pickup to proceed
        if (SUCKED_ITEMS.containsKey(event.getItem().getUUID())) {
            return;
        }
        // Otherwise, cancel the event to prevent pickup by walking over it
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        boolean isNowCrouching = player.isCrouching();
        boolean wasPreviouslyCrouching = CROUCH_STATE_MAP.getOrDefault(player.getUUID(), false);

        if (isNowCrouching && !wasPreviouslyCrouching) {
            AABB searchBox = player.getBoundingBox().inflate(AOE_RANGE_CROUCH);
            List<ItemEntity> nearbyItems = player.level().getEntitiesOfClass(ItemEntity.class, searchBox);

            for (ItemEntity item : nearbyItems) {
                if (hasSpaceInInventory(player, item.getItem())) {
                    item.setPickUpDelay(0);
                    SUCKED_ITEMS.put(item.getUUID(), player.getUUID());
                } else {
                    player.displayClientMessage(Component.literal("Inventory full").withStyle(ChatFormatting.RED), true);
                }
            }
        }
        CROUCH_STATE_MAP.put(player.getUUID(), isNowCrouching);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();

        SUCKED_ITEMS.entrySet().removeIf(entry -> {
            Player player = server.getPlayerList().getPlayer(entry.getValue());
            ItemEntity item = null;

            if (player != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity instanceof ItemEntity) {
                        item = (ItemEntity) entity;
                        break;
                    }
                }
            }

            if (player == null || item == null || !item.isAlive()) {
                return true;
            }

            if (!hasSpaceInInventory(player, item.getItem())) {
                player.displayClientMessage(Component.literal("Inventory full").withStyle(ChatFormatting.RED), true);
                return true;
            }

            Vec3 direction = player.getEyePosition().subtract(item.position());
            if (direction.lengthSqr() < 1.25D) {
                item.playerTouch(player);
                return !item.isAlive();
            }

            Vec3 motion = direction.normalize().scale(SPEED);
            item.setDeltaMovement(motion);
            return false;
        });
    }

    private static boolean hasSpaceInInventory(Player player, ItemStack stackToPickup) {
        if (stackToPickup.isEmpty()) {
            return true;
        }
        // Check for any empty slot
        if (player.getInventory().getFreeSlot() != -1) {
            return true;
        }
        // If no empty slots, check for existing stacks that can accept the item
        for (ItemStack invStack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(invStack, stackToPickup) && invStack.getCount() < invStack.getMaxStackSize()) {
                // Check if the remaining space is enough
                if (invStack.getCount() + stackToPickup.getCount() <= invStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }
}