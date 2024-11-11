package me.example.huntervsspeedrunner.commands;

import org.bukkit.Bukkit;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HunterCommand implements CommandExecutor {

    private final HunterVSSpeedrunnerPlugin plugin;

    public HunterCommand(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can execute this command.");
            return false;
        }
        Player player = (Player) sender;

        // Handle command with no arguments - open team selection menu
        if (args.length == 0) {
            GameManager.openTeamSelectionMenu(player, plugin);
        }
        // Handle "start" command - start the game
        else if (args[0].equalsIgnoreCase("start")) {
            if (GameManager.isGameStarted()) {
                player.sendMessage("§cThe game has already started.");
                return false;
            }
            if (GameManager.canStartGame(plugin)) {
                GameManager.startGame(plugin);
                player.sendMessage("§aThe game has started!");
            } else {
                player.sendMessage("§cUnable to start the game. Ensure there are players in both teams.");
            }
        }
        // Handle "stop" command - end the game
        else if (args[0].equalsIgnoreCase("stop")) {
            if (!player.hasPermission("hunter.stop")) {  // Check player permissions
                player.sendMessage("§cYou do not have permission to execute this command.");
                return false;
            }
            if (GameManager.isGameStarted()) {
                GameManager.endGame();  // End the game
                Bukkit.broadcastMessage("§cThe game has been stopped.");
                org.bukkit.World world = Bukkit.getWorld("world");

            } else {
                player.sendMessage("§cThe game has not started yet.");
            }
        } else {
            return false;  // Command not recognized
        }

        return true;
    }
}
