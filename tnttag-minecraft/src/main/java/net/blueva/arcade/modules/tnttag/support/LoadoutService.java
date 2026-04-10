package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class LoadoutService {

    private final ModuleConfigAPI moduleConfig;

    public LoadoutService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void prepareForStart(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        restorePlayerHealthAndHunger(player);
        giveStartingItems(player);
        applyStartingEffects(player);
        applyUntaggedEffects(player);
    }

    public void restorePlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        restorePlayerHealthAndHunger(player);
        clearTransitionEffects(player);
        applyStartingEffects(player);
        applyUntaggedEffects(player);
    }

    public void applyTaggedState(Player player) {
        clearTransitionEffects(player);
        applyStartingEffects(player);
        applyTaggedEffects(player);
    }

    public void applyUntaggedState(Player player) {
        clearTransitionEffects(player);
        applyStartingEffects(player);
        applyUntaggedEffects(player);
    }

    public void clearTransitionEffects(Player player) {
        clearTaggedEffects(player);
        clearUntaggedEffects(player);
    }

    public void applyTaggedEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.tagged_effects"));
    }

    public void clearTaggedEffects(Player player) {
        clearEffects(player, moduleConfig.getStringList("effects.tagged_effects"));
    }

    public void applyUntaggedEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.untagged_effects"));
    }

    public void clearUntaggedEffects(Player player) {
        clearEffects(player, moduleConfig.getStringList("effects.untagged_effects"));
    }

    private void restorePlayerHealthAndHunger(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
    }

    private void giveStartingItems(Player player) {
        List<String> startingItems = moduleConfig.getStringList("items.starting_items");

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(material, amount);

                    if (slot == 40) {
                        inventory.setItemInOffHand(item);
                    } else if (slot == 39) {
                        inventory.setHelmet(item);
                    } else if (slot == 38) {
                        inventory.setChestplate(item);
                    } else if (slot == 37) {
                        inventory.setLeggings(item);
                    } else if (slot == 36) {
                        inventory.setBoots(item);
                    } else if (slot >= 0 && slot < 36) {
                        inventory.setItem(slot, item);
                    } else {
                        inventory.addItem(item);
                    }
                }
            } catch (Exception e) {
                // Ignore malformed entries
            }
        }
    }

    private void applyStartingEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.starting_effects"));
    }

    private void applyEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (String effectString : effects) {
            try {
                PotionEffect effect = parseEffect(effectString);
                if (effect != null) {
                    player.addPotionEffect(effect);
                }
            } catch (Exception e) {
                // Ignore malformed entries
            }
        }
    }

    private void clearEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (String effectString : effects) {
            try {
                PotionEffectType effectType = parseEffectType(effectString);
                if (effectType != null) {
                    player.removePotionEffect(effectType);
                }
            } catch (Exception e) {
                // Ignore malformed entries
            }
        }
    }

    private PotionEffect parseEffect(String effectString) {
        String[] parts = effectString.split(":");
        if (parts.length < 3) {
            return null;
        }

        PotionEffectType effectType = parseEffectType(effectString);
        if (effectType == null) {
            return null;
        }

        int duration = Integer.parseInt(parts[1]);
        int amplifier = Integer.parseInt(parts[2]);
        return new PotionEffect(effectType, duration, amplifier, false, false);
    }

    private PotionEffectType parseEffectType(String effectString) {
        String[] parts = effectString.split(":");
        if (parts.length == 0) {
            return null;
        }

        return PotionEffectType.getByName(parts[0].toUpperCase());
    }
}
