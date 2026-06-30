package com.example.clearlagstorage.task;

import com.example.clearlagstorage.ClearLagStorage;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class MergeTask {
    private final ClearLagStorage plugin;
    private BukkitTask mergeTask;

    public MergeTask(ClearLagStorage plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfigManager().isMergeEnabled()) return;
        int interval = plugin.getConfigManager().getMergeInterval();
        this.mergeTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::mergeAllWorlds, 20L, interval * 20L
        );
    }

    public void stop() {
        if (mergeTask != null) {
            mergeTask.cancel();
            mergeTask = null;
        }
    }

    public void restart() {
        start();
    }

    private void mergeAllWorlds() {
        double radius = plugin.getConfigManager().getMergeRadius();
        double radiusSq = radius * radius;

        for (World world : plugin.getServer().getWorlds()) {
            List<Item> items = new ArrayList<>(world.getEntitiesByClass(Item.class));
            if (items.size() < 2) continue;

            for (int i = 0; i < items.size(); i++) {
                Item main = items.get(i);
                if (main.isDead()) continue;
                ItemStack mainStack = main.getItemStack();

                for (int j = i + 1; j < items.size(); j++) {
                    Item other = items.get(j);
                    if (other.isDead()) continue;

                    if (mainStack.isSimilar(other.getItemStack())
                            && main.getLocation().distanceSquared(other.getLocation()) <= radiusSq) {

                        int max = mainStack.getMaxStackSize();
                        int space = max - mainStack.getAmount();
                        if (space <= 0) break;

                        int add = Math.min(space, other.getItemStack().getAmount());
                        // Thay addAmount bằng setAmount
                        mainStack.setAmount(mainStack.getAmount() + add);
                        main.setItemStack(mainStack);
                        other.remove();
                    }
                }
            }
        }
    }
}
