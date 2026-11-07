package com.attributerpg.compat;

import com.attributerpg.config.RpgConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Isolated soft-dependency wrapper for "Epic Fight" (modid: epicfight).
 * Interacts with Epic Fight attributes via ForgeRegistries
 * so that missing Epic Fight classes never trigger NoClassDefFoundError at runtime.
 */
public final class EpicFightCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Epic Fight Attribute Registry Keys
    private static final ResourceLocation ARMOR_NEGATION_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.EPIC_FIGHT_MOD_ID, "armor_negation");
    private static final ResourceLocation OFFHAND_ARMOR_NEGATION_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.EPIC_FIGHT_MOD_ID, "offhand_armor_negation");
    private static final ResourceLocation STAMINA_REGEN_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.EPIC_FIGHT_MOD_ID, "stamina_regen");

    // Unique Modifier UUIDs for Epic Fight integration
    private static final UUID STR_ARMOR_NEGATION_UUID = UUID.fromString("a81b7410-32e1-49a4-9b1f-110000000001");
    private static final UUID STR_OFFHAND_ARMOR_NEGATION_UUID = UUID.fromString("a81b7410-32e1-49a4-9b1f-110000000002");
    private static final UUID AGI_STAMINA_REGEN_UUID = UUID.fromString("a81b7410-32e1-49a4-9b1f-110000000004");

    private EpicFightCompat() {
    }

    /**
     * Applies Strength (Armor Negation) and Agility (Stamina Regen Speed) modifiers
     * to the player if Epic Fight is loaded, using values from {@link RpgConfig}.
     * Note: Feature +1 Max Stamina has been removed as requested.
     */
    public static void applyModifiers(Player player, int strPoints, int agiPoints) {
        if (!ModCompat.isEpicFightLoaded()) {
            return;
        }

        try {
            double armorNegationPerPoint = RpgConfig.getStrEpicFightArmorNegation();
            double staminaRegenPerPoint = RpgConfig.getAgiEpicFightStaminaRegen();

            // 1. Strength -> Epic Fight Armor Negation (Mainhand & Offhand)
            applySingleModifier(
                    player,
                    ARMOR_NEGATION_ID,
                    STR_ARMOR_NEGATION_UUID,
                    "AttributeRPG STR Armor Negation",
                    strPoints * armorNegationPerPoint,
                    AttributeModifier.Operation.ADDITION
            );
            applySingleModifier(
                    player,
                    OFFHAND_ARMOR_NEGATION_ID,
                    STR_OFFHAND_ARMOR_NEGATION_UUID,
                    "AttributeRPG STR Offhand Armor Negation",
                    strPoints * armorNegationPerPoint,
                    AttributeModifier.Operation.ADDITION
            );

            // 2. Agility -> Epic Fight Stamina Regeneration Speed
            applySingleModifier(
                    player,
                    STAMINA_REGEN_ID,
                    AGI_STAMINA_REGEN_UUID,
                    "AttributeRPG AGI Stamina Regen",
                    agiPoints * staminaRegenPerPoint,
                    AttributeModifier.Operation.ADDITION
            );
        } catch (Throwable t) {
            LOGGER.warn("Failed to apply Epic Fight attribute modifiers safely: {}", t.getMessage());
        }
    }

    private static void applySingleModifier(
            Player player,
            ResourceLocation attributeId,
            UUID modifierUuid,
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(modifierUuid);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(modifierUuid, name, amount, operation));
        }
    }
}
