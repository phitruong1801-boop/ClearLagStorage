package com.example.clearlagstorage;

import com.example.clearlagstorage.command.ClearLagCommand;
import com.example.clearlagstorage.config.ConfigManager;
import com.example.clearlagstorage.gui.StorageGuiListener;
import com.example.clearlagstorage.storage.ItemStorageManager;
import com.example.clearlagstorage.task.ClearTask;
import com.example.clearlagstorage.task.MergeTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClearLagStorage extends JavaPlugin {
    private ConfigManager configManager;
    private ItemStorageManager storageManager;
    private ClearTask clearTask;
    private MergeTask mergeTask;

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

        this.mergeTask = new MergeTask(this);
        this.mergeTask.start();

        getLogger().info("✅ ClearLagStorage: Đã bật Gộp Item + Lưu Trữ!");
    }

    @Override
    public void onDisable() {
        if (clearTask != null) clearTask.stop();
        if (mergeTask != null) mergeTask.stop();
        if (storageManager != null) storageManager.saveAll();
        getLogger().info("🛑 ClearLagStorage: Đã tắt & lưu dữ liệu.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public ItemStorageManager getStorageManager() { return storageManager; }
    public ClearTask getClearTask() { return clearTask; }
    public MergeTask getMergeTask() { return mergeTask; }
}
