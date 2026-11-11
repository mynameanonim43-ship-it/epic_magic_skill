package com.attributerpg.network.packet;

import com.attributerpg.attribute.RpgAttributeType;
import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.config.RpgConfig;
import com.attributerpg.network.ModNetworking;
import com.attributerpg.util.AttributeModifierHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client-to-Server (C2S) packet sent when the player clicks "Apply" in the RPG Character Screen.
 * Sends the batch of newly allocated points to be permanently applied on the server.
 */
public class PacketApplyAllocations {

    private final Map<RpgAttributeType, Integer> allocations;

    public PacketApplyAllocations(Map<RpgAttributeType, Integer> allocations) {
        this.allocations = allocations;
    }

    public static void encode(PacketApplyAllocations msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.allocations.size());
        for (Map.Entry<RpgAttributeType, Integer> entry : msg.allocations.entrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static PacketApplyAllocations decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<RpgAttributeType, Integer> map = new EnumMap<>(RpgAttributeType.class);
        for (int i = 0; i < size; i++) {
            RpgAttributeType type = buf.readEnum(RpgAttributeType.class);
            int amount = buf.readVarInt();
            map.put(type, amount);
        }
        return new PacketApplyAllocations(map);
    }

    public static void handle(PacketApplyAllocations msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || msg.allocations == null || msg.allocations.isEmpty()) {
                return;
            }

            player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                // Calculate total points requested to spend
                int totalRequested = 0;
                for (int amount : msg.allocations.values()) {
                    if (amount > 0) {
                        totalRequested += amount;
                    }
                }

                // Security check: Verify player has enough available status points
                if (totalRequested <= 0 || totalRequested > stats.getStatusPoints()) {
                    return;
                }

                // Verify each attribute doesn't exceed maxAttributeCap and is visible
                int successfullyAllocated = 0;
                for (Map.Entry<RpgAttributeType, Integer> entry : msg.allocations.entrySet()) {
                    RpgAttributeType type = entry.getKey();
                    int amount = entry.getValue();
                    if (type == null || !type.isVisibleInGui() || amount <= 0) {
                        continue;
                    }

                    int current = stats.getAttributeValue(type);
                    int cap = RpgConfig.getMaxAttributeCap();
                    int canAdd = Math.min(amount, Math.max(0, cap - current));
                    if (canAdd > 0 && stats.getStatusPoints() >= canAdd) {
                        stats.setAllocatedPoints(type, stats.getAllocatedPoints(type) + canAdd);
                        stats.addStatusPoints(-canAdd);
                        successfullyAllocated += canAdd;
                    }
                }

                if (successfullyAllocated > 0) {
                    // Re-apply all Vanilla and Modded attribute modifiers on the server
                    AttributeModifierHelper.applyAllModifiers(player, stats);

                    // Play triumphant sound on applying stats
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.PLAYER_LEVELUP,
                            SoundSource.PLAYERS,
                            0.6F,
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

