package com.example.clearlagstorage.gui;

import com.example.clearlagstorage.ClearLagStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StorageGuiFactory {

    public static final int PAGE_SIZE = 45;
    public static final int PREV_PAGE_SLOT = 45;
    public static final int NEXT_PAGE_SLOT = 53;
    public static final int INFO_SLOT = 49;

    private final ClearLagStorage plugin;

    public StorageGuiFactory(ClearLagStorage plugin) {
        this.plugin = plugin;
    }

    public Inventory build(UUID targetOwner, int page, boolean unclaimed, String titleOverride) {
        List<ItemStack> items = plugin.getStorageManager().getItems(unclaimed ? null : targetOwner);
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String baseTitle = titleOverride != null ? titleOverride
                : (unclaimed
                    ? plugin.getConfigManager().getRawMessage("unclaimed-title")
                    : plugin.getConfigManager().getRawMessage("storage-title"));
        String title = baseTitle + " (" + (page + 1) + "/" + totalPages + ")";

        StorageGuiHolder holder = new StorageGuiHolder(unclaimed ? null : targetOwner);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, items.get(i));
        }

        if (page > 0) {
            inv.setItem(PREV_PAGE_SLOT, navButton(Material.ARROW, "&eTrang trước"));
        }
        if (page < totalPages - 1) {
            inv.setItem(NEXT_PAGE_SLOT, navButton(Material.ARROW, "&eTrang sau"));
        }
        inv.setItem(INFO_SLOT, infoItem(items.size()));

        return inv;
    }

    private ItemStack navButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(int totalCount) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Tổng số vật phẩm: &f" + totalCount));
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Nhấn vào item để lấy lại."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
          }
