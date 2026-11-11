package com.attributerpg.config;

import com.attributerpg.attribute.RpgAttributeType;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge Common Configuration for the Attribute RPG & Leveling Mod.
 * Generates "config/attributerpg-common.toml" with detailed English
 * documentation
 * explaining how to customize leveling rewards and every RPG attribute's stat
 * scaling.
 */
public final class RpgConfig {

        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;

        // =========================================================================
        // 1. LEVELING & STATUS POINTS SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.IntValue STATUS_POINTS_PER_LEVEL;
        public static final ForgeConfigSpec.IntValue MAX_ATTRIBUTE_CAP;
        public static final ForgeConfigSpec.DoubleValue EXP_COST_BASE_MULTIPLIER;
        public static final ForgeConfigSpec.DoubleValue EXP_COST_PER_LEVEL_INCREMENT;

        // =========================================================================
        // 1b. STARTING / BASE ATTRIBUTES SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.IntValue STARTING_CONSTITUTION;
        public static final ForgeConfigSpec.IntValue STARTING_STRENGTH;
        public static final ForgeConfigSpec.IntValue STARTING_RANGED;
        public static final ForgeConfigSpec.IntValue STARTING_AGILITY;
        public static final ForgeConfigSpec.IntValue STARTING_INTELLIGENCE;
        public static final ForgeConfigSpec.IntValue STARTING_WISDOM;
        public static final ForgeConfigSpec.IntValue STARTING_LUCK;

        // =========================================================================
        // 2. STRENGTH (STR) SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue STR_ATTACK_DAMAGE_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue STR_ATTACK_SPEED_MULTIPLIER_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue STR_EPICFIGHT_ARMOR_NEGATION_PER_POINT;

        // =========================================================================
        // 3. RANGED (RNG) SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue RNG_RANGED_DAMAGE_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue RNG_PROJECTILE_SPEED_PER_POINT;

        // =========================================================================
        // 4. CONSTITUTION (CON) SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue CON_MAX_HEALTH_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue CON_KNOCKBACK_RESISTANCE_PER_POINT;

        // =========================================================================
        // 5. AGILITY (AGI) SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue AGI_MOVEMENT_SPEED_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue AGI_EPICFIGHT_STAMINA_REGEN_PER_POINT;

        // =========================================================================
        // 6. INTELLIGENCE (INT) SETTINGS - Iron's Spells 'n Spellbooks
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue INT_IRONS_SPELL_POWER_PER_POINT;

        // =========================================================================
        // 7. WISDOM (WIS) SETTINGS - Iron's Spells 'n Spellbooks
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue WIS_IRONS_MAX_MANA_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue WIS_IRONS_MANA_REGEN_PER_POINT;

        // =========================================================================
        // 8. LUCK (LUK) SETTINGS
        // =========================================================================
        public static final ForgeConfigSpec.DoubleValue LUK_VANILLA_LUCK_PER_POINT;
        public static final ForgeConfigSpec.DoubleValue LUK_CRIT_DROP_CHANCE_PER_POINT;

