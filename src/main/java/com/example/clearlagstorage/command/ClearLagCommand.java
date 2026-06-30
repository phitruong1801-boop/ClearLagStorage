package com.example.clearlagstorage.command;

import com.example.clearlagstorage.ClearLagStorage;
import com.example.clearlagstorage.gui.StorageGuiListener;
import com.example.clearlagstorage.storage.ItemStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClearLagCommand implements CommandExecutor, TabCompleter {

    private final ClearLagStorage plugin;
    private final StorageGuiListener guiListener;

    public ClearLagCommand(ClearLagStorage plugin, StorageGuiListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "now" -> handleNow(sender);
            case "storage" -> handleStorage(sender, args);
            case "unclaimed" -> handleUnclaimed(sender);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleNow(CommandSender sender) {
        if (!sender.hasPermission("clearlagstorage.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        int cleared = plugin.getClearTask().runClear();
        sender.sendMessage(plugin.getConfigManager().getMessage("cleared-now")
                .replace("{count}", String.valueOf(cleared)));
    }

    private void handleStorage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Lệnh này chỉ dùng được trong game.");
            return;
        }
        if (!sender.hasPermission("clearlagstorage.use")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        UUID targetId;
        String titleOverride = null;

        if (args.length >= 2) {
            if (!sender.hasPermission("clearlagstorage.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                viewer.sendMessage("§cKhông tìm thấy người chơi: " + args[1]);
                return;
            }
            targetId = target.getUniqueId();
            titleOverride = "&8Kho của " + (target.getName() != null ? target.getName() : args[1]);
        } else {
            targetId = viewer.getUniqueId();
        }

        openStorageGui(viewer, targetId, false, titleOverride);
    }

    private void handleUnclaimed(CommandSender sender) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Lệnh này chỉ dùng được trong game.");
            return;
        }
        if (!sender.hasPermission("clearlagstorage.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        openStorageGui(viewer, ItemStorageManager.UNCLAIMED_ID, true, null);
    }

    private void openStorageGui(Player viewer, UUID targetId, boolean unclaimed, String titleOverride) {
        Inventory inv = guiListener.getFactory().build(targetId, 0, unclaimed, titleOverride);
        guiListener.registerOpenContext(viewer, targetId, unclaimed, 0);
        viewer.openInventory(inv);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("clearlagstorage.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        plugin.getConfigManager().reload();
        plugin.getClearTask().restart();
        sender.sendMessage(plugin.getConfigManager().getMessage("reloaded"));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§7/clearlag now §f- Dọn item rơi ngay (admin)");
        sender.sendMessage("§7/clearlag storage [player] §f- Mở kho item đã bị dọn");
        sender.sendMessage("§7/clearlag unclaimed §f- Mở kho item không rõ chủ (admin)");
        sender.sendMessage("§7/clearlag reload §f- Tải lại config.yml (admin)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("now");
            options.add("storage");
            options.add("unclaimed");
            options.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("storage")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
        }
        return options;
    }
              }
