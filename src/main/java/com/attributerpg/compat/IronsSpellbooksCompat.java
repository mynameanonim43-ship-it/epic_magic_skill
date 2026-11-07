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
 * Isolated soft-dependency wrapper for "Iron's Spells 'n Spellbooks" (modid: irons_spellbooks).
 * Modifies Spell Power, Max Mana, and Mana Regeneration via ForgeRegistries without hard class links.
 */
public final class IronsSpellbooksCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Iron's Spells 'n Spellbooks Attribute Registry Keys
    private static final ResourceLocation SPELL_POWER_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.IRONS_SPELLBOOKS_MOD_ID, "spell_power");
    private static final ResourceLocation MAX_MANA_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.IRONS_SPELLBOOKS_MOD_ID, "max_mana");
    private static final ResourceLocation MANA_REGEN_ID = ResourceLocation.fromNamespaceAndPath(ModCompat.IRONS_SPELLBOOKS_MOD_ID, "mana_regeneration");

    // Unique Modifier UUIDs for Iron's Spells integration
    private static final UUID INT_SPELL_POWER_UUID = UUID.fromString("b92c8521-43f2-4ab5-8c2e-220000000001");
    private static final UUID WIS_MAX_MANA_UUID = UUID.fromString("b92c8521-43f2-4ab5-8c2e-220000000002");
    private static final UUID WIS_MANA_REGEN_UUID = UUID.fromString("b92c8521-43f2-4ab5-8c2e-220000000003");

    private IronsSpellbooksCompat() {
    }

    /**
     * Applies Intelligence (Spell Power multiplier) and Wisdom (Max Mana & Mana Regeneration)
     * modifiers to the player if Iron's Spells 'n Spellbooks is loaded, using values from {@link RpgConfig}.
     */
    public static void applyModifiers(Player player, int intPoints, int wisPoints) {
        if (!ModCompat.isIronsSpellbooksLoaded()) {
            return;
        }

        try {
            double spellPowerPerInt = RpgConfig.getIntSpellPowerMultiplier();
            double maxManaPerWis = RpgConfig.getWisMaxMana();
            double manaRegenPerWis = RpgConfig.getWisManaRegenMultiplier();

            // 1. Intelligence -> Multiplies Iron's Spells Spell Power (irons_spellbooks:spell_power)
            applySingleModifier(
                    player,
                    SPELL_POWER_ID,
                    INT_SPELL_POWER_UUID,
                    "AttributeRPG INT Spell Power",
                    intPoints * spellPowerPerInt,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );

            // 2. Wisdom -> Increases Iron's Spells Max Mana (irons_spellbooks:max_mana)
            applySingleModifier(
                    player,
                    MAX_MANA_ID,
                    WIS_MAX_MANA_UUID,
                    "AttributeRPG WIS Max Mana",
                    wisPoints * maxManaPerWis,
                    AttributeModifier.Operation.ADDITION
            );

            // 3. Wisdom -> Increases Iron's Spells Mana Regeneration (irons_spellbooks:mana_regeneration)
            applySingleModifier(
                    player,
                    MANA_REGEN_ID,
                    WIS_MANA_REGEN_UUID,
                    "AttributeRPG WIS Mana Regeneration",
                    wisPoints * manaRegenPerWis,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
        } catch (Throwable t) {
            LOGGER.warn("Failed to apply Iron's Spells 'n Spellbooks attribute modifiers safely: {}", t.getMessage());
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
