package me.example.huntervsspeedrunner;

import me.example.huntervsspeedrunner.listeners.PlayerDeathListener;
import me.example.huntervsspeedrunner.listeners.EnderDragonDeathListener;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import me.example.huntervsspeedrunner.listeners.MenuListener;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HunterVSSpeedrunnerPlugin extends JavaPlugin {

    private LifeManager lifeManager;
    private GameManager gameManager;  // Adding field for GameManager
    private boolean isMenuOpen = false;  // Flag to indicate if the menu is open

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded successfully!");
        this.saveDefaultConfig();  // Saving default configuration
        this.lifeManager = new LifeManager(this);
        this.gameManager = new GameManager();  // Initializing GameManager

        // Registering event listeners
        getServer().getPluginManager().registerEvents(new EnderDragonDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        // Registering commands
        if (getCommand("hunter") != null) {
            getCommand("hunter").setExecutor(this);
        } else {
            getLogger().warning("Command 'hunter' not found in plugin.yml");
        }

        if (getCommand("start") != null) {
            getCommand("start").setExecutor(this);  // Registering start command
        } else {
            getLogger().warning("Command 'start' not found in plugin.yml");
        }

        if (getCommand("stop") != null) {
            getCommand("stop").setExecutor(this);   // Registering stop command
        } else {
            getLogger().warning("Command 'stop' not found in plugin.yml");
        }

        if (getCommand("hunterworld") != null) {
            getCommand("hunterworld").setExecutor(this); // Registering new command "hunterworld"
        } else {
            getLogger().warning("Command 'hunterworld' not found in plugin.yml");
        }
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public GameManager getGameManager() {
        return gameManager;  // Method to get GameManager
    }

    public boolean isMenuOpen() {
        return isMenuOpen;
    }

    public void setMenuOpen(boolean menuOpen) {
        this.isMenuOpen = menuOpen;
    }

    // Adding method to get compass item from the configuration
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Command /hunter (open menu)
        if (command.getName().equalsIgnoreCase("hunter")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (gameManager.isGameStarted()) {  // Check game status through GameManager
                        player.sendMessage("§cThe game has already started! Cannot open menu.");
                    } else {
                        if (!isMenuOpen()) {  // Check if the menu is not already open
                            setMenuOpen(true);  // Set flag to indicate that the menu is open
                            gameManager.openTeamSelectionMenu(player, this);  // Open menu through GameManager
                        } else {
                            player.sendMessage("§cMenu is already open!");
                        }
                    }
                } else {
                    sender.sendMessage("Command can only be used by players.");
                }
                return true;
            }

            // If argument is "start" or "stop" — for operators
            if (args[0].equalsIgnoreCase("start")) {
                if (!sender.isOp()) {  // Check if sender is an operator
                    sender.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }
                if (gameManager.canStartGame(this)) {  // Start game through GameManager
                    gameManager.startGame(this);
                    sender.sendMessage("§aGame started!");
                } else {
                    sender.sendMessage("§cGame cannot start: there must be at least one Hunter and one Speedrunner.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Command /stop can only be used by players.");
                    return false;
                }
                if (!sender.isOp()) {  // Check if sender is an operator
                    sender.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }
                if (gameManager.isGameStarted()) {  // End game through GameManager
                    gameManager.endGame();
                    sender.sendMessage("§aGame ended!");
                } else {
                    sender.sendMessage("§cGame cannot be ended as it has not started.");
                }
                return true;
            }
        }

        // New command to execute a sequence of commands with delay
        if (command.getName().equalsIgnoreCase("hunterworld")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can execute this command.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("hunter.world")) {
                player.sendMessage("§cYou do not have permission to execute this command.");
                return false;
            }

            // Execute commands with delay
            executeWorldCommands(player);
            return true;
        }

        return false;
    }

    // Method to execute a sequence of commands with delay
    private void executeWorldCommands(Player player) {
        // Получаем название мира из конфигурации
        FileConfiguration config = getConfig();
        String eventWorldName = config.getString("event.worldName"); // Default "Event" if not found

        player.sendMessage("World regeneration has begun, please wait...");

        new BukkitRunnable() {
            int step = 0;
            final String[] commands = {
                    "mv delete " + eventWorldName,
                    "mv confirm",
                    "mv delete " + eventWorldName + "_nether",
                    "mv confirm",
                    "mv delete " + eventWorldName + "the_end",
                    "mv confirm",
                    "mv create " + eventWorldName + " world",
                    "mv create " + eventWorldName + "_nether nether",
                    "mv create " + eventWorldName + "the_end end"
            };

            @Override
            public void run() {
                if (step < commands.length) {
                    // Execute command
                    String command = commands[step];
                    getServer().dispatchCommand(getServer().getConsoleSender(), command);
                    step++;
                } else {
                    cancel(); // Stop executing commands
                    player.sendMessage("All commands have been executed.");
                }
            }
        }.runTaskTimer(this, 0L, 40L); // 40L ticks = 2 seconds
    }

}