        static {
                BUILDER.comment(
                                "====================================================================================",
                                " Attribute RPG & Leveling Mod - Configuration File (attributerpg-common.toml)",
                                "====================================================================================",
                                " HOW TO EDIT THIS CONFIG:",
                                " 1. Locate this file at: <your_minecraft_folder>/config/attributerpg-common.toml",
                                "    (For Dedicated Servers, also check: <world_folder>/serverconfig/ if applicable).",
                                " 2. Open the file with any text editor (Notepad, VS Code, Notepad++).",
                                " 3. Change the numbers after the '=' sign for any stat you wish to balance.",
                                "    - Flat bonuses (e.g., Attack Damage, Max Health, Max Mana) add the exact number",
                                "      per allocated point. Example: 2.0 Max Health = +1 full Heart per point.",
                                "    - Percentage/Multiplier bonuses (e.g., Attack Speed, Movement Speed, Spell Power, Crit Drop)",
                                "      use decimal format where 0.005 = 0.5%, 0.02 = 2%, 0.10 = 10%.",
                                " 4. Save the file and restart Minecraft (or reload the world) to apply your changes.",
                                "====================================================================================");

                // --- 1. Leveling ---
                BUILDER.push("leveling");
                STATUS_POINTS_PER_LEVEL = BUILDER
                                .comment(
                                                "Number of Status Points granted every time a player reaches a new RPG Level.",
                                                "Default: 4 | Range: 1 ~ 100")
                                .defineInRange("statusPointsPerLevel", 4, 1, 100);

                MAX_ATTRIBUTE_CAP = BUILDER
                                .comment(
                                                "Maximum number of points a player can allocate into a single RPG attribute.",
                                                "Default: 999 | Range: 1 ~ 10000")
                                .defineInRange("maxAttributeCap", 999, 1, 10000);

                EXP_COST_BASE_MULTIPLIER = BUILDER
                                .comment(
                                                "Base multiplier applied to the Vanilla EXP needed to level up.",
                                                "Setting this to 3.0 requires 3x the normal vanilla EXP curve to reach level ups.",
                                                "Default: 3.0 | Range: 0.1 ~ 100.0")
                                .defineInRange("expCostBaseMultiplier", 3.0D, 0.1D, 100.0D);

                EXP_COST_PER_LEVEL_INCREMENT = BUILDER
                                .comment(
                                                "Additional difficulty scaling multiplier added per level to make higher levels progressively steeper.",
                                                "Formula: VanillaExp * expCostBaseMultiplier * (1.0 + (level - 1) * expCostPerLevelIncrement)",
                                                "Setting to 0.2 increases the required EXP steepness by +20% with each subsequent level.",
                                                "Default: 0.2 | Range: 0.0 ~ 10.0")
                                .defineInRange("expCostPerLevelIncrement", 0.2D, 0.0D, 10.0D);
                BUILDER.pop();

                // --- 1b. Starting / Base Attributes ---
                BUILDER.push("starting_attributes");
                BUILDER.comment(
                                "Initial base attribute values granted to all players without spending status points.",
                                "These act as the foundation stat floor (e.g. baseline health from CON) and cannot be lost via resets.");

                STARTING_CONSTITUTION = BUILDER
                                .comment("Initial base Constitution (CON) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingConstitution", 0, 0, 1000);

                STARTING_STRENGTH = BUILDER
                                .comment("Initial base Strength (STR) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingStrength", 0, 0, 1000);

                STARTING_RANGED = BUILDER
                                .comment("Initial base Ranged (RNG) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingRanged", 0, 0, 1000);

                STARTING_AGILITY = BUILDER
                                .comment("Initial base Agility (AGI) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingAgility", 0, 0, 1000);

                STARTING_INTELLIGENCE = BUILDER
                                .comment("Initial base Intelligence (INT) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingIntelligence", 0, 0, 1000);

                STARTING_WISDOM = BUILDER
                                .comment("Initial base Wisdom (WIS) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingWisdom", 0, 0, 1000);

                STARTING_LUCK = BUILDER
                                .comment("Initial base Luck (LUK) points. Default: 0 | Range: 0 ~ 1000")
                                .defineInRange("startingLuck", 0, 0, 1000);
                BUILDER.pop();

                // --- 2. Strength (STR) ---
                BUILDER.push("strength_str");
                STR_ATTACK_DAMAGE_PER_POINT = BUILDER
                                .comment(
                                                "Flat Vanilla Melee Attack Damage (minecraft:generic.attack_damage) added per 1 point of STR.",
                                                "Default: 0.5 | Range: 0.0 ~ 1000.0")
                                .defineInRange("attackDamagePerPoint", 0.5D, 0.0D, 1000.0D);

                STR_ATTACK_SPEED_MULTIPLIER_PER_POINT = BUILDER
                                .comment(
                                                "Vanilla Attack Speed multiplier (minecraft:generic.attack_speed) added per 1 point of STR.",
                                                "Uses MULTIPLY_BASE operation. Setting to 0.002 (+0.2% per point) reaches exactly +20% attack speed at level 100.",
                                                "Default: 0.002 | Range: 0.0 ~ 5.0")
                                .defineInRange("attackSpeedMultiplierPerPoint", 0.002D, 0.0D, 5.0D);

                STR_EPICFIGHT_ARMOR_NEGATION_PER_POINT = BUILDER
                                .comment(
                                                "[Soft Dependency: Epic Fight] Armor Negation percentage (epicfight:armor_negation) added per 1 point of STR.",
                                                "Only active if Epic Fight is installed. 0.2 = +0.2% Armor Negation per point.",
                                                "Default: 0.2 | Range: 0.0 ~ 100.0")
                                .defineInRange("epicFightArmorNegationPerPoint", 0.2D, 0.0D, 100.0D);
                BUILDER.pop();

                // --- 3. Ranged (RNG) ---
                BUILDER.push("ranged_rng");
                RNG_RANGED_DAMAGE_PER_POINT = BUILDER
                                .comment(
                                                "Flat bonus damage added to arrows and projectiles fired by the player per 1 point of RNG.",
                                                "Default: 0.6 | Range: 0.0 ~ 1000.0")
                                .defineInRange("rangedDamagePerPoint", 0.6D, 0.0D, 1000.0D);

                RNG_PROJECTILE_SPEED_PER_POINT = BUILDER
                                .comment(
                                                "Projectile velocity multiplier added per 1 point of RNG (0.02 = +2% arrow speed per point).",
                                                "Default: 0.02 | Range: 0.0 ~ 5.0")
                                .defineInRange("projectileSpeedPerPoint", 0.02D, 0.0D, 5.0D);
                BUILDER.pop();

                // --- 4. Constitution (CON) ---
                BUILDER.push("constitution_con");
                CON_MAX_HEALTH_PER_POINT = BUILDER
                                .comment(
                                                "Flat Vanilla Max Health (minecraft:generic.max_health) added per 1 point of CON.",
                                                "2.0 Health = 1 full Heart. Setting to 2.0 grants +1 full Heart per point.",
                                                "Default: 2.0 | Range: 0.0 ~ 1000.0")
                                .defineInRange("maxHealthPerPoint", 2.0D, 0.0D, 1000.0D);

                CON_KNOCKBACK_RESISTANCE_PER_POINT = BUILDER
                                .comment(
                                                "Vanilla Knockback Resistance (minecraft:generic.knockback_resistance) added per 1 point of CON.",
                                                "In Vanilla, 1.0 = 100% knockback immunity. 0.008 per point reaches exactly 0.8 (80% knockback reduction) at 100 points.",
                                                "Default: 0.008 | Range: 0.0 ~ 1.0")
                                .defineInRange("knockbackResistancePerPoint", 0.008D, 0.0D, 1.0D);
                BUILDER.pop();

                // --- 5. Agility (AGI) ---
                BUILDER.push("agility_agi");
                AGI_MOVEMENT_SPEED_PER_POINT = BUILDER
                                .comment(
                                                "Vanilla Movement Speed multiplier (minecraft:generic.movement_speed) added per 1 point of AGI.",
                                                "Uses MULTIPLY_BASE: 0.005 = +0.5% movement speed per point (100 AGI = +50% speed).",
                                                "Default: 0.005 | Range: 0.0 ~ 5.0")
                                .defineInRange("movementSpeedMultiplierPerPoint", 0.005D, 0.0D, 5.0D);

                AGI_EPICFIGHT_STAMINA_REGEN_PER_POINT = BUILDER
                                .comment(
                                                "[Soft Dependency: Epic Fight] Flat Stamina Regeneration Speed (epicfight:stamina_regen) added per 1 point of AGI.",
                                                "Default: 0.03 | Range: 0.0 ~ 100.0")
                                .defineInRange("epicFightStaminaRegenPerPoint", 0.03D, 0.0D, 100.0D);
                BUILDER.pop();

                // --- 6. Intelligence (INT) ---
                BUILDER.push("intelligence_int");
                INT_IRONS_SPELL_POWER_PER_POINT = BUILDER
                                .comment(
                                                "[Soft Dependency: Iron's Spells 'n Spellbooks] Spell Power multiplier (irons_spellbooks:spell_power) per 1 point of INT.",
                                                "Uses MULTIPLY_BASE: 0.02 = +2.0% Spell Power per point (100 INT = +200% Spell Power).",
                                                "Default: 0.02 | Range: 0.0 ~ 10.0")
                                .defineInRange("ironsSpellPowerMultiplierPerPoint", 0.02D, 0.0D, 10.0D);
                BUILDER.pop();

                // --- 7. Wisdom (WIS) ---
                BUILDER.push("wisdom_wis");
                WIS_IRONS_MAX_MANA_PER_POINT = BUILDER
                                .comment(
                                                "[Soft Dependency: Iron's Spells 'n Spellbooks] Flat Max Mana (irons_spellbooks:max_mana) added per 1 point of WIS.",
                                                "Default: 10.0 | Range: 0.0 ~ 10000.0")
                                .defineInRange("ironsMaxManaPerPoint", 10.0D, 0.0D, 10000.0D);

                WIS_IRONS_MANA_REGEN_PER_POINT = BUILDER
                                .comment(
                                                "[Soft Dependency: Iron's Spells 'n Spellbooks] Mana Regeneration multiplier (irons_spellbooks:mana_regeneration) per 1 point of WIS.",
                                                "Uses MULTIPLY_BASE: 0.005 = +0.5% Mana Regeneration per point (max +50% at level 100).",
                                                "Default: 0.005 | Range: 0.0 ~ 10.0")
                                .defineInRange("ironsManaRegenMultiplierPerPoint", 0.005D, 0.0D, 10.0D);
                BUILDER.pop();

                // --- 8. Luck (LUK) ---
                BUILDER.push("luck_luk");
                LUK_VANILLA_LUCK_PER_POINT = BUILDER
                                .comment(
                                                "Flat Vanilla Luck (minecraft:generic.luck) added per 1 point of LUK.",
                                                "Default: 0.5 | Range: 0.0 ~ 1000.0")
                                .defineInRange("vanillaLuckPerPoint", 0.5D, 0.0D, 1000.0D);

                LUK_CRIT_DROP_CHANCE_PER_POINT = BUILDER
                                .comment(
                                                "Custom Critical Mob Loot Drop chance added per 1 point of LUK (0.025 = +2.5% chance per point).",
                                                "Default: 0.025 | Range: 0.0 ~ 10.0")
                                .defineInRange("critDropChancePerPoint", 0.025D, 0.0D, 10.0D);
                BUILDER.pop();

                SPEC = BUILDER.build();
        }

