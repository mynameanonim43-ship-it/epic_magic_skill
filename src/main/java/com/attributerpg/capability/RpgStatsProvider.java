package com.attributerpg.capability;

import com.attributerpg.AttributeRpgMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability Provider and NBT Serializer attaching {@link IRpgStats} to Player entities.
 */
public class RpgStatsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation IDENTIFIER =
            ResourceLocation.fromNamespaceAndPath(AttributeRpgMod.MODID, "rpg_stats");

    public static final Capability<IRpgStats> RPG_STATS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final RpgStats backend = new RpgStats();
    private final LazyOptional<IRpgStats> optional = LazyOptional.of(() -> this.backend);

    public RpgStats getBackend() {
        return this.backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == RPG_STATS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.backend.saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.backend.loadNBTData(nbt);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
