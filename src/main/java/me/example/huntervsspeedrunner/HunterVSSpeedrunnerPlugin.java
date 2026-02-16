package me.example.huntervsspeedrunner;

import me.example.huntervsspeedrunner.listeners.PlayerDeathListener;
import me.example.huntervsspeedrunner.listeners.EnderDragonDeathListener;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.random.RandomTaskManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import me.example.huntervsspeedrunner.utils.PlayerDataManager;
import me.example.huntervsspeedrunner.listeners.MenuListener;
import me.example.huntervsspeedrunner.listeners.PortalRedirectListener;
import me.example.huntervsspeedrunner.listeners.CompassClickListener;
import me.example.huntervsspeedrunner.menus.SetConfig;
import me.example.huntervsspeedrunner.utils.CompassManager;
import me.example.huntervsspeedrunner.utils.Feedback;
import me.example.huntervsspeedrunner.random.BossBarJoinListener;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import me.example.huntervsspeedrunner.utils.ErrorReporter;
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


public class HunterVSSpeedrunnerPlugin extends JavaPlugin {

    private LifeManager lifeManager;
    private GameManager gameManager;
    private BossBar bossBar;
    private RandomTaskManager randomTaskManager;
    private CompassManager compassManager;
    private PlayerDataManager playerDataManager;
    private SetConfig setConfig;
    private ErrorReporter reporter;
    private boolean worldRegenerating = false;
    private int worldRegenDots = 0;

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded successfully!");
        this.saveDefaultConfig();
        
        String webhookUrl = ErrorReporter.decryptWebhookUrl();
        this.reporter = new ErrorReporter(this, webhookUrl);

        new Feedback(this).forceUpdateIfNeeded();
        initializeManagers();
        registerListeners();
        registerCommands();
        this.setConfig = new SetConfig(this);
        this.compassManager = new CompassManager();
        this.playerDataManager = new PlayerDataManager(this.getDataFolder(), this.lifeManager);
        getServer().getPluginManager().registerEvents(new BossBarJoinListener(randomTaskManager), this);
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (isFromOurPlugin(throwable)) {
                this.reporter.report(throwable instanceof Exception ? (Exception) throwable : new Exception(throwable),
                    "Необработанная ошибка в потоке: " + thread.getName());
            }
        });
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CompassManager getCompassManager() {
        return compassManager;
    }

    public SetConfig getSetConfig() {
        return setConfig;
    }

    public boolean isWorldRegenerating() {
        return worldRegenerating;
    }

    public int getWorldRegenDots() {
        return worldRegenDots;
    }

    public void setWorldRegenerating(boolean worldRegenerating) {
        this.worldRegenerating = worldRegenerating;
        if (!worldRegenerating) {
            this.worldRegenDots = 0;
        }
    }

    public void tickWorldRegenDots() {
        this.worldRegenDots = (worldRegenDots + 1) % 3;
    }

    private void initializeManagers() {
        this.lifeManager = new LifeManager(this);
        this.gameManager = new GameManager();
        this.randomTaskManager = new RandomTaskManager(this.getDataFolder(), this.getConfig(), this);
    }

    public RandomTaskManager getRandomTaskManager() {
        return randomTaskManager;
    }

    public ErrorReporter getErrorReporter() {
        return reporter;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new EnderDragonDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalRedirectListener(this), this);
        getServer().getPluginManager().registerEvents(new CompassClickListener(this), this);
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

    /** Проверяет, что исключение возникло в коде нашего плагина (для вебхука только свои ошибки). */
    private static boolean isFromOurPlugin(Throwable throwable) {
        String ourPackage = "me.example.huntervsspeedrunner";
        for (StackTraceElement el : throwable.getStackTrace()) {
            if (el.getClassName() != null && el.getClassName().startsWith(ourPackage)) {
                return true;
            }
        }
        Throwable cause = throwable.getCause();
        return cause != null && isFromOurPlugin(cause);
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public GameManager getGameManager() {
        return gameManager;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hunter")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (GameManager.isGameStarted()) {
                        player.sendMessage(getMessage("game_started"));
                    } else {
                        GameManager.openTeamSelectionMenu(player, this);
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
                if (GameManager.canStartGame(this)) {
                    GameManager.startGame(this);
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
                if (GameManager.isGameStarted()) {
                    GameManager.endGame(this);
                    sender.sendMessage(getMessage("game_stopped"));
                } else {
                    sender.sendMessage(getMessage("game_not_started"));
                }
                return true;
            }
        }
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
        if (GameManager.isGameStarted()) {
            GameManager.endGame(this);
        }

        reloadConfig();
        new Feedback(this).forceUpdateIfNeeded();

        getLogger().info("Плагин успешно перезагружен!");
    }

    public void executeWorldCommands(Player player) {
        if (GameManager.isGameStarted()) {
            GameManager.endGame(this);
        }
        FileConfiguration config = getConfig();
        String eventWorldName = config.getString("event.worldName");
        World mainWorld = Bukkit.getWorld("world");

        if (mainWorld == null) {
            player.sendMessage("Main world 'world' not found. Please ensure it exists.");
            return;
        }

        player.sendMessage("Starting world regeneration...");

        setWorldRegenerating(true);
        worldRegenDots = 0;
        GameManager.updateEventWorldStatusInOpenMenus(this);

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
                            WorldCreator normalWorldCreator = new WorldCreator(eventWorldName);
                            normalWorldCreator.environment(World.Environment.NORMAL);
                            Bukkit.createWorld(normalWorldCreator);
                            break;

                        case 3:
                            WorldCreator netherWorldCreator = new WorldCreator(eventWorldName + "_nether");
                            netherWorldCreator.environment(World.Environment.NETHER);
                            Bukkit.createWorld(netherWorldCreator);
                            break;

                        case 4:
                            WorldCreator endWorldCreator = new WorldCreator(eventWorldName + "_the_end");
                            endWorldCreator.environment(World.Environment.THE_END);
                            Bukkit.createWorld(endWorldCreator);
                            break;

                        default:
                            bossBar.removeAll();
                            bossBar = null;
                            player.sendMessage("World regeneration completed.");
                            setWorldRegenerating(false);
                            GameManager.updateEventWorldStatusInOpenMenus(HunterVSSpeedrunnerPlugin.this);
                            cancel();
                            return;
                    }

                    bossBar.setProgress((double) step / 4);
                    bossBar.setTitle("Processing step " + (step + 1) + " of 5...");
                    tickWorldRegenDots();
                    GameManager.updateEventWorldStatusInOpenMenus(HunterVSSpeedrunnerPlugin.this);
                    step++;
                } catch (Exception e) {
                    player.sendMessage("An error occurred during world regeneration: " + e.getMessage());
                    bossBar.removeAll();
                    bossBar = null;
                    setWorldRegenerating(false);
                    GameManager.updateEventWorldStatusInOpenMenus(HunterVSSpeedrunnerPlugin.this);
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 40L);
    }


    private void deleteWorldFolder(File worldFolder) {
        if (worldFolder == null || !worldFolder.exists()) {
            return;
        }

        File[] files = worldFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteWorldFolder(file);
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
}
