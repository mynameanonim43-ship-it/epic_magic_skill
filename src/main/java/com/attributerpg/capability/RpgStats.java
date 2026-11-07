package com.attributerpg.capability;

import com.attributerpg.attribute.RpgAttributeType;
import com.attributerpg.config.RpgConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Default implementation of {@link IRpgStats} storing Status Points, Highest Level Achieved,
 * and allocated attribute levels.
 */
public class RpgStats implements IRpgStats {

    private static final String NBT_STATUS_POINTS = "StatusPoints";
    private static final String NBT_HIGHEST_LEVEL = "HighestLevelAchieved";
    private static final String NBT_ATTRIBUTES_TAG = "AllocatedAttributes";

    private int statusPoints = 0;
    private int highestLevelAchieved = 0;
    private final EnumMap<RpgAttributeType, Integer> attributes = new EnumMap<>(RpgAttributeType.class);

    public RpgStats() {
        for (RpgAttributeType type : RpgAttributeType.values()) {
            attributes.put(type, 0);
        }
    }

    @Override
    public int getStatusPoints() {
        return statusPoints;
    }

    @Override
    public void setStatusPoints(int points) {
        this.statusPoints = Math.max(0, points);
    }

    @Override
    public void addStatusPoints(int delta) {
        this.statusPoints = Math.max(0, this.statusPoints + delta);
    }

    @Override
    public int getHighestLevelAchieved() {
        return highestLevelAchieved;
    }

    @Override
    public void setHighestLevelAchieved(int level) {
        this.highestLevelAchieved = Math.max(0, level);
    }

    @Override
    public int getAttributeValue(RpgAttributeType type) {
        return attributes.getOrDefault(type, 0);
    }

    @Override
    public void setAttributeValue(RpgAttributeType type, int value) {
        attributes.put(type, Math.max(0, Math.min(value, RpgConfig.getMaxAttributeCap())));
    }

    @Override
    public boolean allocatePoint(RpgAttributeType type) {
        if (this.statusPoints <= 0 || type == null) {
            return false;
        }
        // Prevent allocating into hidden attributes if their soft-dependency mod is not loaded
        if (!type.isVisibleInGui()) {
            return false;
        }
        int current = getAttributeValue(type);
        if (current >= RpgConfig.getMaxAttributeCap()) {
            return false;
        }
        this.statusPoints -= 1;
        this.attributes.put(type, current + 1);
        return true;
    }

    @Override
    public int resetPoints() {
        int refunded = 0;
        for (RpgAttributeType type : RpgAttributeType.values()) {
            refunded += getAttributeValue(type);
            attributes.put(type, 0);
        }
        this.statusPoints += refunded;
        return refunded;
    }

    @Override
    public Map<RpgAttributeType, Integer> getAllAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public void copyFrom(IRpgStats source) {
        this.statusPoints = source.getStatusPoints();
        this.highestLevelAchieved = source.getHighestLevelAchieved();
        for (RpgAttributeType type : RpgAttributeType.values()) {
            this.attributes.put(type, source.getAttributeValue(type));
        }
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt(NBT_STATUS_POINTS, this.statusPoints);
        nbt.putInt(NBT_HIGHEST_LEVEL, this.highestLevelAchieved);

        CompoundTag attrTag = new CompoundTag();
        for (RpgAttributeType type : RpgAttributeType.values()) {
            attrTag.putInt(type.getSerializedName(), getAttributeValue(type));
        }
        nbt.put(NBT_ATTRIBUTES_TAG, attrTag);
    }

    @Override
    public void loadNBTData(CompoundTag nbt) {
        this.statusPoints = Math.max(0, nbt.getInt(NBT_STATUS_POINTS));
        this.highestLevelAchieved = Math.max(0, nbt.getInt(NBT_HIGHEST_LEVEL));

        CompoundTag attrTag = nbt.getCompound(NBT_ATTRIBUTES_TAG);
        for (RpgAttributeType type : RpgAttributeType.values()) {
            this.attributes.put(type, Math.max(0, attrTag.getInt(type.getSerializedName())));
        }
    }
}
