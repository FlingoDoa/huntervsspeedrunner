package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.NamespacedKey;
import java.util.Iterator;
import java.util.List;
import org.bukkit.World;
import org.bukkit.Location;



public class PlayerDataManager {

    private final File dataFolder;
    private final LifeManager lifeManager;

    public PlayerDataManager(File dataFolder, LifeManager lifeManager) {
        this.dataFolder = new File(dataFolder, "playerdata");
        this.lifeManager = lifeManager;
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }
    public void clearPlayerData(Player player) {
        File file = new File(dataFolder, player.getUniqueId() + ".yml");
        if (file.exists()) {
            file.delete();
        }

        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType())
        );

        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criteria : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criteria);
            }
        }

        World world = Bukkit.getWorld("world");
        if (world != null) {
            Location spawnLocation = world.getSpawnLocation();
            player.teleport(spawnLocation);
            player.setBedSpawnLocation(spawnLocation, true);
        } else {
            Bukkit.getLogger().warning("Мир 'world' не найден! Убедитесь, что он загружен.");
        }
    }

    public void savePlayerData(Player player) {
        if (!isPlayerInGame(player)) {
            return;
        }

        File file = new File(dataFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("inventory", player.getInventory().getContents());

        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            boolean achieved = player.getAdvancementProgress(advancement).isDone();
            config.set("achievements." + advancement.getKey().getKey(), achieved);
        }

        Location loc = player.getLocation();
        config.set("location.world", loc.getWorld().getName());
        config.set("location.x", loc.getX());
        config.set("location.y", loc.getY());
        config.set("location.z", loc.getZ());
        config.set("location.yaw", loc.getYaw());
        config.set("location.pitch", loc.getPitch());

        config.set("exp", player.getTotalExperience());

        if (player.getBedSpawnLocation() != null) {
            config.set("old_spawn", player.getBedSpawnLocation());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPlayerInGame(Player player) {
        return lifeManager.isHunter(player) || lifeManager.isSpeedrunner(player);
    }


    public void loadPlayerData(Player player) {
        File file = new File(dataFolder, player.getUniqueId() + ".yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("location.world")) {
            String worldName = config.getString("location.world");
            if (worldName != null && !worldName.isEmpty()) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("location.x");
                    double y = config.getDouble("location.y");
                    double z = config.getDouble("location.z");
                    float yaw = (float) config.getDouble("location.yaw");
                    float pitch = (float) config.getDouble("location.pitch");

                    Location returnLocation = new Location(world, x, y, z, yaw, pitch);
                    if (player.isOnline() && !player.isDead()) {
                        player.teleport(returnLocation);
                    } else {
                        Bukkit.getLogger().info("Игрок " + player.getName() + " еще не возродился, ждем...");
                    }
                } else {
                    Bukkit.getLogger().warning("Мир '" + worldName + "' не найден. Игрок не был телепортирован.");
                }
            }
        }
        if (config.contains("inventory")) {
            Object inventoryObject = config.get("inventory");
            if (inventoryObject instanceof List<?>) {
                List<?> itemList = (List<?>) inventoryObject;
                ItemStack[] inventoryArray = itemList.toArray(new ItemStack[0]);
                player.getInventory().setContents(inventoryArray);
            }
        }
        if (config.contains("achievements") && config.getConfigurationSection("achievements") != null) {
            for (String key : config.getConfigurationSection("achievements").getKeys(false)) {
                boolean achieved = config.getBoolean("achievements." + key);
                if (achieved) {
                    Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(key));
                    if (advancement != null) {
                        AdvancementProgress progress = player.getAdvancementProgress(advancement);
                        for (String criteria : progress.getRemainingCriteria()) {
                            progress.awardCriteria(criteria);
                        }
                    }
                }
            }
        }
        if (config.contains("old_spawn")) {
            Location oldSpawn = config.getLocation("old_spawn");
            player.setBedSpawnLocation(oldSpawn, true);
        }

        player.setTotalExperience(config.getInt("exp"));
    }
}
