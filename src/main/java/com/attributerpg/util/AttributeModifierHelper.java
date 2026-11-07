package com.attributerpg.util;

import com.attributerpg.attribute.ModAttributes;
import com.attributerpg.attribute.RpgAttributeType;
import com.attributerpg.capability.IRpgStats;
import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.compat.EpicFightCompat;
import com.attributerpg.compat.IronsSpellbooksCompat;
import com.attributerpg.config.RpgConfig;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Synchronizes a player's allocated RPG Attributes with Vanilla, Custom, Epic Fight,
 * and Iron's Spells 'n Spellbooks Entity Attributes using configurable values from {@link RpgConfig}.
 */
public final class AttributeModifierHelper {

    // Deterministic UUIDs for Vanilla & Custom RPG Attribute Modifiers
    private static final UUID STR_ATTACK_DAMAGE_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000001");
    private static final UUID STR_ATTACK_SPEED_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000009");
    private static final UUID RNG_DAMAGE_BONUS_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000002");
    private static final UUID RNG_PROJECTILE_SPEED_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000003");
    private static final UUID CON_MAX_HEALTH_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000004");
    private static final UUID CON_KNOCKBACK_RESIST_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000008");
    private static final UUID AGI_MOVEMENT_SPEED_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000005");
    private static final UUID LUK_VANILLA_LUCK_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000006");
    private static final UUID LUK_CRIT_DROP_UUID = UUID.fromString("c10a1001-1111-4000-8000-000000000007");

    private AttributeModifierHelper() {
    }

    /**
     * Recomputes and applies all RPG attribute modifiers for the given player.
     */
    public static void applyAllModifiers(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> applyAllModifiers(player, stats));
    }

    /**
     * Recomputes and applies all RPG attribute modifiers using the provided {@link IRpgStats}.
     */
    public static void applyAllModifiers(Player player, IRpgStats stats) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        int str = stats.getAttributeValue(RpgAttributeType.STR);
        int rng = stats.getAttributeValue(RpgAttributeType.RNG);
        int con = stats.getAttributeValue(RpgAttributeType.CON);
        int agi = stats.getAttributeValue(RpgAttributeType.AGI);
        int intel = stats.getAttributeValue(RpgAttributeType.INT);
        int wis = stats.getAttributeValue(RpgAttributeType.WIS);
        int luk = stats.getAttributeValue(RpgAttributeType.LUK);

        // 1. Strength (STR) -> Vanilla Attack Damage & Attack Speed
        applyModifier(
                player,
                Attributes.ATTACK_DAMAGE,
                STR_ATTACK_DAMAGE_UUID,
                "AttributeRPG STR Attack Damage",
                str * RpgConfig.getStrAttackDamage(),
                AttributeModifier.Operation.ADDITION
        );

        // Strength (STR) -> Vanilla Attack Speed (+0.5% per point = +50% at level 100)
        applyModifier(
                player,
                Attributes.ATTACK_SPEED,
                STR_ATTACK_SPEED_UUID,
                "AttributeRPG STR Attack Speed",
                str * RpgConfig.getStrAttackSpeedMultiplier(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        // 2. Ranged (RNG) -> Custom Ranged Damage Bonus & Projectile Speed
        applyModifier(
                player,
                ModAttributes.RANGED_DAMAGE_BONUS.get(),
                RNG_DAMAGE_BONUS_UUID,
                "AttributeRPG RNG Ranged Damage",
                rng * RpgConfig.getRngRangedDamage(),
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player,
                ModAttributes.PROJECTILE_SPEED_BONUS.get(),
                RNG_PROJECTILE_SPEED_UUID,
                "AttributeRPG RNG Projectile Speed",
                rng * RpgConfig.getRngProjectileSpeed(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        // 3. Constitution (CON) -> Vanilla Max Health & Knockback Resistance
        float previousMaxHealth = player.getMaxHealth();
        applyModifier(
                player,
                Attributes.MAX_HEALTH,
                CON_MAX_HEALTH_UUID,
                "AttributeRPG CON Max Health",
                con * RpgConfig.getConMaxHealth(),
                AttributeModifier.Operation.ADDITION
        );
        float newMaxHealth = player.getMaxHealth();
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        } else if (newMaxHealth > previousMaxHealth && player.isAlive()) {
            player.heal(newMaxHealth - previousMaxHealth);
        }

        applyModifier(
                player,
                Attributes.KNOCKBACK_RESISTANCE,
                CON_KNOCKBACK_RESIST_UUID,
                "AttributeRPG CON Knockback Resistance",
                con * RpgConfig.getConKnockbackResistance(),
                AttributeModifier.Operation.ADDITION
        );

        // 4. Agility (AGI) -> Vanilla Movement Speed
        applyModifier(
                player,
                Attributes.MOVEMENT_SPEED,
                AGI_MOVEMENT_SPEED_UUID,
                "AttributeRPG AGI Movement Speed",
                agi * RpgConfig.getAgiMovementSpeed(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        // 5. Luck (LUK) -> Vanilla Luck & Custom Critical Loot Drop Rate
        applyModifier(
                player,
                Attributes.LUCK,
                LUK_VANILLA_LUCK_UUID,
                "AttributeRPG LUK Vanilla Luck",
                luk * RpgConfig.getLukVanillaLuck(),
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player,
                ModAttributes.CRIT_DROP_MULTIPLIER.get(),
                LUK_CRIT_DROP_UUID,
                "AttributeRPG LUK Critical Drop Rate",
                luk * RpgConfig.getLukCritDropChance(),
                AttributeModifier.Operation.ADDITION
        );

        // 6. Soft Dependency: Epic Fight (STR -> Armor Negation, AGI -> Stamina Regen)
        EpicFightCompat.applyModifiers(player, str, agi);

        // 7. Soft Dependency: Iron's Spells 'n Spellbooks (INT -> Spell Power, WIS -> Max Mana & Mana Regen)
        IronsSpellbooksCompat.applyModifiers(player, intel, wis);
    }

    private static void applyModifier(
            Player player,
            Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }
}
