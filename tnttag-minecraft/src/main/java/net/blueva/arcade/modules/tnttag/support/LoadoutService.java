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
    }

    public void restorePlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        restorePlayerHealthAndHunger(player);
        applyStartingEffects(player);
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
        List<String> startingEffects = moduleConfig.getStringList("effects.starting_effects");

        if (startingEffects == null || startingEffects.isEmpty()) {
            return;
        }

        for (String effectString : startingEffects) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 3) {
                    PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                    int duration = Integer.parseInt(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);

                    if (effectType != null) {
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                    }
                }
            } catch (Exception e) {
                // Ignore malformed entries
            }
        }
    }
}