        private RpgConfig() {
        }

        public static int getStatusPointsPerLevel() {
                return SPEC.isLoaded() ? STATUS_POINTS_PER_LEVEL.get() : 4;
        }

        public static int getMaxAttributeCap() {
                return SPEC.isLoaded() ? MAX_ATTRIBUTE_CAP.get() : 999;
        }

        public static double getExpCostBaseMultiplier() {
                return SPEC.isLoaded() ? EXP_COST_BASE_MULTIPLIER.get() : 3.0D;
        }

        public static double getExpCostPerLevelIncrement() {
                return SPEC.isLoaded() ? EXP_COST_PER_LEVEL_INCREMENT.get() : 0.2D;
        }

        public static int getStartingAttribute(RpgAttributeType type) {
                if (!SPEC.isLoaded() || type == null) {
                        return 0;
                }
                return switch (type) {
                        case CON -> STARTING_CONSTITUTION.get();
                        case STR -> STARTING_STRENGTH.get();
                        case RNG -> STARTING_RANGED.get();
                        case AGI -> STARTING_AGILITY.get();
                        case INT -> STARTING_INTELLIGENCE.get();
                        case WIS -> STARTING_WISDOM.get();
                        case LUK -> STARTING_LUCK.get();
                };
        }

        public static int getStartingConstitution() {
                return SPEC.isLoaded() ? STARTING_CONSTITUTION.get() : 0;
        }

