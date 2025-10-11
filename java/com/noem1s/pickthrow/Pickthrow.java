// File: src/main/java/com/noem1s/pickthrow/Pickthrow.java
package com.noem1s.pickthrow;

import com.noem1s.pickthrow.network.PacketHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("'get()' is deprecated since version 1.21.1 and marked for removal")
@Mod(Pickthrow.MOD_ID)
public class Pickthrow {
    public static final String MOD_ID = "pickthrow";
    public static final Logger LOGGER = LogManager.getLogger();

    public Pickthrow() {
        // CORRECTED: Using the non-deprecated method from ModLoadingContext
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // The common setup event is a good place to register our network packets
        event.enqueueWork(PacketHandler::register);
        LOGGER.info("Pickthrow common setup complete.");
    }
}