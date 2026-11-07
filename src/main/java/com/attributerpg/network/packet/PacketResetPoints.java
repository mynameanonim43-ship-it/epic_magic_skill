package com.attributerpg.network.packet;

import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.network.ModNetworking;
import com.attributerpg.util.AttributeModifierHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-Server (C2S) packet sent when a player clicks the "Reset Points" button
 * in the footer of the Custom RPG Character Screen.
 */
public class PacketResetPoints {

    public PacketResetPoints() {
    }

    public static void encode(PacketResetPoints msg, FriendlyByteBuf buf) {
        // No payload needed
    }

    public static PacketResetPoints decode(FriendlyByteBuf buf) {
        return new PacketResetPoints();
    }

    public static void handle(PacketResetPoints msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                int refunded = stats.resetPoints();
                if (refunded > 0) {
                    // Re-apply all Vanilla and Modded attribute modifiers (clears zeroed modifiers)
                    AttributeModifierHelper.applyAllModifiers(player, stats);

                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ANVIL_USE,
                            SoundSource.PLAYERS,
                            0.45F,
                            1.2F
                    );

                    // Synchronize updated capability back to the client
                    ModNetworking.syncRpgData(player);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}

