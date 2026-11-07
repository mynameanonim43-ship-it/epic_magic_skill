package com.attributerpg.client;

import com.attributerpg.AttributeRpgMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the 'O' keybinding to open the Custom RPG Character Screen.
 */
@Mod.EventBusSubscriber(modid = AttributeRpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {

    public static final String KEY_CATEGORY_RPG = "key.categories.attributerpg";
    public static final String KEY_OPEN_CHARACTER_GUI = "key.attributerpg.open_character_gui";

    public static final KeyMapping OPEN_CHARACTER_GUI_KEY = new KeyMapping(
            KEY_OPEN_CHARACTER_GUI,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KEY_CATEGORY_RPG
    );

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CHARACTER_GUI_KEY);
    }
}

