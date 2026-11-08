package com.attributerpg.capability;

import com.attributerpg.attribute.RpgAttributeType;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * Capability interface representing a player's persistent RPG leveling, EXP, and attribute data.
 * The RPG Level and RPG Experience are completely independent from Vanilla Experience/Deaths/Enchanting.
 */
public interface IRpgStats {

    /**
     * @return The player's persistent RPG Level (starts at 1).
     */
    int getRpgLevel();

    /**
     * Sets the player's persistent RPG Level.
     */
    void setRpgLevel(int level);

    /**
     * @return The player's current RPG Experience points towards the next level.
     */
    int getRpgExp();

    /**
     * Sets the player's current RPG Experience points.
     */
    void setRpgExp(int exp);

    /**
     * Adds RPG experience points. If enough exp is accumulated to reach the next level,
     * levels up the player, awards Status Points, and carries over leftover exp.
     *
     * @param exp Points of experience gained.
     * @return Number of level-ups triggered.
     */
    int addRpgExp(int exp);

    /**
     * Calculates the experience required to level up from the specified RPG Level.
     * Matches Vanilla Minecraft's progressive scaling (each level gets progressively harder):
     * - Level 1 to 15:  2 * Level + 7
     * - Level 16 to 30: 5 * Level - 38
     * - Level 31+:      9 * Level - 158
     */
    static int getExpNeededForLevel(int level) {
        if (level <= 0) {
            return 7;
        }
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    /**
     * @return The player's current unspent Status Points balance.
     */
    int getStatusPoints();

    /**
     * Sets the player's available Status Points balance.
     */
    void setStatusPoints(int points);

    /**
     * Adds (or subtracts) Status Points from the player's balance.
     */
    void addStatusPoints(int delta);

    /**
     * Legacy getter kept for backward compatibility with existing saves.
     */
    int getHighestLevelAchieved();

    /**
     * Legacy setter kept for backward compatibility with existing saves.
     */
    void setHighestLevelAchieved(int level);

    /**
     * @return The number of points allocated to the specified RPG attribute.
     */
    int getAttributeValue(RpgAttributeType type);

    /**
     * Sets the allocated value for a specific RPG attribute.
     */
    void setAttributeValue(RpgAttributeType type, int value);

    /**
     * Attempts to spend 1 Status Point to increment the target RPG attribute by 1.
     *
     * @param type The attribute to upgrade.
     * @return true if the point was successfully spent and allocated.
     */
    boolean allocatePoint(RpgAttributeType type);

    /**
     * Refunds all allocated attribute points back into the available Status Points balance
     * and resets all RPG attribute allocations to 0.
     *
     * @return The total number of points refunded.
     */
    int resetPoints();

    /**
     * @return An unmodifiable view of all attribute allocations.
     */
    Map<RpgAttributeType, Integer> getAllAttributes();

    /**
     * Copies all state from an existing capability instance (used during PlayerEvent.Clone).
     */
    void copyFrom(IRpgStats source);

    /**
     * Serializes the capability state into an NBT CompoundTag.
     */
    void saveNBTData(CompoundTag nbt);

    /**
     * Deserializes the capability state from an NBT CompoundTag.
     */
    void loadNBTData(CompoundTag nbt);
}
