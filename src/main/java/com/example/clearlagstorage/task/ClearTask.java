package com.example.clearlagstorage.task;

import com.example.clearlagstorage.ClearLagStorage;
import com.example.clearlagstorage.storage.ItemStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class ClearTask {

    private final ClearLagStorage plugin;
    private BukkitTask scheduledTask;
    private int secondsRemaining;

    public ClearTask(ClearLagStorage plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfigManager().isClearTaskEnabled()) {
            return;
        }
        this.secondsRemaining = plugin.getConfigManager().getIntervalSeconds();

        this.scheduledTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    public void restart() {
        start();
    }

    private void tick() {
        List<Integer> warnings = plugin.getConfigManager().getWarningSeconds();

        if (warnings.contains(secondsRemaining) && secondsRemaining > 0) {
            String msg = plugin.getConfigManager().getMessage("warning")
                    .replace("{seconds}", String.valueOf(secondsRemaining));
            Bukkit.broadcastMessage(msg);
            playSoundToAll(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        if (secondsRemaining <= 0) {
            int cleared = runClear();
            String msg = plugin.getConfigManager().getMessage("cleared-broadcast")
                    .replace("{count}", String.valueOf(cleared));
            if (cleared > 0) {
                Bukkit.broadcastMessage(msg);
                playSoundToAll(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            secondsRemaining = plugin.getConfigManager().getIntervalSeconds();
            return;
        }

        secondsRemaining--;
    }

    public int runClear() {
        List<String> worldFilter = plugin.getConfigManager().getWorldsFilter();
        boolean ignoreNamed = plugin.getConfigManager().isIgnoreCustomNamedItems();
        ItemStorageManager storage = plugin.getStorageManager();

        int count = 0;

        for (World world : Bukkit.getWorlds()) {
            if (!worldFilter.isEmpty() && !worldFilter.contains(world.getName())) {
                continue;
            }

            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                Item itemEntity = (Item) entity;
                org.bukkit.inventory.ItemStack stack = itemEntity.getItemStack();

                if (ignoreNamed && stack.getItemMeta() != null && stack.getItemMeta().hasDisplayName()) {
                    continue;
                }

                UUID owner = resolveOwner(itemEntity);
                storage.addItem(owner, stack);
                itemEntity.remove();
                count++;
            }
        }

        storage.saveAll();
        return count;
    }

    private void playSoundToAll(org.bukkit.Sound sound, float volume, float pitch) {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private UUID resolveOwner(Item itemEntity) {
        if (itemEntity.getOwner() != null) {
            return itemEntity.getOwner();
        }
        if (itemEntity.getThrower() != null) {
            return itemEntity.getThrower();
        }
        return null;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }
    }
