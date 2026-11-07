package com.attributerpg.client;

import com.attributerpg.AttributeRpgMod;
import com.attributerpg.client.gui.RpgCharacterScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens for client ticks to detect when the player presses the 'O' keybinding
 * and opens the {@link RpgCharacterScreen}.
 */
@Mod.EventBusSubscriber(modid = AttributeRpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        while (KeyBindings.OPEN_CHARACTER_GUI_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new RpgCharacterScreen());
            }
        }
    }
}

