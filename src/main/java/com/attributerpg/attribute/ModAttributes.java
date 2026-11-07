package com.attributerpg.attribute;

import com.attributerpg.AttributeRpgMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom RPG attributes to ForgeRegistries.ATTRIBUTES and attaches them to Player entities.
 */
@Mod.EventBusSubscriber(modid = AttributeRpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AttributeRpgMod.MODID);

    public static final RegistryObject<Attribute> RANGED_DAMAGE_BONUS = ATTRIBUTES.register(
            "ranged_damage_bonus",
            () -> new RangedAttribute("attribute.name.attributerpg.ranged_damage_bonus", 0.0D, 0.0D, 1024.0D).setSyncable(true)
    );

    public static final RegistryObject<Attribute> PROJECTILE_SPEED_BONUS = ATTRIBUTES.register(
            "projectile_speed_bonus",
            () -> new RangedAttribute("attribute.name.attributerpg.projectile_speed_bonus", 1.0D, 1.0D, 10.0D).setSyncable(true)
    );

    public static final RegistryObject<Attribute> CRIT_DROP_MULTIPLIER = ATTRIBUTES.register(
            "crit_drop_multiplier",
            () -> new RangedAttribute("attribute.name.attributerpg.crit_drop_multiplier", 0.0D, 0.0D, 10.0D).setSyncable(true)
    );

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, RANGED_DAMAGE_BONUS.get())) {
            event.add(EntityType.PLAYER, RANGED_DAMAGE_BONUS.get());
        }
        if (!event.has(EntityType.PLAYER, PROJECTILE_SPEED_BONUS.get())) {
            event.add(EntityType.PLAYER, PROJECTILE_SPEED_BONUS.get());
        }
        if (!event.has(EntityType.PLAYER, CRIT_DROP_MULTIPLIER.get())) {
            event.add(EntityType.PLAYER, CRIT_DROP_MULTIPLIER.get());
        }
    }
}

