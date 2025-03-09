package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.random.RandomTaskManager;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import me.example.huntervsspeedrunner.random.Task;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatColor;
import java.util.List;
import java.util.ArrayList;




public class GameManager {

    private static boolean gameStarted = false;
    private static BukkitTask compassCountdownTask;
    private static BossBar compassBossBar;


    public static void startGame(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        RandomTaskManager randomTaskManager = plugin.getRandomTaskManager();
        LifeManager lifeManager = plugin.getLifeManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getPlayerDataManager().savePlayerData(player);
            if (randomTaskManager.isRandomModeEnabled() && lifeManager.isSpeedrunner(player)) {
                Task randomTask = randomTaskManager.generateRandomTask(player);
                randomTaskManager.showTaskToAllPlayers(randomTask);
                randomTaskManager.startTaskChecking(player, randomTask);
            }

        }

        if (gameStarted) {
            return;
        }

        gameStarted = true;
        lifeManager.initializeScoreboard();

        startCompassCountdown(plugin);

        String language = config.getString("language", "en");
        String messagePath = language + ".messages.game_start_success";
        String message = config.getString(messagePath, "§aThe game has started!");
        Bukkit.broadcastMessage(message);

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
        Inventory inventory = Bukkit.createInventory(null, 18, "Select a Team");

        boolean randomEnabled = plugin.getRandomTaskManager().isRandomModeEnabled();
        boolean compassEnabled = plugin.getCompassManager().isCompassEnabled(player);

        String randomModeStatus = randomEnabled ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌";
        String compassStatus = compassEnabled ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌";

        // Создание предметов для меню
        ItemStack hunterItem = createTeamMenuItem(config, config.getString("language") + ".menu.hunter", plugin.getLifeManager().getHunters());
        inventory.setItem(6, hunterItem);

        ItemStack speedrunnerItem = createTeamMenuItem(config, config.getString("language") + ".menu.speedrunner", plugin.getLifeManager().getSpeedrunners());
        inventory.setItem(2, speedrunnerItem);


        ItemStack addLifeItem = createMenuItem(config, config.getString("language") + ".menu.add_life", "");
        inventory.setItem(0, addLifeItem);

        ItemStack removeLifeItem = createMenuItem(config, config.getString("language") + ".menu.remove_life", "");
        inventory.setItem(8, removeLifeItem);


        ItemStack startItem = createMenuItem(config, config.getString("language") + ".menu.start", "");
        inventory.setItem(4, startItem);

        // Новые кнопки
        ItemStack toggleRandomItem = createMenuItem(config, config.getString("language") + ".menu.toggle_random", randomModeStatus);
        inventory.setItem(10, toggleRandomItem);

        ItemStack restartWorldsItem = createMenuItem(config, config.getString("language") + ".menu.restart_world", "");
        inventory.setItem(12, restartWorldsItem);

        ItemStack toggleCompassItem = createMenuItem(config, config.getString("language") + ".menu.toggle_compass", compassStatus);
        inventory.setItem(14, toggleCompassItem);

        ItemStack reloadPluginItem = createMenuItem(config, config.getString("language") + ".menu.reload_plugin", "");
        inventory.setItem(16, reloadPluginItem);

