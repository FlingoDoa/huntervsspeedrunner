package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {

    private static boolean gameStarted = false;
    private static BukkitTask compassCountdownTask;  // Храним задачу таймера компаса

    public static void startGame(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (gameStarted) {
            return;
        }

        gameStarted = true;
        plugin.getLifeManager().initializeScoreboard();
        Bukkit.getLogger().info("Game is starting...");

        startCompassCountdown(plugin);  // Запуск отсчета компаса
        Bukkit.getLogger().info("Compass countdown started.");

        Bukkit.broadcastMessage(config.getString(language + ".messages.game_start_success"));

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

        ItemStack hunterItem = createMenuItem(config, config.getString("language") + ".menu.hunter");
        ItemStack speedrunnerItem = createMenuItem(config, config.getString("language") + ".menu.speedrunner");

        inventory.setItem(config.getInt(config.getString("language") + ".menu.hunter.slot"), hunterItem);
        inventory.setItem(config.getInt(config.getString("language") + ".menu.speedrunner.slot"), speedrunnerItem);

        ItemStack addLifeItem = createMenuItem(config, config.getString("language") + ".menu.add_life");
        ItemStack removeLifeItem = createMenuItem(config, config.getString("language") + ".menu.remove_life");
        inventory.setItem(config.getInt(config.getString("language") + ".menu.add_life.slot"), addLifeItem);
        inventory.setItem(config.getInt(config.getString("language") + ".menu.remove_life.slot"), removeLifeItem);

        ItemStack startItem = createMenuItem(config, config.getString("language") + ".menu.start");
        inventory.setItem(config.getInt(config.getString("language") + ".menu.start.slot"), startItem);

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
        // Если уже есть активная задача, отменяем её
        if (compassCountdownTask != null && !compassCountdownTask.isCancelled()) {
            compassCountdownTask.cancel();
            Bukkit.getLogger().info("Old compass countdown task cancelled.");
        }

        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        int compassDelaySeconds = config.getInt("hunter.compassgive");

        Bukkit.getLogger().info(language.equals("ru") ?
                "Время до выдачи компаса хантерам: " + compassDelaySeconds + " секунд." :
                "Time until compass is given: " + compassDelaySeconds + " seconds.");

        compassCountdownTask = new BukkitRunnable() {
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
        }.runTaskTimer(plugin, 0, 20L * 30);  // Запускаем новый таймер и сохраняем задачу
    }

    private static void sendTimeMessage(int countdown) {
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        int minutes = countdown / 60;
        int seconds = countdown % 60;

        String timeMessage = (minutes > 0) ?
                (language.equals("ru") ?
                        "Время до выдачи компаса хантерам: " + minutes + " минут" + (minutes >= 2 ? "с" : "") + " и " + seconds + " секунд" :
                        "Time until compass is given to hunters: " + minutes + " minute" + (minutes >= 2 ? "s" : "") + " and " + seconds + " seconds") :
                (language.equals("ru") ?
                        "Время до выдачи компаса хантерам: " + seconds + " секунд." :
                        "Time until compass is given to hunters: " + seconds + " seconds.");

        Bukkit.broadcastMessage("§a" + timeMessage);
    }

    private static void giveCompassToHunters(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        Bukkit.getLogger().info(config.getString(language + ".messages.compass_give"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(player) && !player.getInventory().contains(Material.COMPASS)) {
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

    private static void teleportSpeedrunners(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        Bukkit.getLogger().info(config.getString(language + ".messages.teleport_speed"));

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
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        int teleportDelay = config.getInt("hunter.teleportDelay");

        Bukkit.getLogger().info(language.equals("ru") ?
                "Задержка телепорта хантеров: " + teleportDelay + " секунд." :
                "Hunter teleport delay: " + teleportDelay + " seconds.");

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
        String language = config.getString("language");

        World eventWorld = Bukkit.getWorld(eventWorldName);
        if (eventWorld != null) {
            Location teleportLocation = new Location(eventWorld, 0, eventWorld.getHighestBlockYAt(0, 0) + 2, 0);
            player.teleport(teleportLocation);
        } else {
            player.sendMessage(config.getString(language + ".messages.not_found"));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload confirm");
            player.sendMessage(config.getString(language + ".messages.plugins"));
        }
    }

    private static void clearAchievements() {
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        Bukkit.broadcastMessage(config.getString(language + ".messages.clear_ach"));

        // Пройтись по каждому игроку и отозвать достижения
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.advancementIterator().forEachRemaining(advancement -> {
                player.getAdvancementProgress(advancement).getAwardedCriteria()
                        .forEach(criteria -> player.getAdvancementProgress(advancement).revokeCriteria(criteria));
            });
        }
    }

    public static void endGame() {
        gameStarted = false;
        Bukkit.getLogger().info("Game has ended.");

        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");

        LifeManager lifeManager = plugin.getLifeManager();
        if (lifeManager.getSpeedrunners().isEmpty()) {
            Bukkit.broadcastMessage("§a" + config.getString(language + ".messages.hunter_win"));
        } else {
            Bukkit.broadcastMessage("§a" + config.getString(language + ".messages.speedrunners"));
        }
        Bukkit.broadcastMessage("§c" + config.getString(language + ".messages.game_end"));
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }
}