        public static int getStartingStrength() {
                return SPEC.isLoaded() ? STARTING_STRENGTH.get() : 0;
        }

        public static int getStartingRanged() {
                return SPEC.isLoaded() ? STARTING_RANGED.get() : 0;
        }

        public static int getStartingAgility() {
                return SPEC.isLoaded() ? STARTING_AGILITY.get() : 0;
        }

        public static int getStartingIntelligence() {
                return SPEC.isLoaded() ? STARTING_INTELLIGENCE.get() : 0;
        }

        public static int getStartingWisdom() {
                return SPEC.isLoaded() ? STARTING_WISDOM.get() : 0;
        }

        public static int getStartingLuck() {
                return SPEC.isLoaded() ? STARTING_LUCK.get() : 0;
        }

        public static double getStrAttackDamage() {
                return SPEC.isLoaded() ? STR_ATTACK_DAMAGE_PER_POINT.get() : 0.5D;
        }

        public static double getStrAttackSpeedMultiplier() {
                return SPEC.isLoaded() ? STR_ATTACK_SPEED_MULTIPLIER_PER_POINT.get() : 0.002D;
        }

        public static double getStrEpicFightArmorNegation() {
                return SPEC.isLoaded() ? STR_EPICFIGHT_ARMOR_NEGATION_PER_POINT.get() : 0.2D;
        }

