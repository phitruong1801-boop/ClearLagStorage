package com.example.clearlagstorage;

import com.example.clearlagstorage.command.ClearLagCommand;
import com.example.clearlagstorage.config.ConfigManager;
import com.example.clearlagstorage.gui.StorageGuiListener;
import com.example.clearlagstorage.storage.ItemStorageManager;
import com.example.clearlagstorage.task.ClearTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClearLagStorage extends JavaPlugin {

    private ConfigManager configManager;
    private ItemStorageManager storageManager;
    private ClearTask clearTask;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.storageManager = new ItemStorageManager(this);
        this.storageManager.loadAll();

        StorageGuiListener guiListener = new StorageGuiListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        ClearLagCommand commandExecutor = new ClearLagCommand(this, guiListener);
        getCommand("clearlag").setExecutor(commandExecutor);
        getCommand("clearlag").setTabCompleter(commandExecutor);

        this.clearTask = new ClearTask(this);
        this.clearTask.start();

        getLogger().info("ClearLagStorage đã bật. Item bị dọn sẽ được lưu lại, không mất hẳn.");
    }

    @Override
    public void onDisable() {
        if (clearTask != null) {
            clearTask.stop();
        }
        if (storageManager != null) {
            storageManager.saveAll();
        }
        getLogger().info("ClearLagStorage đã tắt, đã lưu toàn bộ kho item.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemStorageManager getStorageManager() {
        return storageManager;
    }

    public ClearTask getClearTask() {
        return clearTask;
    }
              }
