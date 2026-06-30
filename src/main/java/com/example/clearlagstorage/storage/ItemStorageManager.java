package com.example.clearlagstorage.storage;

import com.example.clearlagstorage.ClearLagStorage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ItemStorageManager {
    public static final UUID UNCLAIMED_ID = new UUID(0L, 0L);
    private final ClearLagStorage plugin;
    private final File dataFolder;
    private final Map<UUID, List<ItemStack>> storages = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> timestamps = new ConcurrentHashMap<>();

    public ItemStorageManager(ClearLagStorage plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data" + File.separator + "players");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public void addItem(UUID owner, ItemStack item) {
        UUID key = (owner != null) ? owner : UNCLAIMED_ID;
        List<ItemStack> list = storages.computeIfAbsent(key, k -> new ArrayList<>());
        List<Long> times = timestamps.computeIfAbsent(key, k -> new ArrayList<>());
        ItemStack toAdd = item.clone();

        // GỘP ITEM GIỐNG NHAU: cùng loại + cùng thuộc tính, còn chỗ trống
        for (int i = 0; i < list.size() && toAdd.getAmount() > 0; i++) {
            ItemStack existing = list.get(i);
            if (existing.isSimilar(toAdd)) {
                int maxStack = existing.getMaxStackSize();
                int space = maxStack - existing.getAmount();
                if (space > 0) {
                    int add = Math.min(space, toAdd.getAmount());
                    existing.addAmount(add);
                    toAdd.subtractAmount(add);
                }
            }
        }

        // Nếu còn lại sau khi gộp → thêm stack mới
        if (toAdd.getAmount() > 0) {
            list.add(toAdd);
            times.add(System.currentTimeMillis());
        }

        // Giới hạn số stack tối đa
        int max = key.equals(UNCLAIMED_ID)
                ? plugin.getConfigManager().getMaxStacksUnclaimed()
                : plugin.getConfigManager().getMaxStacksPerPlayer();
        while (list.size() > max) {
            list.remove(0);
            if (!times.isEmpty()) times.remove(0);
        }
    }

    public List<ItemStack> getItems(UUID owner) {
        UUID key = (owner != null) ? owner : UNCLAIMED_ID;
        return storages.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public List<Long> getTimestamps(UUID owner) {
        UUID key = (owner != null) ? owner : UNCLAIMED_ID;
        return timestamps.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public void removeItemAt(UUID owner, int index) {
        UUID key = (owner != null) ? owner : UNCLAIMED_ID;
        List<ItemStack> list = storages.get(key);
        List<Long> times = timestamps.get(key);
        if (list != null && index >= 0 && index < list.size()) list.remove(index);
        if (times != null && index >= 0 && index < times.size()) times.remove(index);
    }

    public boolean hasAnyData(UUID owner) {
        List<ItemStack> list = storages.get(owner != null ? owner : UNCLAIMED_ID);
        return list != null && !list.isEmpty();
    }

    public void purgeExpired() {
        int days = plugin.getConfigManager().getAutoPurgeDays();
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        for (UUID key : storages.keySet()) {
            List<ItemStack> list = storages.get(key);
            List<Long> times = timestamps.get(key);
            if (list == null || times == null) continue;
            int i = 0;
            while (i < list.size() && i < times.size()) {
                if (times.get(i) < cutoff) {
                    list.remove(i);
                    times.remove(i);
                } else i++;
            }
        }
    }

    public void loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                loadOne(uuid, file);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadOne(UUID uuid, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> encoded = yaml.getStringList("items");
        List<Long> times = yaml.getLongList("timestamps");
        List<ItemStack> items = new ArrayList<>();
        for (String enc : encoded) {
            ItemStack stack = deserializeItem(enc);
            if (stack != null) items.add(stack);
        }
        while (times.size() < items.size()) times.add(System.currentTimeMillis());
        storages.put(uuid, items);
        timestamps.put(uuid, times);
    }

    public void saveAll() {
        for (UUID uuid : storages.keySet()) saveOne(uuid);
    }

    public void saveOne(UUID uuid) {
        List<ItemStack> items = storages.getOrDefault(uuid, new ArrayList<>());
        List<Long> times = timestamps.getOrDefault(uuid, new ArrayList<>());
        File file = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> encoded = new ArrayList<>();
        for (ItemStack item : items) {
            String s = serializeItem(item);
            if (s != null) encoded.add(s);
        }
        yaml.set("items", encoded);
        yaml.set("timestamps", times);
        try { yaml.save(file); }
        catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Không thể lưu kho cho " + uuid, e);
        }
    }

    private String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out)) {
            dataOut.writeObject(item);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi mã hóa item", e);
            return null;
        }
    }

    private ItemStack deserializeItem(String encoded) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in)) {
            return (ItemStack) dataIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Bỏ qua item lỗi", e);
            return null;
        }
    }
}
