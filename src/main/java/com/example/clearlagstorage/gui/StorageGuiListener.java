package com.example.clearlagstorage.gui;

import com.example.clearlagstorage.ClearLagStorage;
import com.example.clearlagstorage.storage.ItemStorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StorageGuiListener implements Listener {

    private final Map<UUID, Integer> openPage = new HashMap<>();
    private final Map<UUID, UUID> openTarget = new HashMap<>();
    private final Map<UUID, Boolean> openUnclaimed = new HashMap<>();
    private final java.util.Set<UUID> navigating = new java.util.HashSet<>();

    private final ClearLagStorage plugin;
    private final StorageGuiFactory factory;

    public StorageGuiListener(ClearLagStorage plugin) {
        this.plugin = plugin;
        this.factory = new StorageGuiFactory(plugin);
    }

    public StorageGuiFactory getFactory() {
        return factory;
    }

    public void registerOpenContext(Player viewer, UUID targetOwner, boolean unclaimed, int page) {
        openPage.put(viewer.getUniqueId(), page);
        openTarget.put(viewer.getUniqueId(), targetOwner);
        openUnclaimed.put(viewer.getUniqueId(), unclaimed);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageGuiHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();

        if (slot >= topInventory.getSize()) {
            return;
        }

        if (slot == StorageGuiFactory.PREV_PAGE_SLOT || slot == StorageGuiFactory.NEXT_PAGE_SLOT) {
            event.setCancelled(true);
            handlePageNav(player, slot);
            return;
        }

        if (slot == StorageGuiFactory.INFO_SLOT) {
            event.setCancelled(true);
            return;
        }

        ClickType clickType = event.getClick();
        ItemStack cursor = event.getCursor();
        boolean isPlacingAction = (cursor != null && !cursor.getType().isAir())
                && (clickType == ClickType.LEFT || clickType == ClickType.RIGHT);

        if (isPlacingAction) {
            event.setCancelled(true);
            return;
        }
    }

    private void handlePageNav(Player player, int slot) {
        UUID viewerId = player.getUniqueId();
        int currentPage = openPage.getOrDefault(viewerId, 0);
        UUID target = openTarget.get(viewerId);
        boolean unclaimed = openUnclaimed.getOrDefault(viewerId, false);

        int newPage = (slot == StorageGuiFactory.PREV_PAGE_SLOT) ? currentPage - 1 : currentPage + 1;

        navigating.add(viewerId);

        applySync(target, unclaimed, currentPage, player.getOpenInventory().getTopInventory());

        Inventory newInv = factory.build(target, newPage, unclaimed, null);
        player.openInventory(newInv);

        openPage.put(viewerId, newPage);
        openTarget.put(viewerId, target);
        openUnclaimed.put(viewerId, unclaimed);
        navigating.remove(viewerId);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageGuiHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (navigating.contains(player.getUniqueId())) {
            return;
        }
        syncCloseForViewer(player, event.getInventory());
    }

    private void syncCloseForViewer(Player player, Inventory closingInventory) {
        UUID viewerId = player.getUniqueId();
        UUID target = openTarget.remove(viewerId);
        Boolean unclaimedBox = openUnclaimed.remove(viewerId);
        Integer page = openPage.remove(viewerId);

        if (page == null || unclaimedBox == null) {
            return;
        }

        applySync(target, unclaimedBox, page, closingInventory);
    }

    private void applySync(UUID target, boolean unclaimed, int page, Inventory inv) {
        ItemStorageManager storage = plugin.getStorageManager();
        UUID storageKey = unclaimed ? null : target;
        List<ItemStack> items = storage.getItems(storageKey);

        int start = page * StorageGuiFactory.PAGE_SIZE;

        for (int slot = StorageGuiFactory.PAGE_SIZE - 1; slot >= 0; slot--) {
            int realIndex = start + slot;
            if (realIndex >= items.size()) {
                continue;
            }
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) {
                storage.removeItemAt(storageKey, realIndex);
            } else {
                items.set(realIndex, current);
            }
        }

        storage.saveOne(unclaimed ? ItemStorageManager.UNCLAIMED_ID : target);
    }
    }
