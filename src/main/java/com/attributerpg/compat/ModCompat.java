package com.attributerpg.compat;

import net.minecraftforge.fml.ModList;

/**
 * Central utility class to safely check runtime presence of optional soft-dependency mods.
 * Prevents ClassNotFoundException / NoClassDefFoundError when optional mods are omitted at runtime.
 */
public final class ModCompat {

    public static final String EPIC_FIGHT_MOD_ID = "epicfight";
    public static final String IRONS_SPELLBOOKS_MOD_ID = "irons_spellbooks";

    private ModCompat() {
    }

    /**
     * @return true if Epic Fight ("epicfight") is loaded in the current runtime environment.
     */
    public static boolean isEpicFightLoaded() {
        return ModList.get().isLoaded(EPIC_FIGHT_MOD_ID);
    }

    /**
     * @return true if Iron's Spells 'n Spellbooks ("irons_spellbooks") is loaded in the current runtime environment.
     */
    public static boolean isIronsSpellbooksLoaded() {
        return ModList.get().isLoaded(IRONS_SPELLBOOKS_MOD_ID);
    }
}

