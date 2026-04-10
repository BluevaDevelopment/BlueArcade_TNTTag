package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.List;

public class LoadoutService {

    private final ModuleConfigAPI moduleConfig;

    public LoadoutService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void prepareForStart(Player player) {
        clearInventory(player);
        giveStartingItems(player);
        applyStartingEffects(player);
        applyUntaggedEffects(player);
    }

    public void restorePlayer(Player player) {
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

    public void clearInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        clearContainer(inventory.getArmor());
        clearContainer(inventory.getHotbar());
        clearContainer(inventory.getStorage());
        clearContainer(inventory.getUtility());
        clearContainer(inventory.getTools());
        clearContainer(inventory.getBackpack());
    }

    private void giveStartingItems(Player player) {
        List<String> startingItems = moduleConfig.getStringList("items.starting_items");

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    String itemId = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    addItem(player, new ItemStack(itemId, amount), slot);
                }
            } catch (Exception ignored) {
                // Ignore malformed entries
            }
        }
    }

    private void applyStartingEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.starting_effects"));
    }

    private void applyTaggedEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.tagged_effects"));
    }

    private void clearTaggedEffects(Player player) {
        clearEffects(player, moduleConfig.getStringList("effects.tagged_effects"));
    }

    private void applyUntaggedEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.untagged_effects"));
    }

    private void clearUntaggedEffects(Player player) {
        clearEffects(player, moduleConfig.getStringList("effects.untagged_effects"));
    }

    private void applyEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        // Status effect application is not yet available in the Hytale runtime.
    }

    private void clearEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        // Status effect removal is not yet available in the Hytale runtime.
    }

    private void addItem(Player player, ItemStack item, int slot) {
        if (player == null || item == null || player.getInventory() == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (slot >= 0) {
            ItemContainer hotbar = inventory.getHotbar();
            if (slot < 9 && hotbar != null) {
                hotbar.addItemStackToSlot((short) slot, item);
                return;
            }

            ItemContainer storage = inventory.getStorage();
            if (storage != null) {
                short storageSlot = (short) Math.max(0, slot - 9);
                if (storageSlot < storage.getCapacity()) {
                    storage.addItemStackToSlot(storageSlot, item);
                } else {
                    storage.addItemStack(item);
                }
            }
            return;
        }

        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.addItemStack(item);
        }
    }

    private void clearContainer(ItemContainer container) {
        if (container != null) {
            container.clear();
        }
    }
}
