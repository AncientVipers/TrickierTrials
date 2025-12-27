package de.t14d3.trickiertrials;

import org.apache.commons.lang3.RandomUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.TrialSpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class TrialSpawnerListener implements Listener {

    private final boolean strengthenTrialMobs;
    private final boolean glowingEffect;
    private final boolean secret;
    private final String secretName;

    private static final NamespacedKey TRIAL_SPAWNED_KEY = new NamespacedKey("trickiertrials", "trialspawned");
    private static final NamespacedKey TRIAL_TIER_KEY = new NamespacedKey("trickiertrials", "trialtier");

    public TrialSpawnerListener(JavaPlugin plugin, boolean strengthenTrialMobs, boolean glowingEffect, boolean secret, String secretName) {
        this.strengthenTrialMobs = strengthenTrialMobs;
        this.glowingEffect = glowingEffect;
        this.secret = secret;
        this.secretName = secretName;
    }

    public enum TrialTiers {
        DEFAULT,
        NORMAL,
        HARD,
        EXTREME
    }

    public void healthMultiplier(LivingEntity entity, double healthMultiplier) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMultiplier);
        entity.setHealth(maxHealth.getValue());
    }

    public void damageMultiplier(LivingEntity entity, double damageMultiplier) {
        AttributeInstance attackDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        attackDamage.setBaseValue(attackDamage.getBaseValue() * damageMultiplier);
    }

    public void assignArmorBasedOnDifficulty(LivingEntity entity, TrialTiers trialTiers) {
        Random random = new Random();

        switch (trialTiers) {
            case EXTREME:
                assignArmorPiece(entity, 0.80, 0.50, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, "helmet");
                assignArmorPiece(entity, 0.80, 0.50, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, "chestplate");
                assignArmorPiece(entity, 0.80, 0.50, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS, "leggings");
                assignArmorPiece(entity, 0.80, 0.50, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS, "boots");
                break;

            case HARD:
                assignArmorPiece(entity, 0.60, 0.05, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.LEATHER_HELMET, 0.20, "helmet");
                assignArmorPiece(entity, 0.60, 0.05, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.LEATHER_CHESTPLATE, 0.20, "chestplate");
                assignArmorPiece(entity, 0.60, 0.05, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.LEATHER_LEGGINGS, 0.20, "leggings");
                assignArmorPiece(entity, 0.60, 0.05, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.LEATHER_BOOTS, 0.20, "boots");
                break;

            case NORMAL:
                assignArmorPiece(entity, 0.20, 0.10, Material.LEATHER_HELMET, Material.IRON_HELMET, "helmet");
                assignArmorPiece(entity, 0.20, 0.10, Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, "chestplate");
                assignArmorPiece(entity, 0.20, 0.10, Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS, "leggings");
                assignArmorPiece(entity, 0.20, 0.10, Material.LEATHER_BOOTS, Material.IRON_BOOTS, "boots");
                break;

            case DEFAULT:
            default:
                assignArmorPiece(entity, 0.25, Material.LEATHER_HELMET, "helmet");
                assignArmorPiece(entity, 0.25, Material.LEATHER_CHESTPLATE, "chestplate");
                assignArmorPiece(entity, 0.25, Material.LEATHER_LEGGINGS, "leggings");
                assignArmorPiece(entity, 0.25, Material.LEATHER_BOOTS, "boots");
                break;
        }
    }

    public void assignArmorPiece(LivingEntity entity, double primaryChance, double secondaryChance, Material primaryArmor, Material secondaryArmor, String armorSlot) {
        Random random = new Random();
        if (random.nextDouble() < primaryChance) {
            equipArmor(entity, primaryArmor, armorSlot);
        } else if (random.nextDouble() < secondaryChance) {
            equipArmor(entity, secondaryArmor, armorSlot);
        }
    }

    public void assignArmorPiece(LivingEntity entity, double primaryChance, double secondaryChance, Material primaryArmor, Material secondaryArmor, Material tertiaryArmor, double tertiaryChance, String armorSlot) {
        Random random = new Random();
        if (random.nextDouble() < primaryChance) {
            equipArmor(entity, primaryArmor, armorSlot);
        } else if (random.nextDouble() < secondaryChance) {
            equipArmor(entity, secondaryArmor, armorSlot);
        } else if (random.nextDouble() < tertiaryChance) {
            equipArmor(entity, tertiaryArmor, armorSlot);
        }
    }

    public void assignArmorPiece(LivingEntity entity, double chance, Material armor, String armorSlot) {
        Random random = new Random();
        if (random.nextDouble() < chance) {
            equipArmor(entity, armor, armorSlot);
        }
    }

    public void equipArmor(LivingEntity entity, Material armor, String armorSlot) {
        switch (armorSlot) {
            case "helmet":
                entity.getEquipment().setHelmet(new ItemStack(armor));
                break;
            case "chestplate":
                entity.getEquipment().setChestplate(new ItemStack(armor));
                break;
            case "leggings":
                entity.getEquipment().setLeggings(new ItemStack(armor));
                break;
            case "boots":
                entity.getEquipment().setBoots(new ItemStack(armor));
                break;
        }
    }

    private void assignRandomSword(LivingEntity entity) {
        Random random = new Random();
        if (entity.getEquipment().getItemInMainHand().getType() == Material.AIR) { // Check if the main hand is empty
            double chance = random.nextDouble();
            if (chance < 0.20) {
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            } else if (chance < 0.30) {
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            } else if (chance < 0.40) {
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            } else {
                return;
            }
        }
    }

    @EventHandler
    public void onSpawn(TrialSpawnerSpawnEvent event) {
        if (!strengthenTrialMobs) {
            return;
        }

        final int[] gearScore = {0};
        event.getTrialSpawner().getTrackedPlayers().forEach(player -> {
                    Arrays.stream(player.getEquipment().getArmorContents())
                            .filter(Objects::nonNull)
                            .forEach(itemStack -> {
                                switch (itemStack.getType()) {
                                    case ELYTRA:
                                        gearScore[0] += 10;
                                        break;
                                    case NETHERITE_CHESTPLATE:
                                    case NETHERITE_HELMET:
                                    case NETHERITE_LEGGINGS:
                                    case NETHERITE_BOOTS:
                                        gearScore[0] += 5;
                                        break;
                                    case DIAMOND_CHESTPLATE:
                                    case DIAMOND_HELMET:
                                    case DIAMOND_LEGGINGS:
                                    case DIAMOND_BOOTS:
                                        gearScore[0] += 4;
                                        break;
                                    case IRON_CHESTPLATE:
                                    case IRON_HELMET:
                                    case IRON_LEGGINGS:
                                    case IRON_BOOTS:
                                    case CHAINMAIL_CHESTPLATE:
                                    case CHAINMAIL_HELMET:
                                    case CHAINMAIL_LEGGINGS:
                                    case CHAINMAIL_BOOTS:
                                        gearScore[0] += 3;
                                        break;
                                    case GOLDEN_CHESTPLATE:
                                    case GOLDEN_HELMET:
                                    case GOLDEN_LEGGINGS:
                                    case GOLDEN_BOOTS:
                                        gearScore[0] += 2;
                                        break;
                                    case LEATHER_CHESTPLATE:
                                    case LEATHER_HELMET:
                                    case LEATHER_LEGGINGS:
                                    case LEATHER_BOOTS:
                                        gearScore[0] += 1;
                                        break;
                                    default:
                                        break;
                                }
                            });
                    Arrays.stream(player.getInventory().getContents())
                            .filter(Objects::nonNull)
                            .forEach(itemStack -> {
                                switch (itemStack.getType()) {

                                    case NETHERITE_SWORD:
                                    case NETHERITE_AXE:
                                    case NETHERITE_PICKAXE:
                                    case NETHERITE_SHOVEL:
                                        gearScore[0] += 10;
                                        break;
                                    case DIAMOND_SWORD:
                                    case DIAMOND_AXE:
                                    case DIAMOND_PICKAXE:
                                    case DIAMOND_SHOVEL:
                                        gearScore[0] += 5;
                                        break;
                                }
                            });
                });

        TrialTiers trialTiers;
        if (gearScore[0] >= 35) {
            trialTiers = TrialTiers.EXTREME;
        } else if (gearScore[0] >= 20) {
            trialTiers = TrialTiers.HARD;
        } else if (gearScore[0] >= 10) {
            trialTiers = TrialTiers.NORMAL;
        } else {
            trialTiers = TrialTiers.DEFAULT;
        }

        if (glowingEffect) {
            event.getEntity().setGlowing(true);
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        switch (trialTiers) {
            case NORMAL:
                healthMultiplier(entity, 1.1);
                assignArmorBasedOnDifficulty(entity, TrialTiers.NORMAL);
                break;

            case HARD:
                healthMultiplier(entity, 1.5);
                damageMultiplier(entity, 2);
                assignArmorBasedOnDifficulty(entity, TrialTiers.HARD);
                break;

            case EXTREME:
                healthMultiplier(entity, 2);
                damageMultiplier(entity, 3);
                assignArmorBasedOnDifficulty(entity, TrialTiers.EXTREME);
                break;

            default:
                assignArmorBasedOnDifficulty(entity, TrialTiers.DEFAULT);
                break;
        }

        // Randomly assign a sword if the entity has no item in the main hand
        assignRandomSword(entity);

        // Tag the mob so we can apply special drops later.
        entity.getPersistentDataContainer().set(TRIAL_SPAWNED_KEY, PersistentDataType.INTEGER, 1);

        // Store the determined tier so rewards can be configured per tier.
        entity.getPersistentDataContainer().set(TRIAL_TIER_KEY, PersistentDataType.STRING, trialTiers.name());


        // Easter Egg
        if (secret && entity instanceof Breeze && RandomUtils.nextDouble(0, 1) < 0.05) {
            entity.setCustomName(secretName);
            entity.setCustomNameVisible(true);
            healthMultiplier(entity, 4);
            entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(1.2);
        }
    }
}