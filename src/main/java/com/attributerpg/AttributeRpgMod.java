package com.attributerpg;

import com.attributerpg.attribute.ModAttributes;
import com.attributerpg.compat.ModCompat;
import com.attributerpg.config.RpgConfig;
import com.attributerpg.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Main entrypoint for the Custom RPG Attribute & Leveling Mod.
 */
@Mod(AttributeRpgMod.MODID)
public class AttributeRpgMod {

    public static final String MODID = "attributerpg";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AttributeRpgMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register custom RPG Forge attributes
        ModAttributes.register(modEventBus);

        // Register common setup listener for networking
        modEventBus.addListener(this::commonSetup);

        // Register Common Config (generates config/attributerpg-common.toml)
        context.registerConfig(ModConfig.Type.COMMON, RpgConfig.SPEC, "attributerpg-common.toml");

        // Register ourselves on the Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            LOGGER.info("[AttributeRPG] Initialized network channel & config. Epic Fight loaded: {}, Iron's Spells loaded: {}",
                    ModCompat.isEpicFightLoaded(), ModCompat.isIronsSpellbooksLoaded());
        });
    }
}
