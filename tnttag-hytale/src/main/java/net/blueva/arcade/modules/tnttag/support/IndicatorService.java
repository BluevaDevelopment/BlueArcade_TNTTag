package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.modules.tnttag.state.ArenaRuntime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IndicatorService {

    private static final String DEFAULT_INDICATOR_ITEM = "Weapon_Bomb_Fire";

    private final ModuleConfigAPI moduleConfig;
    private final Map<UUID, Map<SlotRef, StoredItem>> storedSlots = new ConcurrentHashMap<>();

    public IndicatorService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void applyIndicator(Player player, ArenaRuntime runtime) {
        boolean enabled = moduleConfig.getBoolean("tagging.inventory_indicator.enabled", true);
        if (!enabled || player == null || player.getInventory() == null || player.getWorld() == null) {
            return;
        }

        String primaryItemId = resolveItemId("tagging.inventory_indicator.wool_primary", DEFAULT_INDICATOR_ITEM);
        String secondaryItemId = resolveItemId("tagging.inventory_indicator.wool_secondary", DEFAULT_INDICATOR_ITEM);
        boolean blink = runtime != null && runtime.isBlinkState();
        String indicatorItemId = blink ? primaryItemId : secondaryItemId;
        ItemStack indicatorStack = new ItemStack(indicatorItemId, 1);

        Inventory inventory = player.getInventory();
        applyToContainer(player, inventory.getArmor(), ContainerType.ARMOR, indicatorStack);
        applyToContainer(player, inventory.getHotbar(), ContainerType.HOTBAR, indicatorStack);
        applyToContainer(player, inventory.getStorage(), ContainerType.STORAGE, indicatorStack);
        applyToContainer(player, inventory.getUtility(), ContainerType.UTILITY, indicatorStack);
        applyToContainer(player, inventory.getTools(), ContainerType.TOOLS, indicatorStack);
        applyToContainer(player, inventory.getBackpack(), ContainerType.BACKPACK, indicatorStack);
    }

    public void clearIndicator(Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        UUID uuid = player.getUuid();
        Map<SlotRef, StoredItem> saved = storedSlots.remove(uuid);
        if (saved == null || saved.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (Map.Entry<SlotRef, StoredItem> entry : saved.entrySet()) {
            SlotRef ref = entry.getKey();
            StoredItem item = entry.getValue();
            ItemContainer container = resolveContainer(inventory, ref.containerType);
            if (container == null) {
                continue;
            }
            restoreSlot(container, ref.slot, item);
        }
    }

    public void clearAllIndicators() {
        storedSlots.clear();
    }

    private void applyToContainer(Player player, ItemContainer container, ContainerType type, ItemStack indicatorStack) {
        if (container == null) {
            return;
        }
        for (short slot = 0; slot < container.getCapacity(); slot++) {
            applyToSlot(player, container, type, slot, indicatorStack);
        }
    }


    private void applyToSlot(Player player, ItemContainer container, ContainerType type, short slot, ItemStack indicatorStack) {
        storeSlot(player, type, slot, container.getItemStack(slot));
        container.removeItemStackFromSlot(slot);
        container.addItemStackToSlot(slot, indicatorStack);
    }

    private void restoreSlot(ItemContainer container, short slot, StoredItem item) {
        container.removeItemStackFromSlot(slot);
        if (item == null || item.empty) {
            return;
        }
        if (item.stack != null) {
            container.addItemStackToSlot(slot, item.stack);
        }
    }

    private void storeSlot(Player player, ContainerType type, short slot, ItemStack original) {
        UUID uuid = player.getUuid();
        SlotRef ref = new SlotRef(type, slot);
        Map<SlotRef, StoredItem> saved = storedSlots.get(uuid);
        if (saved != null && saved.containsKey(ref)) {
            return;
        }
        storedSlots.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                .put(ref, new StoredItem(original, original == null || original.isEmpty()));
    }

    private ItemContainer resolveContainer(Inventory inventory, ContainerType type) {
        return switch (type) {
            case ARMOR -> inventory.getArmor();
            case HOTBAR -> inventory.getHotbar();
            case STORAGE -> inventory.getStorage();
            case UTILITY -> inventory.getUtility();
            case TOOLS -> inventory.getTools();
            case BACKPACK -> inventory.getBackpack();
        };
    }


    private String resolveItemId(String path, String def) {
        String value = moduleConfig.getString(path, def);
        if (value == null || value.isBlank()) {
            return def;
        }
        return value;
    }

    private enum ContainerType {
        ARMOR,
        HOTBAR,
        STORAGE,
        UTILITY,
        TOOLS,
        BACKPACK
    }

    private static final class SlotRef {
        private final ContainerType containerType;
        private final short slot;

        private SlotRef(ContainerType containerType, short slot) {
            this.containerType = containerType;
            this.slot = slot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SlotRef slotRef = (SlotRef) o;
            return slot == slotRef.slot && containerType == slotRef.containerType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerType, slot);
        }
    }

    private static final class StoredItem {
        private final ItemStack stack;
        private final boolean empty;

        private StoredItem(ItemStack stack, boolean empty) {
            this.stack = stack;
            this.empty = empty;
        }
    }
}
