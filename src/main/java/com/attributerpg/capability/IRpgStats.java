package com.attributerpg.capability;

import com.attributerpg.attribute.RpgAttributeType;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * Capability interface representing a player's persistent RPG leveling and attribute data.
 */
public interface IRpgStats {

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
     * @return The highest vanilla XP level the player has ever reached.
     *         Used to prevent point farming exploits via death or Enchanting Tables.
     */
    int getHighestLevelAchieved();

    /**
     * Updates the highest vanilla XP level achieved by the player.
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

