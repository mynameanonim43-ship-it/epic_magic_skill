package com.attributerpg.capability;

import com.attributerpg.AttributeRpgMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability Provider and NBT Serializer attaching {@link IRpgStats} to Player entities.
 */
public class RpgStatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final ResourceLocation IDENTIFIER =
            ResourceLocation.fromNamespaceAndPath(AttributeRpgMod.MODID, "rpg_stats");

    public static final Capability<IRpgStats> RPG_STATS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private RpgStats backend = null;
    private final LazyOptional<IRpgStats> optional = LazyOptional.of(this::createRpgStats);

    @NotNull
    private RpgStats createRpgStats() {
        if (this.backend == null) {
            this.backend = new RpgStats();
        }
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
        createRpgStats().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createRpgStats().loadNBTData(nbt);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