        player.openInventory(inventory);
    }

    private static ItemStack createTeamMenuItem(FileConfiguration config, String path, List<Player> players) {
        String materialName = config.getString(path + ".item");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            return null;
        }

        String displayName = config.getString(path + ".name");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName + " (" + players.size() + ")");

            List<String> lore = new ArrayList<>();
            if (players.isEmpty()) {
                lore.add(ChatColor.RED + "Пока никто не выбрал этот класс!");
            } else {
                for (Player p : players) {
                    lore.add(ChatColor.GRAY + "- " + p.getName());
                }
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());

            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createMenuItem(FileConfiguration config, String path, String extraInfo) {
        String materialName = config.getString(path + ".item");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            return null;
        }

        String displayName = config.getString(path + ".name");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName + " " + extraInfo);


            meta.addItemFlags(ItemFlag.values());
            meta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        } else {
            Bukkit.getLogger().warning("ItemMeta is null for material: " + material);
        }

        return item;
    }



    private static String applyGradient(String text, ChatColor startColor, ChatColor endColor) {
        StringBuilder gradientText = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            ChatColor color = interpolateColor(startColor, endColor, ratio);
            gradientText.append(color).append(ChatColor.BOLD).append(text.charAt(i));
        }

        return gradientText.toString();
    }


    private static ChatColor interpolateColor(ChatColor startColor, ChatColor endColor, float ratio) {
        int red = (int) ((1 - ratio) * startColor.getColor().getRed() + ratio * endColor.getColor().getRed());
        int green = (int) ((1 - ratio) * startColor.getColor().getGreen() + ratio * endColor.getColor().getGreen());
        int blue = (int) ((1 - ratio) * startColor.getColor().getBlue() + ratio * endColor.getColor().getBlue());
        try {
            return ChatColor.of(new java.awt.Color(red, green, blue));
        } catch (NoSuchMethodError e) {
            return getNearestChatColor(new java.awt.Color(red, green, blue));
        }
    }

    private static ChatColor getNearestChatColor(java.awt.Color color) {
        ChatColor closestColor = ChatColor.WHITE;
        double closestDistance = Double.MAX_VALUE;

        for (ChatColor chatColor : ChatColor.values()) {
            if (chatColor.getColor() == null) continue;

            java.awt.Color chatColorRGB = chatColor.getColor();
            double distance = Math.pow(chatColorRGB.getRed() - color.getRed(), 2) +
                    Math.pow(chatColorRGB.getGreen() - color.getGreen(), 2) +
                    Math.pow(chatColorRGB.getBlue() - color.getBlue(), 2);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestColor = chatColor;
            }
        }
        return closestColor;
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
        compassBossBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_20);
        compassBossBar.setProgress(1.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            compassBossBar.addPlayer(player);
        }

        compassCountdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                double progress = compassBossBar.getProgress();

                if (progress <= 0) {
                    giveCompassToHunters(plugin);
                    compassBossBar.removeAll();
                    if (compassBossBar != null) {
                        compassBossBar.removeAll();
                        compassBossBar = null;
                        Bukkit.broadcastMessage(config.getString(language + ".messages.compass_give"));
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
                        (minutes > 0 ? minutes + "m " : "") + seconds + "s";

                String gradientText = applyGradient(timeMessage, ChatColor.BLUE, ChatColor.DARK_PURPLE);

                compassBossBar.setTitle(gradientText);
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

        World eventWorld = Bukkit.getWorld(config.getString("event.worldName"));
        if (eventWorld != null) {
            eventWorld.setTime(1000);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                LifeManager lifeManager = plugin.getLifeManager();
                if (lifeManager.isSpeedrunner(player) || lifeManager.isHunter(player)) {
                    player.getInventory().clear();
                    player.setTotalExperience(0);
                    player.setLevel(0);
                    player.setExp(0);

                    Bukkit.advancementIterator().forEachRemaining(advancement ->
                            player.getAdvancementProgress(advancement).getAwardedCriteria()
                                    .forEach(criteria -> player.getAdvancementProgress(advancement).revokeCriteria(criteria))
                    );
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10 * 20, 255, false, false));
                    if (lifeManager.isSpeedrunner(player)) {
                        teleportToEventWorld(player, plugin);
                        new SpawnTask(plugin, player).runTaskTimer(plugin, 0L, 20L * 5);
                    }
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
                        player.sendTitle("Хантеры вышли на охоту!", "", 10, 60, 10);
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

    public static void endGame(HunterVSSpeedrunnerPlugin plugin) {
        gameStarted = false;
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        LifeManager lifeManager = plugin.getLifeManager();

        plugin.getRandomTaskManager().hideTaskFromAllPlayers();
        if (compassBossBar != null) {
            compassBossBar.removeAll();
            compassBossBar = null;
        }
        if (compassCountdownTask != null) {
            compassCountdownTask.cancel();
            compassCountdownTask = null;
        }

        lifeManager.resetPlayers();
        teleportPlayersToDefaultWorld(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

                    if (!player.isDead()) {
                        plugin.getPlayerDataManager().loadPlayerData(player);
                    } else {
                        waitForRespawn(player, plugin);
                    }
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    private static void waitForRespawn(Player player, HunterVSSpeedrunnerPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isDead()) {
                    plugin.getPlayerDataManager().loadPlayerData(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private static void teleportPlayersToDefaultWorld(HunterVSSpeedrunnerPlugin plugin) {

        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld == null) {
            return;
        }

        Location spawnLocation = defaultWorld.getSpawnLocation();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        player.setGameMode(GameMode.ADVENTURE);
                        player.teleport(spawnLocation);
                    }
                }
            }
        }.runTaskLater(plugin, 40L);
    }


    public static boolean isGameStarted() {
        return gameStarted;
    }
}

