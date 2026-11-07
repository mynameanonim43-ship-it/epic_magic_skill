package com.attributerpg.network.packet;

import com.attributerpg.attribute.RpgAttributeType;
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
 * Client-to-Server (C2S) packet sent when a player clicks the [+] button next to an attribute
 * in the Custom RPG Character Screen.
 */
public class PacketAllocatePoint {

    private final RpgAttributeType attributeType;

    public PacketAllocatePoint(RpgAttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public static void encode(PacketAllocatePoint msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.attributeType);
    }

    public static PacketAllocatePoint decode(FriendlyByteBuf buf) {
        return new PacketAllocatePoint(buf.readEnum(RpgAttributeType.class));
    }

    public static void handle(PacketAllocatePoint msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || msg.attributeType == null) {
                return;
            }

            player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                if (stats.allocatePoint(msg.attributeType)) {
                    // Re-apply all Vanilla and Modded attribute modifiers on the server
                    AttributeModifierHelper.applyAllModifiers(player, stats);

                    // Play subtle UI experience chime feedback
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            0.5F,
                            1.35F
                    );

                    // Synchronize updated capability back to the client
                    ModNetworking.syncRpgData(player);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}

