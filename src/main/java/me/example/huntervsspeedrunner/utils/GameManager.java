package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.World;
import org.bukkit.Location;

public class GameManager {

    private static boolean gameStarted = false;

    public static void startGame(HunterVSSpeedrunnerPlugin plugin) {
        if (gameStarted) {
            return;
        }

        gameStarted = true;
        plugin.getLifeManager().initializeScoreboard();
        Bukkit.getLogger().info("Game is starting...");

        startCompassCountdown(plugin);
        Bukkit.getLogger().info("Compass countdown started.");

        Bukkit.broadcastMessage("§aThe game has started!");

        clearAchievements();

        teleportSpeedrunners(plugin);
        teleportHuntersDelayed(plugin);
    }

    public static boolean canStartGame(HunterVSSpeedrunnerPlugin plugin) {
        LifeManager lifeManager = plugin.getLifeManager();
        boolean hasHunter = !lifeManager.getHunters().isEmpty();
        boolean hasSpeedrunner = !lifeManager.getSpeedrunners().isEmpty();
        return hasHunter && hasSpeedrunner;
    }

    public static void openTeamSelectionMenu(Player player, HunterVSSpeedrunnerPlugin plugin) {
        if (gameStarted) {
            player.sendMessage("§cThe game has already started! Unable to change team.");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        Inventory inventory = Bukkit.createInventory(null, 9, "Select a Team");

        ItemStack hunterItem = createMenuItem(config, "menu.hunter");
        ItemStack speedrunnerItem = createMenuItem(config, "menu.speedrunner");

        inventory.setItem(config.getInt("menu.hunter.slot"), hunterItem);
        inventory.setItem(config.getInt("menu.speedrunner.slot"), speedrunnerItem);

        ItemStack addLifeItem = createMenuItem(config, "menu.add_life");
        ItemStack removeLifeItem = createMenuItem(config, "menu.remove_life");
        inventory.setItem(config.getInt("menu.add_life.slot"), addLifeItem);
        inventory.setItem(config.getInt("menu.remove_life.slot"), removeLifeItem);

        ItemStack startItem = createMenuItem(config, "menu.start");
        inventory.setItem(config.getInt("menu.start.slot"), startItem);

        player.openInventory(inventory);
    }

    private static ItemStack createMenuItem(FileConfiguration config, String path) {
        String materialName = config.getString(path + ".item");
        String displayName = config.getString(path + ".name");
        Material material = Material.getMaterial(materialName);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static void startCompassCountdown(HunterVSSpeedrunnerPlugin plugin) {
        int compassDelaySeconds = plugin.getConfig().getInt("hunter.compassgive");
        Bukkit.getLogger().info("Time until compass is given: " + compassDelaySeconds + " seconds.");

        new BukkitRunnable() {
            int countdown = compassDelaySeconds;

            @Override
            public void run() {
                if (countdown <= 0) {
                    giveCompassToHunters(plugin);
                    cancel();
                    return;
                }

                sendTimeMessage(countdown);
                countdown -= 30;
            }
        }.runTaskTimer(plugin, 0, 20L * 30);
    }

    private static void sendTimeMessage(int countdown) {
        int minutes = countdown / 60;
        int seconds = countdown % 60;

        String timeMessage = (minutes > 0) ?
                "Time until compass is given to hunters: " + minutes + " minute" + (minutes >= 2 ? "s" : "") + " and " + seconds + " seconds" :
                "Time until compass is given to hunters: " + seconds + " seconds.";

        Bukkit.broadcastMessage("§a" + timeMessage);
    }

    private static void giveCompassToHunters(HunterVSSpeedrunnerPlugin plugin) {
        Bukkit.getLogger().info("Giving compasses to hunters.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(player)) {
                // Check if player already has a compass
                boolean hasCompass = player.getInventory().contains(Material.COMPASS);
                if (!hasCompass) {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    ItemMeta meta = compass.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§cHunter's Compass");
                        compass.setItemMeta(meta);
                    }
                    player.getInventory().addItem(compass);
                }
            }
        }
    }

    private static void teleportSpeedrunners(HunterVSSpeedrunnerPlugin plugin) {
        Bukkit.getLogger().info("Teleporting speedrunners.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clear @a");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "time set day");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give @a minecraft:saturation 10 255");
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getLifeManager().isSpeedrunner(player)) {
                    teleportToEventWorld(player, plugin);
                    new SpawnTask(plugin, player).runTaskTimer(plugin, 0L, 20L * 5);
                }
            }
        });
    }

    private static void teleportHuntersDelayed(HunterVSSpeedrunnerPlugin plugin) {
        int teleportDelay = plugin.getConfig().getInt("hunter.teleportDelay");
        Bukkit.getLogger().info("Hunter teleport delay: " + teleportDelay + " seconds.");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getLifeManager().isHunter(player)) {
                        teleportToEventWorld(player, plugin);
                    }
                }
            }
        }.runTaskLater(plugin, 20L * teleportDelay);
    }

    private static void teleportToEventWorld(Player player, HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String eventWorldName = config.getString("event.worldName");
        if (Bukkit.getWorld(eventWorldName) != null) {
            World eventWorld = Bukkit.getWorld(eventWorldName);

            // Find the highest ground point at (0, 0) and add two blocks to the height
            int groundY = eventWorld.getHighestBlockYAt(0, 0);
            int teleportY = groundY + 2;

            Location teleportLocation = new Location(eventWorld, 0, teleportY, 0);
            player.teleport(teleportLocation);
            player.sendMessage("§aYou have been teleported to the event world at coordinates (0, " + teleportY + ", 0)!");
        } else {
            player.sendMessage("§cEvent world not found! use /hunterworld to create  ");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload confirm");
            player.sendMessage("§cPlugins reload all!");
        }
    }

    private static void clearAchievements() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke @a everything");
        Bukkit.broadcastMessage("§cAll achievements have been reset for all players.");
    }

    public static void endGame() {
        gameStarted = false;
        Bukkit.getLogger().info("Game has ended.");

        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");

        if (plugin == null) {
            Bukkit.getLogger().warning("HunterVSSpeedrunner plugin not found!");
            return;
        }

        LifeManager lifeManager = plugin.getLifeManager();

        if (lifeManager.getSpeedrunners().isEmpty()) {
            Bukkit.broadcastMessage("§cHunters win!");
        } else {
            Bukkit.broadcastMessage("§aSpeedrunners win!");
        }

        Bukkit.broadcastMessage("§cThe game has ended.");
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }
}
