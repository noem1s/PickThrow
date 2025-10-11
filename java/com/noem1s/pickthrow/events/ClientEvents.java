// File: src/main/java/com/noem1s/pickthrow/events/ClientEvents.java
package com.noem1s.pickthrow.events;

import com.mojang.blaze3d.platform.InputConstants;
import com.noem1s.pickthrow.Pickthrow;
import com.noem1s.pickthrow.network.PacketHandler;
import com.noem1s.pickthrow.network.packets.ActivateItemPacket;
import com.noem1s.pickthrow.network.packets.ThrowItemPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
// CORRECTED: Added the missing import for ViewportEvent.
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Pickthrow.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static long chargeStartTime = 0;
    private static final double MAX_FOV_OFFSET = 6.0;
    private static boolean keyConflictResolved = false;

    private static double fovOffset = 0.0;

    public static final KeyMapping CHARGE_THROW_KEY = new KeyMapping(
            "key." + Pickthrow.MOD_ID + ".charge_throw",
            InputConstants.Type.KEYSYM,
            // CORRECTED: Restored the full key name.
            GLFW.GLFW_KEY_Q,
            "key.categories.inventory"
    );

    @Mod.EventBusSubscriber(modid = Pickthrow.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CHARGE_THROW_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (!keyConflictResolved) {
            KeyMapping dropKey = mc.options.keyDrop;
            if (CHARGE_THROW_KEY.same(dropKey)) {
                InputConstants.Key ourOriginalKey = CHARGE_THROW_KEY.getKey();
                dropKey.setKey(InputConstants.UNKNOWN);
                CHARGE_THROW_KEY.setKey(ourOriginalKey);
                KeyMapping.setAll();
                Pickthrow.LOGGER.info("Pickthrow unbound vanilla drop key and re-asserted custom key to resolve conflict.");
            }
            keyConflictResolved = true;
        }

        boolean isKeyDown = CHARGE_THROW_KEY.isDown();
        double targetFovOffset = 0.0;

        if (isKeyDown && chargeStartTime == 0 && !mc.player.getMainHandItem().isEmpty()) {
            chargeStartTime = System.currentTimeMillis();
        } else if (!isKeyDown && chargeStartTime > 0) {
            long chargeTime = System.currentTimeMillis() - chargeStartTime;
            PacketHandler.sendToServer(new ThrowItemPacket(chargeTime));
            mc.player.swing(InteractionHand.MAIN_HAND);
            chargeStartTime = 0;
        }

        if (chargeStartTime > 0) {
            long chargeTime = System.currentTimeMillis() - chargeStartTime;
            double fovScale = Math.min(chargeTime / 2500.0, 1.0);
            targetFovOffset = fovScale * MAX_FOV_OFFSET;
        }

        fovOffset += (targetFovOffset - fovOffset) * 0.4;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (Math.abs(fovOffset) > 0.01) {
            // CORRECTED: The method names are getFOV() and setFOV().
            event.setFOV(event.getFOV() + fovOffset);
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        HitResult result = rayTrace(player);

        if (result.getType() == HitResult.Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) result).getEntity();
            if (targetEntity instanceof ItemEntity) {
                PacketHandler.sendToServer(new ActivateItemPacket(targetEntity.getUUID()));
                event.setCanceled(true);
                player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static HitResult rayTrace(Player player) {
        double range = PickupEvents.MAX_RANGE_RIGHT_CLICK;
        Vec3 startVec = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endVec = startVec.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
        AABB boundingBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                startVec,
                endVec,
                boundingBox,
                (entity) -> !entity.isSpectator() && entity instanceof ItemEntity
        );

        BlockHitResult blockResult = player.level().clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (entityResult == null) return blockResult;

        double blockDistSq = blockResult.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : startVec.distanceToSqr(blockResult.getLocation());
        double entityDistSq = startVec.distanceToSqr(entityResult.getLocation());

        return entityDistSq < blockDistSq ? entityResult : blockResult;
    }
}