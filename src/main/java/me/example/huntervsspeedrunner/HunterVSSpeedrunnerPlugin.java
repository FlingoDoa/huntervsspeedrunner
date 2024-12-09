package me.example.huntervsspeedrunner;

import me.example.huntervsspeedrunner.listeners.PlayerDeathListener;
import me.example.huntervsspeedrunner.listeners.EnderDragonDeathListener;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.RandomTaskManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import me.example.huntervsspeedrunner.listeners.MenuListener;
import me.example.huntervsspeedrunner.listeners.PortalRedirectListener;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import java.io.File;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import net.md_5.bungee.api.ChatColor;


public class HunterVSSpeedrunnerPlugin extends JavaPlugin {

    private LifeManager lifeManager;
    private GameManager gameManager;
    private boolean isMenuOpen = false;
    private BossBar bossBar;

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded successfully!");
        this.saveDefaultConfig();
        initializeManagers();
        registerListeners();
        registerCommands();
    }


    private void initializeManagers() {
        this.lifeManager = new LifeManager(this);
        this.gameManager = new GameManager();
        this.randomTaskManager = new RandomTaskManager();
    }

    public RandomTaskManager getRandomTaskManager() {
        return randomTaskManager;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new EnderDragonDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalRedirectListener(this), this);
    }

    private void registerCommands() {
        if (getCommand("hunter") != null) {
            getCommand("hunter").setExecutor(this);
        }
        if (getCommand("start") != null) {
            getCommand("start").setExecutor(this);
        }
        if (getCommand("stop") != null) {
            getCommand("stop").setExecutor(this);
        }
        if (getCommand("hunterworld") != null) {
            getCommand("hunterworld").setExecutor(this);
        }
        if (getCommand("hunterreload") != null) {
            getCommand("hunterreload").setExecutor(this);
        }
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public boolean isMenuOpen() {
        return isMenuOpen;
    }

    public void setMenuOpen(boolean menuOpen) {
        this.isMenuOpen = menuOpen;
    }

    public ItemStack getCompassItem() {
        FileConfiguration config = this.getConfig();
        String materialName = config.getString("hunter.compass.item");
        String displayName = config.getString("hunter.compass.name");

        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + materialName);
        }

        ItemStack compassItem = new ItemStack(material);
        ItemMeta meta = compassItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            compassItem.setItemMeta(meta);
        }

        return compassItem;
    }

    private String getMessage(String key) {
        FileConfiguration config = this.getConfig();
        String language = config.getString("language", "en");

        return config.getString(language + ".messages." + key, "Message not found!");
    }

    private RandomTaskManager randomTaskManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hunter")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (gameManager.isGameStarted()) {
                        player.sendMessage(getMessage("game_started"));
                    } else {
                        if (!isMenuOpen()) {
                            setMenuOpen(true);
                            gameManager.openTeamSelectionMenu(player, this);
                        } else {
                            player.sendMessage(getMessage("menu_closed"));
                        }
                    }
                } else {
                    sender.sendMessage(getMessage("only_players"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("start")) {
                if (!sender.isOp()) {
                    sender.sendMessage(getMessage("no_permission"));
                    return true;
                }
                if (gameManager.canStartGame(this)) {
                    gameManager.startGame(this);
                    sender.sendMessage(getMessage("game_start_success"));
                } else {
                    sender.sendMessage(getMessage("game_start_fail"));
                }
                return true;
            }

            if (command.getName().equalsIgnoreCase("hunterworld")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command can only be executed by a player.");
                    return false;
                }

                Player player = (Player) sender;
                if (!player.hasPermission("hunter.world")) {
                    player.sendMessage("You don't have permission to execute this command.");
                    return false;
                }

                // Выполняем команды для создания миров
                executeWorldCommands(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getMessage("only_players"));
                    return false;
                }
                if (!sender.isOp()) {
                    sender.sendMessage(getMessage("no_permission"));
                    return true;
                }
                if (gameManager.isGameStarted()) {
                    gameManager.endGame();
                    sender.sendMessage(getMessage("game_stopped"));
                } else {
                    sender.sendMessage(getMessage("game_not_started"));
                }
                return true;
            }
        }
        // New command to execute a sequence of commands with delay
        if (command.getName().equalsIgnoreCase("hunterworld")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("only_players");
                return false;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("hunter.world")) {
                player.sendMessage("no_permission");
                return false;
            }

            // Execute commands with delay
            executeWorldCommands(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("hunterreload")) {
            if (!sender.isOp()) {
                sender.sendMessage(getMessage("no_permission"));
                return true;
            }
            reloadPlugin();
            sender.sendMessage("§aPlugin reloaded successfully!");
            return true;
        }

        return false;
    }

    public void reloadPlugin() {
        if (gameManager.isGameStarted()) {
            gameManager.endGame();
        }

        reloadConfig();
        initializeManagers();
        setMenuOpen(false);

        getLogger().info("Plugin reloaded successfully!");
    }

    public void executeWorldCommands(Player player) {
        if (gameManager.isGameStarted()) {
            gameManager.endGame();
        }
        FileConfiguration config = getConfig();
        String eventWorldName = config.getString("event.worldName");
        World mainWorld = Bukkit.getWorld("world"); // Основной мир

        if (mainWorld == null) {
            player.sendMessage("Main world 'world' not found. Please ensure it exists.");
            return;
        }

        player.sendMessage("Starting world regeneration...");

        bossBar = Bukkit.createBossBar("Processing commands...", BarColor.GREEN, BarStyle.SEGMENTED_10);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(onlinePlayer);
        }

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                try {
                    switch (step) {
                        case 0:
                            // Телепортация игроков из удаляемых миров
                            for (World world : Bukkit.getWorlds()) {
                                if (world.getName().equals(eventWorldName) ||
                                        world.getName().equals(eventWorldName + "_nether") ||
                                        world.getName().equals(eventWorldName + "_the_end")) {
                                    for (Player player : world.getPlayers()) {
                                        player.teleport(mainWorld.getSpawnLocation());
                                        player.sendMessage("You have been teleported to the main world.");
                                    }
                                }
                            }
                            break;

                        case 1:
                            // Удаление старого мира
                            World oldWorld = Bukkit.getWorld(eventWorldName);
                            if (oldWorld != null && Bukkit.unloadWorld(oldWorld, false)) {
                                deleteWorldFolder(oldWorld.getWorldFolder());
                            }

                            World oldNether = Bukkit.getWorld(eventWorldName + "_nether");
                            if (oldNether != null && Bukkit.unloadWorld(oldNether, false)) {
                                deleteWorldFolder(oldNether.getWorldFolder());
                            }

                            World oldEnd = Bukkit.getWorld(eventWorldName + "_the_end");
                            if (oldEnd != null && Bukkit.unloadWorld(oldEnd, false)) {
                                deleteWorldFolder(oldEnd.getWorldFolder());
                            }
                            break;

                        case 2:
                            // Создание нового обычного мира
                            WorldCreator normalWorldCreator = new WorldCreator(eventWorldName);
                            normalWorldCreator.environment(World.Environment.NORMAL);
                            Bukkit.createWorld(normalWorldCreator);
                            break;

                        case 3:
                            // Создание нижнего мира
                            WorldCreator netherWorldCreator = new WorldCreator(eventWorldName + "_nether");
                            netherWorldCreator.environment(World.Environment.NETHER);
                            Bukkit.createWorld(netherWorldCreator);
                            break;

                        case 4:
                            // Создание энда
                            WorldCreator endWorldCreator = new WorldCreator(eventWorldName + "_the_end");
                            endWorldCreator.environment(World.Environment.THE_END);
                            Bukkit.createWorld(endWorldCreator);
                            break;

                        default:
                            bossBar.removeAll();
                            bossBar = null;
                            player.sendMessage("World regeneration completed.");
                            cancel();
                            return;
                    }

                    bossBar.setProgress((double) step / 4);
                    bossBar.setTitle("Processing step " + (step + 1) + " of 5...");
                    step++;
                } catch (Exception e) {
                    player.sendMessage("An error occurred during world regeneration: " + e.getMessage());
                    bossBar.removeAll();
                    bossBar = null;
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 40L);
    }


    private void deleteWorldFolder(File worldFolder) {
        if (worldFolder == null || !worldFolder.exists()) {
            return; // Если директория не существует, выходим
        }

        File[] files = worldFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteWorldFolder(file); // Рекурсивно удаляем подкаталоги
                } else {
                    if (!file.delete()) {
                        getLogger().warning("Failed to delete file: " + file.getPath());
                    }
                }
            }
        }

        if (!worldFolder.delete()) {
            getLogger().warning("Failed to delete world folder: " + worldFolder.getPath());
        }
    }
    private String applyGradient(String text, ChatColor startColor, ChatColor endColor) {
        StringBuilder gradientText = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) length;
            ChatColor color = interpolateColor(startColor, endColor, ratio);
            gradientText.append(color).append(text.charAt(i));
        }

        return gradientText.toString();
    }

    private ChatColor interpolateColor(ChatColor startColor, ChatColor endColor, float ratio) {
        int red = (int) ((1 - ratio) * startColor.getColor().getRed() + ratio * endColor.getColor().getRed());
        int green = (int) ((1 - ratio) * startColor.getColor().getGreen() + ratio * endColor.getColor().getGreen());
        int blue = (int) ((1 - ratio) * startColor.getColor().getBlue() + ratio * endColor.getColor().getBlue());
        return ChatColor.of(new java.awt.Color(red, green, blue));
    }
}
