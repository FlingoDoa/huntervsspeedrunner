package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.RandomTaskManager;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import me.example.huntervsspeedrunner.utils.RandomTaskManager.Task;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {

    private static boolean gameStarted = false;
    private static BukkitTask compassCountdownTask;
    private static BossBar compassBossBar;

    public static void startGame(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        RandomTaskManager randomTaskManager = plugin.getRandomTaskManager();
        LifeManager lifeManager = plugin.getLifeManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (randomTaskManager.isRandomModeEnabled() && lifeManager.isSpeedrunner(player)) {
                Task task = randomTaskManager.generateRandomTask(player);
                randomTaskManager.showTaskToPlayer(player, task);
            }
        }

        if (gameStarted) {
            return; // Если игра уже началась, ничего не делаем
        }

        gameStarted = true;
        lifeManager.initializeScoreboard();
        Bukkit.getLogger().info("Game is starting...");

        startCompassCountdown(plugin); // Запуск отсчета компаса
        Bukkit.broadcastMessage(config.getString("language") + ".messages.game_start_success");

        teleportSpeedrunners(plugin); // Телепортируем спидраннеров
        teleportHuntersDelayed(plugin); // Телепортируем охотников с задержкой
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
        Inventory inventory = Bukkit.createInventory(null, 18, "Select a Team");  // 18 ячеек для 2 строк

        // Создание предметов для меню
        ItemStack hunterItem = createMenuItem(config, config.getString("language") + ".menu.hunter");
        ItemStack speedrunnerItem = createMenuItem(config, config.getString("language") + ".menu.speedrunner");

        // Установка предметов на соответствующие слоты
        inventory.setItem(config.getInt(config.getString("language") + ".menu.hunter.slot"), hunterItem);
        inventory.setItem(config.getInt(config.getString("language") + ".menu.speedrunner.slot"), speedrunnerItem);

        ItemStack addLifeItem = createMenuItem(config, config.getString("language") + ".menu.add_life");
        ItemStack removeLifeItem = createMenuItem(config, config.getString("language") + ".menu.remove_life");
        inventory.setItem(config.getInt(config.getString("language") + ".menu.add_life.slot"), addLifeItem);
        inventory.setItem(config.getInt(config.getString("language") + ".menu.remove_life.slot"), removeLifeItem);

        ItemStack startItem = createMenuItem(config, config.getString("language") + ".menu.start");
        inventory.setItem(config.getInt(config.getString("language") + ".menu.start.slot"), startItem);

        // Новые кнопки
        ItemStack toggleRandomItem = createMenuItem(config, config.getString("language") + ".menu.toggle_random");
        inventory.setItem(10, toggleRandomItem);

        ItemStack restartWorldsItem = createMenuItem(config, config.getString("language") + ".menu.restart_world");
        inventory.setItem(12, restartWorldsItem);

        ItemStack toggleCompassItem = createMenuItem(config, config.getString("language") + ".menu.toggle_compass");
        inventory.setItem(14, toggleCompassItem);

        ItemStack reloadPluginItem = createMenuItem(config, config.getString("language") + ".menu.reload_plugin");
        inventory.setItem(16, reloadPluginItem);
        player.openInventory(inventory);
    }

    private static ItemStack createMenuItem(FileConfiguration config, String path) {
        String materialName = config.getString(path + ".item");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            Bukkit.getLogger().warning("Material not found for path: " + path);
            return null;
        }

        String displayName = config.getString(path + ".name");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_PLACED_ON);
            item.setItemMeta(meta);
        } else {
            Bukkit.getLogger().warning("ItemMeta is null for material: " + material);
        }

        return item;
    }


    private static void startCompassCountdown(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (compassCountdownTask != null && !compassCountdownTask.isCancelled()) {
            compassCountdownTask.cancel();
        }
        if (compassBossBar != null) {
            compassBossBar.removeAll();
            compassBossBar = null;
        }

        int compassDelaySeconds = config.getInt("hunter.compassgive");

        compassBossBar = Bukkit.createBossBar(config.getString(language + ".messages.timegive"), BarColor.PURPLE, BarStyle.SEGMENTED_20);
        compassBossBar.setProgress(1.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            compassBossBar.addPlayer(player);
        }

        compassCountdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                double progress = compassBossBar.getProgress();


                if (progress <= 0) {
                    Bukkit.getLogger().info("Countdown reached zero, giving compass to hunters.");
                    giveCompassToHunters(plugin);
                    compassBossBar.removeAll();
                    if (compassBossBar != null) {
                        compassBossBar.removeAll();
                        compassBossBar = null;
                        Bukkit.broadcastMessage(config.getString(language + ".messages.compass_give"));
                        Bukkit.getLogger().info("Compass BossBar removed.");
                    }
                    cancel();
                    return;
                }


                progress -= (1.0 / compassDelaySeconds);
                if (progress < 0) {
                    progress = 0;
                }

                compassBossBar.setProgress(progress);


                int remainingSeconds = (int) Math.ceil(progress * compassDelaySeconds);
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timeMessage = config.getString(language + ".messages.timegive") + ": " +
                        (minutes > 0 ? minutes + " m " : "") + seconds + " с";
                compassBossBar.setTitle(timeMessage);
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    public static boolean isCompassCountdownActive() {
        return compassCountdownTask != null && !compassCountdownTask.isCancelled();
    }

    private static void giveCompassToHunters(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), config.getString(language + ".messages.compass_give"));

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
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.2f, 1f);
                    if (config.getString("language").equalsIgnoreCase("ru")) {
                        player.sendTitle("Хантеры вышли на охоту!", "", 10, 60, 10); // 10 - время отображения, 60 - время паузы
                    } else {
                        player.sendTitle("Hunters are out!", "", 10, 60, 10);
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
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(teleportLocation);
            player.setBedSpawnLocation(teleportLocation, true);
        } else {
            player.sendMessage(config.getString(language + ".messages.not_found"));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hunterreload");
            player.sendMessage(config.getString(language + ".messages.plugins"));
        }
    }

    private static void clearAchievements() {
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        Bukkit.broadcastMessage(config.getString(language + ".messages.clear_ach"));

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

        if (compassBossBar != null) {
            compassBossBar.removeAll();
            compassBossBar = null;
        }

        if (compassCountdownTask != null) {
            compassCountdownTask.cancel();
            compassCountdownTask = null;
        }
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }
}
