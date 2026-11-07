package com.attributerpg.network.packet;

import com.attributerpg.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-Client (S2C) packet that synchronizes the player's persistent {@link com.attributerpg.capability.IRpgStats}
 * capability data to the local client player and refreshes the RPG GUI if open.
 */
public class PacketSyncRpgData {

    private final CompoundTag data;

    public PacketSyncRpgData(CompoundTag data) {
        this.data = data;
    }

    public static void encode(PacketSyncRpgData msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static PacketSyncRpgData decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new PacketSyncRpgData(tag != null ? tag : new CompoundTag());
    }

    public static void handle(PacketSyncRpgData msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncRpgData(msg.data))
        );
        ctx.setPacketHandled(true);
    }
}

