package com.attributerpg.client;

import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.client.gui.RpgCharacterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only packet handler that applies S2C capability sync data to the local player
 * and refreshes the Custom RPG GUI Screen if currently open.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleSyncRpgData(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
            stats.loadNBTData(data);
            if (mc.screen instanceof RpgCharacterScreen rpgScreen) {
                rpgScreen.refreshButtonStates();
            }
        });
    }
}

