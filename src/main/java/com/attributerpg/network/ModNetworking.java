package com.attributerpg.network;

import com.attributerpg.AttributeRpgMod;
import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.network.packet.PacketAllocatePoint;
import com.attributerpg.network.packet.PacketApplyAllocations;
import com.attributerpg.network.packet.PacketResetPoints;
import com.attributerpg.network.packet.PacketSyncRpgData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Manages the SimpleChannel network pipeline for bidirectional C2S and S2C synchronization.
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AttributeRpgMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private ModNetworking() {
    }

    private static int nextId() {
        return packetId++;
    }

    /**
     * Registers all C2S and S2C packets during FMLCommonSetupEvent.
     */
    public static void register() {
        CHANNEL.registerMessage(
                nextId(),
                PacketSyncRpgData.class,
                PacketSyncRpgData::encode,
                PacketSyncRpgData::decode,
                PacketSyncRpgData::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextId(),
                PacketAllocatePoint.class,
                PacketAllocatePoint::encode,
                PacketAllocatePoint::decode,
                PacketAllocatePoint::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                nextId(),
                PacketResetPoints.class,
                PacketResetPoints::encode,
                PacketResetPoints::decode,
                PacketResetPoints::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                nextId(),
                PacketApplyAllocations.class,
                PacketApplyAllocations::encode,
                PacketApplyAllocations::decode,
                PacketApplyAllocations::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    /**
     * Sends a C2S packet from the client to the integrated or dedicated server.
     */
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Sends an S2C packet from the server to a specific player client.
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Helper to serialize and synchronize a ServerPlayer's RPG capability data to their client.
     */
    public static void syncRpgData(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
            CompoundTag tag = new CompoundTag();
            stats.saveNBTData(tag);
            sendToPlayer(new PacketSyncRpgData(tag), player);
        });
    }
}
