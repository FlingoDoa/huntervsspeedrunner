package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.random.RandomTaskManager;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import net.md_5.bungee.api.ChatColor;
import java.util.List;
import java.util.ArrayList;
import java.io.File;




public class GameManager {

    private static boolean gameStarted = false;
    private static BukkitTask compassCountdownTask;
    private static BossBar compassBossBar;
    private static BukkitTask hunterTeleportTask;
    private static BukkitTask locatorBarTask;


    public static void startGame(HunterVSSpeedrunnerPlugin plugin) {
        try {
            FileConfiguration config = plugin.getConfig();
            RandomTaskManager randomTaskManager = plugin.getRandomTaskManager();
            LifeManager lifeManager = plugin.getLifeManager();

            if (gameStarted) {
                return;
            }

            gameStarted = true;
            lifeManager.initializeScoreboard();

            if (locatorBarTask != null && !locatorBarTask.isCancelled()) {
                locatorBarTask.cancel();
            }
            locatorBarTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locatorBar false");
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not enforce locatorBar gamerule: " + e.getMessage());
                        cancel();
                        locatorBarTask = null;
                    }
                }
            }.runTaskTimer(plugin, 0L, 100L);

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    plugin.getPlayerDataManager().savePlayerData(player);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при сохранении данных игрока " + player.getName() + ": " + e.getMessage());
                }
            }

            if (randomTaskManager.isRandomModeEnabled()) {
                randomTaskManager.generateTasksForAllSpeedrunners();
            } else {
                randomTaskManager.showNonRandomBossBar();
            }

            startCompassCountdown(plugin);

            String messagePath = config.getString("language", "en") + ".messages.game_start_success";
            String message = config.getString(messagePath, "§aThe game has started!");
            Bukkit.broadcastMessage(message);

            if (plugin.getErrorReporter() != null && plugin.getErrorReporter().isWebhookConfigured()) {
                int hunters = lifeManager.getHunters().size();
                int speedrunners = lifeManager.getSpeedrunners().size();
                boolean randomEnabled = randomTaskManager.isRandomModeEnabled();

                StringBuilder details = new StringBuilder();
                details.append("Стартовала игра.\n");
                details.append("Игроков: охотники = ").append(hunters)
                        .append(", спидраннеры = ").append(speedrunners).append("\n");
                details.append("Рандомные задания: ").append(randomEnabled ? "включены" : "выключены").append("\n");
                details.append("Версия плагина: ").append(getPluginVersion(plugin)).append("\n");
                details.append("Версия Minecraft: ").append(Bukkit.getVersion()).append("\n");

                plugin.getErrorReporter().reportGameEvent("Игра запущена", details.toString());
            }

            teleportSpeedrunners(plugin);
            teleportHuntersDelayed(plugin);
        } catch (Throwable e) {
            plugin.getLogger().severe("Критическая ошибка при запуске игры: " + e.getMessage());
            e.printStackTrace();
            gameStarted = false;
        }
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

        ItemStack toggleRandomItem = createMenuItem(config, config.getString("language") + ".menu.toggle_random", randomModeStatus);
        inventory.setItem(10, toggleRandomItem);

        String restartStatus = getEventWorldStatus(plugin);
        ItemStack restartWorldsItem = createMenuItem(config, config.getString("language") + ".menu.restart_world", restartStatus);
        inventory.setItem(12, restartWorldsItem);

        ItemStack toggleCompassItem = createMenuItem(config, config.getString("language") + ".menu.toggle_compass", compassStatus);
        inventory.setItem(14, toggleCompassItem);

        ItemStack configButton = createMenuItem(config, config.getString("language") + ".menu.setconf", "");
        inventory.setItem(16, configButton);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    public static void updateEventWorldStatusInOpenMenus(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory() == null) continue;
            Inventory top = p.getOpenInventory().getTopInventory();
            String title = ChatColor.stripColor(p.getOpenInventory().getTitle());
            if (!"Select a Team".equals(title)) continue;

            String status = getEventWorldStatus(plugin);
            ItemStack restartWorldsItem = createMenuItem(config, config.getString("language") + ".menu.restart_world", status);
            if (restartWorldsItem != null) {
                top.setItem(12, restartWorldsItem);
            }
        }
    }

    private static String getEventWorldStatus(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String eventWorldName = config.getString("event.worldName", "Event");


        if (plugin.isWorldRegenerating()) {
            int dots = plugin.getWorldRegenDots() % 3;
            String suffix = dots == 0 ? "." : dots == 1 ? ".." : "...";
            return ChatColor.YELLOW + " " + suffix;
            }

        File worldContainer = Bukkit.getWorldContainer();
        File worldFolder = new File(worldContainer, eventWorldName);
        World world = Bukkit.getWorld(eventWorldName);

        if (!worldFolder.exists() && world == null) {
            return ChatColor.RED + " ✖";
        }

        if (world != null) {
            return ChatColor.GREEN + " ✔";
        }

        return ChatColor.YELLOW + " ✔";
    }

    private static ItemStack createTeamMenuItem(FileConfiguration config, String path, List<Player> players) {
        String materialName = config.getString(path + ".item");
        String language = config.getString("language", "en");
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
                lore.add(org.bukkit.ChatColor.RED + config.getString(language + ".messages.not_take"));
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


            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

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
                        Bukkit.broadcastMessage(config.getString(config.getString("language") + ".messages.compass_give"));
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
                String timeMessage = config.getString(config.getString("language") + ".messages.timegive") + ": " +
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
        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter) && !hunter.getInventory().contains(Material.COMPASS)) {
                ItemStack compass = new ItemStack(Material.COMPASS);
                ItemMeta meta = compass.getItemMeta();

                if (meta instanceof CompassMeta) {
                    CompassMeta compassMeta = (CompassMeta) meta;
                    compassMeta.setLodestoneTracked(false);
                    compass.setItemMeta(compassMeta);
                }

                hunter.getInventory().addItem(compass);
                updateHunterCompass(plugin, hunter);
            }
        }
    }

    private static ItemStack getCompassFromInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return item;
            }
        }
        return null;
    }

    private static void updateHunterCompass(HunterVSSpeedrunnerPlugin plugin, Player hunter) {
        ItemStack compass = getCompassFromInventory(hunter);
        if (compass == null) {
            hunter.sendMessage("§cУ вас нет компаса!");
            return;
        }

        Player target = plugin.getCompassManager().getCurrentTarget(hunter);
        if (target == null || !target.isOnline()) {
            hunter.sendMessage("§cЦель недоступна. Компас не обновлен.");
            return;
        }

        Location targetLocation = target.getLocation();
        World hunterWorld = hunter.getWorld();
        World targetWorld = target.getWorld();

        boolean crossWorldTracking = plugin.getConfig().getBoolean("compass.allow_cross_world_tracking", false);

        if (!crossWorldTracking && !hunterWorld.equals(targetWorld)) {
            hunter.sendMessage("§cЦель в другом мире. Компас не обновлен.");
            return;
        }

        ItemMeta meta = compass.getItemMeta();
        if (meta instanceof CompassMeta) {
            CompassMeta compassMeta = (CompassMeta) meta;

            if (hunterWorld.getEnvironment() == World.Environment.NETHER && targetWorld.getEnvironment() == World.Environment.NETHER) {
                Block lodestone = targetLocation.getBlock();
                if (lodestone.getType() != Material.LODESTONE) {
                    lodestone.setType(Material.LODESTONE);
                }
                compassMeta.setLodestone(targetLocation);
                compassMeta.setLodestoneTracked(true);
                hunter.sendMessage("§aКомпас привязан к Lodestone в Аду.");
            } else {
                compassMeta.setLodestone(null);
                hunter.setCompassTarget(targetLocation);
                hunter.sendMessage("§aКомпас обновлен на " + targetWorld.getName() + " ("
                        + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ() + ")");
            }

            compass.setItemMeta(compassMeta);
        }
    }


    private static void teleportSpeedrunners(HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

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
        int teleportDelay = config.getInt("hunter.teleportDelay");

        if (hunterTeleportTask != null && !hunterTeleportTask.isCancelled()) {
            hunterTeleportTask.cancel();
        }

        Bukkit.getLogger().info(config.getString("language").equalsIgnoreCase("ru") ?
                "Задержка телепорта хантеров: " + teleportDelay + " секунд." :
                "Hunter teleport delay: " + teleportDelay + " seconds.");

        BukkitRunnable hunterTeleportRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameStarted) {
                    cancel();
                    return;
                }
                
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
        };
        hunterTeleportTask = hunterTeleportRunnable.runTaskLater(plugin, 20L * teleportDelay);
    }

    private static void teleportToEventWorld(Player player, HunterVSSpeedrunnerPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String eventWorldName = config.getString("event.worldName");
        String language = config.getString("language", "en");

        World eventWorld = Bukkit.getWorld(eventWorldName);
        if (eventWorld != null) {
            Location teleportLocation = new Location(eventWorld, 0, eventWorld.getHighestBlockYAt(0, 0) + 2, 0);
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(teleportLocation);
            player.setBedSpawnLocation(teleportLocation, true);
        } else {
            String notFoundMsg = config.getString(language + ".messages.not_found");
            String pluginsMsg = config.getString(language + ".messages.plugins");
            if (notFoundMsg != null && !notFoundMsg.isEmpty()) {
                player.sendMessage(notFoundMsg);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hunterreload");
            if (pluginsMsg != null && !pluginsMsg.isEmpty()) {
                player.sendMessage(pluginsMsg);
            }
        }
    }

    public static void endGame(HunterVSSpeedrunnerPlugin plugin) {
        endGame(plugin, null, null);
    }

    public static void endGame(HunterVSSpeedrunnerPlugin plugin, String winner, String reason) {
        gameStarted = false;
        
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

        if (hunterTeleportTask != null && !hunterTeleportTask.isCancelled()) {
            hunterTeleportTask.cancel();
            hunterTeleportTask = null;
        }

        if (locatorBarTask != null && !locatorBarTask.isCancelled()) {
            locatorBarTask.cancel();
            locatorBarTask = null;
        }
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locatorBar true");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not restore locatorBar gamerule: " + e.getMessage());
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

        if (plugin.getErrorReporter() != null && plugin.getErrorReporter().isWebhookConfigured()) {
            int hunters = lifeManager.getHunters().size();
            int speedrunners = lifeManager.getSpeedrunners().size();
            StringBuilder details = new StringBuilder();
            details.append("Игра завершена.\n");
            if (winner != null) {
                details.append("Победитель: ").append(winner).append("\n");
            }
            if (reason != null) {
                details.append("Причина окончания: ").append(reason).append("\n");
            }
            details.append("Оставшихся охотников: ").append(hunters)
                    .append(", спидраннеров: ").append(speedrunners).append("\n");
            details.append("Версия плагина: ").append(getPluginVersion(plugin)).append("\n");
            details.append("Версия Minecraft: ").append(Bukkit.getVersion()).append("\n");

            plugin.getErrorReporter().reportGameEvent("Игра завершена", details.toString());
        }
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

    private static String getPluginVersion(HunterVSSpeedrunnerPlugin plugin) {
        String version = plugin.getConfig().getString("plugin_version", null);
        if (version == null || version.isEmpty()) {
            version = plugin.getDescription().getVersion();
        }
        return version;
    }
    
}

