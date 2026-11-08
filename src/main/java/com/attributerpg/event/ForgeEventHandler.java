package com.attributerpg.event;

import com.attributerpg.AttributeRpgMod;
import com.attributerpg.attribute.RpgAttributeType;
import com.attributerpg.capability.IRpgStats;
import com.attributerpg.capability.RpgStatsProvider;
import com.attributerpg.config.RpgConfig;
import com.attributerpg.network.ModNetworking;
import com.attributerpg.util.AttributeModifierHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Forge gameplay events for:
 * 1. Independent RPG Level & EXP accumulation (mining, smelting, fishing, and killing mobs).
 * 2. Capability attachment, cloning, and synchronization.
 * 3. Ranged (RNG) arrow/projectile speed scaling and ranged attack damage bonus.
 * 4. Luck (LUK) critical mob loot drop rate multiplier.
 */
@Mod.EventBusSubscriber(modid = AttributeRpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {

    private static final String PROJECTILE_BOOSTED_TAG = "attributerpg_rng_boosted";

    /**
     * Attaches the persistent {@link RpgStatsProvider} capability to all Player entities.
     */
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(RpgStatsProvider.RPG_STATS).isPresent()) {
                RpgStatsProvider provider = new RpgStatsProvider();
                event.addCapability(RpgStatsProvider.IDENTIFIER, provider);
                event.addListener(provider::invalidate);
            }
        }
    }

    /**
     * Hooks directly into PlayerXpEvent.XpChange.
     * Every time a player earns experience points from ANY source (mining, smelting, fishing, killing mobs),
     * the exact amount is added to the player's independent RPG Experience.
     * RPG Level never resets upon player death or when spending levels at an Enchanting Table or Anvil!
     */
    @SubscribeEvent
    public static void onPlayerXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getAmount() <= 0) {
            return;
        }

        player.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
            int levelsGained = stats.addRpgExp(event.getAmount());
            if (levelsGained > 0 && player instanceof ServerPlayer serverPlayer) {
                int pointsAwarded = levelsGained * RpgConfig.getStatusPointsPerLevel();
                serverPlayer.displayClientMessage(
                        Component.literal("[RPG] ")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                .append(Component.literal("RPG Level Up! Now Lv. " + stats.getRpgLevel() + " (+" + pointsAwarded + " Points) ")
                                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                .append(Component.literal("(Press 'O' to allocate)")
                                        .withStyle(ChatFormatting.GRAY)),
                        false
                );

                serverPlayer.level().playSound(
                        null,
                        serverPlayer.getX(),
                        serverPlayer.getY(),
                        serverPlayer.getZ(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.75F,
                        1.0F
                );
            }

            if (player instanceof ServerPlayer serverPlayer) {
                ModNetworking.syncRpgData(serverPlayer);
            }
        });
    }

    /**
     * Preserves capability data (RPG Level, RPG Exp, Status Points, and Allocated Attributes)
     * across player deaths and End Portal transitions.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.reviveCaps();
        try {
            original.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(oldStats ->
                    newPlayer.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(newStats -> {
                        newStats.copyFrom(oldStats);
                    })
            );
        } finally {
            original.invalidateCaps();
        }

        // Apply attribute modifiers and synchronize to client
        if (newPlayer instanceof ServerPlayer serverPlayer) {
            newPlayer.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                AttributeModifierHelper.applyAllModifiers(serverPlayer, stats);
            });
            ModNetworking.syncRpgData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                AttributeModifierHelper.applyAllModifiers(serverPlayer, stats);
            });
            ModNetworking.syncRpgData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AttributeModifierHelper.applyAllModifiers(serverPlayer);
            ModNetworking.syncRpgData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AttributeModifierHelper.applyAllModifiers(serverPlayer);
            ModNetworking.syncRpgData(serverPlayer);
        }
    }

    /**
     * Ranged (RNG) Integration - Part 1:
     * Increases projectile speed when a player shoots an arrow or projectile.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player shooter) {
            if (projectile.getPersistentData().getBoolean(PROJECTILE_BOOSTED_TAG)) {
                return;
            }

            shooter.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                int rngPoints = stats.getAttributeValue(RpgAttributeType.RNG);
                if (rngPoints > 0) {
                    double speedMultiplier = 1.0D + (rngPoints * RpgConfig.getRngProjectileSpeed());
                    Vec3 currentVelocity = projectile.getDeltaMovement();
                    projectile.setDeltaMovement(currentVelocity.scale(speedMultiplier));
                    projectile.hasImpulse = true;
                    projectile.getPersistentData().putBoolean(PROJECTILE_BOOSTED_TAG, true);
                }
            });
        }
    }

    /**
     * Ranged (RNG) Integration - Part 2:
     * Increases Ranged Attack Damage dealt by Arrow and Projectile entities fired by the player.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity directEntity = event.getSource().getDirectEntity();
        Entity causingEntity = event.getSource().getEntity();

        if ((directEntity instanceof AbstractArrow || directEntity instanceof Projectile)
                && causingEntity instanceof Player shooter) {
            shooter.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
                int rngPoints = stats.getAttributeValue(RpgAttributeType.RNG);
                if (rngPoints > 0) {
                    float bonusDamage = (float) (rngPoints * RpgConfig.getRngRangedDamage());
                    event.setAmount(event.getAmount() + bonusDamage);
                }
            });
        }
    }

    /**
     * Luck (LUK) Integration:
     * Custom critical drop rate multiplier for mob loot when a player kills a non-player mob.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || target instanceof Player) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }

        killer.getCapability(RpgStatsProvider.RPG_STATS).ifPresent(stats -> {
            int lukPoints = stats.getAttributeValue(RpgAttributeType.LUK);
            if (lukPoints <= 0 || event.getDrops().isEmpty()) {
                return;
            }

            double totalCritChance = lukPoints * RpgConfig.getLukCritDropChance();
            int bonusMultiplier = (int) totalCritChance;
            double remainderChance = totalCritChance - bonusMultiplier;

            if (target.getRandom().nextDouble() < remainderChance) {
                bonusMultiplier += 1;
            }

            if (bonusMultiplier > 0) {
                List<ItemEntity> extraDrops = new ArrayList<>();
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack originalStack = drop.getItem();
                    if (!originalStack.isEmpty() && originalStack.getMaxStackSize() > 1) {
                        for (int i = 0; i < bonusMultiplier; i++) {
                            ItemStack copy = originalStack.copy();
                            ItemEntity extraEntity = new ItemEntity(
                                    target.level(),
                                    drop.getX(),
                                    drop.getY(),
                                    drop.getZ(),
                                    copy
                            );
                            extraEntity.setDefaultPickUpDelay();
                            extraDrops.add(extraEntity);
                        }
                    }
                }

                if (!extraDrops.isEmpty()) {
                    event.getDrops().addAll(extraDrops);
                    if (target.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.HAPPY_VILLAGER,
                                target.getX(),
                                target.getY() + target.getBbHeight() * 0.5D,
                                target.getZ(),
                                12,
                                0.35D,
                                0.35D,
                                0.35D,
                                0.05D
                        );
                        serverLevel.playSound(
                                null,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                SoundEvents.PLAYER_LEVELUP,
                                SoundSource.PLAYERS,
                                0.35F,
                                1.75F
                        );
                    }
                }
            }
        });
    }

    /**
     * Mod Event Bus subscriber for registering the custom {@link IRpgStats} capability.
     */
    @Mod.EventBusSubscriber(modid = AttributeRpgMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IRpgStats.class);
        }
    }
}
