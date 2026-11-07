package com.attributerpg.attribute;

import com.attributerpg.compat.ModCompat;
import com.attributerpg.config.RpgConfig;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines all RPG Attributes, their display icons, colors, scaling constants,
 * and runtime conditional visibility/descriptions based on loaded soft dependencies and RpgConfig.
 */
public enum RpgAttributeType {
    STR("str", "Strength (STR)", "\u2694", 0xFFE05A47),
    RNG("rng", "Ranged (RNG)", "\uD83C\uDFF9", 0xFF5CD65C),
    CON("con", "Constitution (CON)", "\u2764", 0xFFFF4D6D),
    AGI("agi", "Agility (AGI)", "\u26A1", 0xFF4DD2FF),
    INT("int", "Intelligence (INT)", "\u2728", 0xFFB366FF),
    WIS("wis", "Wisdom (WIS)", "\uD83D\uDCD6", 0xFF5C85FF),
    LUK("luk", "Luck (LUK)", "\uD83C\uDF40", 0xFFFFD11A);

    private static final DecimalFormat FMT = new DecimalFormat("0.##");

    private final String serializedName;
    private final String displayName;
    private final String iconPlaceholder;
    private final int accentColor;

    RpgAttributeType(String serializedName, String displayName, String iconPlaceholder, int accentColor) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.iconPlaceholder = iconPlaceholder;
        this.accentColor = accentColor;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPlaceholder() {
        return iconPlaceholder;
    }

    public int getAccentColor() {
        return accentColor;
    }

    /**
     * Determines if this attribute row should be visible and rendered in the RPG GUI.
     * Requirement: If "Iron's Spells 'n Spellbooks" is NOT loaded, completely hide and remove
     * Intelligence (INT) and Wisdom (WIS) rows from the GUI list.
     */
    public boolean isVisibleInGui() {
        if (this == INT || this == WIS) {
            return ModCompat.isIronsSpellbooksLoaded();
        }
        return true;
    }

    /**
     * Returns the dynamically filtered list of attributes to display in the GUI,
     * ensuring no blank spaces remain when optional mods are unloaded.
     */
    public static List<RpgAttributeType> getVisibleAttributes() {
        List<RpgAttributeType> visible = new ArrayList<>();
        for (RpgAttributeType type : values()) {
            if (type.isVisibleInGui()) {
                visible.add(type);
            }
        }
        return visible;
    }

    /**
     * Builds the dynamic description text for the GUI and tooltips using live values from {@link RpgConfig}.
     * Note: Strength now includes Attack Speed (+0.5% per point = +50% at level 100).
     */
    public String getDynamicSummary() {
        return switch (this) {
            case STR -> ModCompat.isEpicFightLoaded()
                    ? "+" + FMT.format(RpgConfig.getStrAttackDamage()) + " Dmg | +"
                      + FMT.format(RpgConfig.getStrAttackSpeedMultiplier() * 100.0D) + "% Spd | +"
                      + FMT.format(RpgConfig.getStrEpicFightArmorNegation()) + "% Neg"
                    : "+" + FMT.format(RpgConfig.getStrAttackDamage()) + " Attack Dmg | +"
                      + FMT.format(RpgConfig.getStrAttackSpeedMultiplier() * 100.0D) + "% Attack Speed";
            case RNG -> "+" + FMT.format(RpgConfig.getRngRangedDamage()) + " Ranged Dmg | +"
                      + FMT.format(RpgConfig.getRngProjectileSpeed() * 100.0D) + "% Proj. Speed";
            case CON -> "+" + FMT.format(RpgConfig.getConMaxHealth()) + " Health | +"
                      + FMT.format(RpgConfig.getConKnockbackResistance() * 100.0D) + "% Knockback Resist";
            case AGI -> ModCompat.isEpicFightLoaded()
                    ? "+" + FMT.format(RpgConfig.getAgiMovementSpeed() * 100.0D) + "% Speed | +"
                      + FMT.format(RpgConfig.getAgiEpicFightStaminaRegen()) + " Stamina Regen"
                    : "+" + FMT.format(RpgConfig.getAgiMovementSpeed() * 100.0D) + "% Vanilla Movement Speed";
            case INT -> "+" + FMT.format(RpgConfig.getIntSpellPowerMultiplier() * 100.0D) + "% Spell Power Multiplier";
            case WIS -> "+" + FMT.format(RpgConfig.getWisMaxMana()) + " Max Mana | +"
                      + FMT.format(RpgConfig.getWisManaRegenMultiplier() * 100.0D) + "% Mana Regen";
            case LUK -> "+" + FMT.format(RpgConfig.getLukVanillaLuck()) + " Luck | +"
                      + FMT.format(RpgConfig.getLukCritDropChance() * 100.0D) + "% Crit Loot Drop";
        };
    }

    public static RpgAttributeType byName(String name) {
        for (RpgAttributeType type : values()) {
            if (type.serializedName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