        public static double getRngRangedDamage() {
                return SPEC.isLoaded() ? RNG_RANGED_DAMAGE_PER_POINT.get() : 0.6D;
        }

        public static double getRngProjectileSpeed() {
                return SPEC.isLoaded() ? RNG_PROJECTILE_SPEED_PER_POINT.get() : 0.02D;
        }

        public static double getConMaxHealth() {
                return SPEC.isLoaded() ? CON_MAX_HEALTH_PER_POINT.get() : 2.0D;
        }

        public static double getConKnockbackResistance() {
                return SPEC.isLoaded() ? CON_KNOCKBACK_RESISTANCE_PER_POINT.get() : 0.008D;
        }

        public static double getAgiMovementSpeed() {
                return SPEC.isLoaded() ? AGI_MOVEMENT_SPEED_PER_POINT.get() : 0.005D;
        }

        public static double getAgiEpicFightStaminaRegen() {
                return SPEC.isLoaded() ? AGI_EPICFIGHT_STAMINA_REGEN_PER_POINT.get() : 0.03D;
        }

        public static double getIntSpellPowerMultiplier() {
                return SPEC.isLoaded() ? INT_IRONS_SPELL_POWER_PER_POINT.get() : 0.02D;
        }

        public static double getWisMaxMana() {
                return SPEC.isLoaded() ? WIS_IRONS_MAX_MANA_PER_POINT.get() : 10.0D;
        }

        public static double getWisManaRegenMultiplier() {
                return SPEC.isLoaded() ? WIS_IRONS_MANA_REGEN_PER_POINT.get() : 0.005D;
        }

        public static double getLukVanillaLuck() {
                return SPEC.isLoaded() ? LUK_VANILLA_LUCK_PER_POINT.get() : 0.5D;
        }

        public static double getLukCritDropChance() {
                return SPEC.isLoaded() ? LUK_CRIT_DROP_CHANCE_PER_POINT.get() : 0.025D;
        }
}
