package com.example.clearlagstorage.config;

import com.example.clearlagstorage.ClearLagStorage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    private final ClearLagStorage plugin;
    private FileConfiguration config;

    public ConfigManager(ClearLagStorage plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Clear Task
    public boolean isClearTaskEnabled() {
        return config.getBoolean("clear-task.enabled", true);
    }
    public int getIntervalSeconds() {
        return config.getInt("clear-task.interval-seconds", 300);
    }
    public List<Integer> getWarningSeconds() {
        return config.getIntegerList("clear-task.warning-seconds");
    }
    public List<String> getWorldsFilter() {
        return config.getStringList("clear-task.worlds");
    }
    public boolean isIgnoreCustomNamedItems() {
        return config.getBoolean("clear-task.ignore-custom-named-items", false);
    }

    // Merge Task
    public boolean isMergeEnabled() {
        return config.getBoolean("merge-task.enabled", true);
    }
    public int getMergeInterval() {
        return config.getInt("merge-task.interval-seconds", 5);
    }
    public double getMergeRadius() {
        return config.getDouble("merge-task.radius", 2.5);
    }

    // Storage
    public int getMaxStacksPerPlayer() {
        return config.getInt("storage.max-stacks-per-player", 200);
    }
    public int getMaxStacksUnclaimed() {
        return config.getInt("storage.max-stacks-unclaimed", 500);
    }
    public int getAutoPurgeDays() {
        return config.getInt("storage.auto-purge-days", 7);
    }

    // Messages
    public String getMessage(String path) {
        String prefix = config.getString("messages.prefix", "");
        String raw = config.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + raw);
    }
    public String getRawMessage(String path) {
        String raw = config.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
