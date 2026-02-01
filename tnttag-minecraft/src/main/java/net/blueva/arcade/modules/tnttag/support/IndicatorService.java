package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.modules.tnttag.state.ArenaRuntime;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IndicatorService {

    private final ModuleConfigAPI moduleConfig;
    private final Map<UUID, Map<Integer, ItemStack>> storedSlots = new ConcurrentHashMap<>();

    public IndicatorService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void applyIndicator(Player player, ArenaRuntime runtime) {
        boolean enabled = moduleConfig.getBoolean("tagging.inventory_indicator.enabled", true);
        if (!enabled) {
            return;
        }

        int tntSlot = moduleConfig.getInt("tagging.inventory_indicator.tnt_slot", 4);
        List<Integer> woolSlots = getWoolSlots();

        ItemStack tntStack = new ItemStack(Material.TNT, 1);
        ItemStack primaryWool = new ItemStack(safeMaterial("tagging.inventory_indicator.wool_primary", "RED_WOOL"));
        ItemStack secondaryWool = new ItemStack(safeMaterial("tagging.inventory_indicator.wool_secondary", "WHITE_WOOL"));

        boolean blink = runtime.isBlinkState();
        ItemStack woolStack = blink ? primaryWool : secondaryWool;

        PlayerInventory inventory = player.getInventory();
        int size = inventory.getSize();

        if (tntSlot >= 0 && tntSlot < size) {
            storeSlot(player, tntSlot, inventory.getItem(tntSlot));
            inventory.setItem(tntSlot, tntStack);
        }

        for (Integer slot : woolSlots) {
            if (slot == null || slot < 0 || slot >= size) continue;
            storeSlot(player, slot, inventory.getItem(slot));
            inventory.setItem(slot, woolStack);
        }

        boolean helmet = moduleConfig.getBoolean("tagging.inventory_indicator.apply_helmet", false);
        if (helmet) {
            storeSlot(player, 39, inventory.getHelmet());
            inventory.setHelmet(new ItemStack(Material.TNT));
        }
    }

    public void clearIndicator(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, ItemStack> saved = storedSlots.remove(uuid);
        if (saved == null || saved.isEmpty()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        for (Map.Entry<Integer, ItemStack> entry : saved.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();

            if (slot == 39) {
                inventory.setHelmet(item);
            } else if (slot == 40) {
                inventory.setItemInOffHand(item);
            } else if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    public void clearAllIndicators() {
        storedSlots.clear();
    }

    private void storeSlot(Player player, int slot, ItemStack original) {
        UUID uuid = player.getUniqueId();
        Map<Integer, ItemStack> saved = storedSlots.get(uuid);
        if (saved != null && saved.containsKey(slot)) {
            return;
        }

        ItemStack storedItem = original != null ? original.clone() : new ItemStack(Material.AIR);
        storedSlots.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>()).put(slot, storedItem);
    }

    private List<Integer> getWoolSlots() {
        List<String> rawSlots = moduleConfig.getStringList("tagging.inventory_indicator.wool_slots");
        List<Integer> slots = new ArrayList<>();

        if (rawSlots != null) {
            for (String entry : rawSlots) {
                try {
                    slots.add(Integer.parseInt(entry));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed entries
                }
            }
        }

        if (slots.isEmpty()) {
            slots.add(3);
            slots.add(5);
        }

        return slots;
    }

    private Material safeMaterial(String path, String def) {
        String value = moduleConfig.getString(path, def);
        if (value == null || value.isEmpty()) {
            value = def;
        }

        try {
            return Material.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Material.valueOf(def);
        }
    }
}
