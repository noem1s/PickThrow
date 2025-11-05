// File: src/main/java/com/noem1s/pickthrow/events/PickupEvents.java
package com.noem1s.pickthrow.events;

import com.noem1s.pickthrow.Pickthrow;
import net.minecraft.ChatFormatting;
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
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerPickup(EntityItemPickupEvent event) {
        if (SUCKED_ITEMS.containsKey(event.getItem().getUUID())) {
            return;
        }
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

            // Create a temporary, mutable copy of the player's inventory to simulate the entire pickup action at once.
            // This prevents trying to pick up more items than will actually fit.
            List<ItemStack> simulatedInventory = new ArrayList<>();
            for (ItemStack stack : player.getInventory().items) {
                simulatedInventory.add(stack.copy());
            }

            boolean inventoryFullMessageSent = false;
            for (ItemEntity itemEntity : nearbyItems) {
                // If the simulated inventory can accept the item, mark it for pickup
                if (tryAddItem(simulatedInventory, itemEntity.getItem())) {
                    itemEntity.setPickUpDelay(0);
                    SUCKED_ITEMS.put(itemEntity.getUUID(), player.getUUID());
                } else {
                    // If the simulation fails, it means there's no space.
                    // We send the message once to avoid spam.
                    if (!inventoryFullMessageSent) {
                        player.displayClientMessage(Component.literal("Inventory full").withStyle(ChatFormatting.RED), true);
                        inventoryFullMessageSent = true;
                    }
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

    /**
     * Simulates adding an item stack to a list representing an inventory.
     * This is a preventative check to ensure an item can fit before we try to move it.
     * It works by trying to merge with existing stacks first, then finding an empty slot.
     *
     * @param simulatedInventory A list of ItemStacks representing the inventory. This list IS modified.
     * @param stackToAdd The ItemStack to be added. A copy is made, so the original is not modified.
     * @return true if the entire stack could be added, false otherwise.
     */
    private static boolean tryAddItem(List<ItemStack> simulatedInventory, ItemStack stackToAdd) {
        ItemStack remaining = stackToAdd.copy();

        // Pass 1: Try to merge with existing stacks in the main inventory area (first 36 slots)
        for (int i = 0; i < 36; i++) {
            ItemStack invStack = simulatedInventory.get(i);
            if (ItemStack.isSameItemSameTags(invStack, remaining) && invStack.isStackable() && invStack.getCount() < invStack.getMaxStackSize()) {
                int canAccept = invStack.getMaxStackSize() - invStack.getCount();
                int toTransfer = Math.min(canAccept, remaining.getCount());
                invStack.grow(toTransfer);
                remaining.shrink(toTransfer);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }

        // Pass 2: Find an empty slot in the main inventory area
        for (int i = 0; i < 36; i++) {
            if (simulatedInventory.get(i).isEmpty()) {
                simulatedInventory.set(i, remaining);
                return true;
            }
        }

        return false; // No space found
    }
}
