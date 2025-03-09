package me.example.huntervsspeedrunner.commands;

import org.bukkit.Bukkit;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class HunterCommand implements CommandExecutor {

    private final HunterVSSpeedrunnerPlugin plugin;

    public HunterCommand(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getString(language + ".messages.only_players"));
            return false;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            GameManager.openTeamSelectionMenu(player, plugin);
        }
        else if (args[0].equalsIgnoreCase("start")) {
            if (GameManager.isGameStarted()) {
                player.sendMessage(config.getString(language + ".messages.game_started"));
                return false;
            }
            if (GameManager.canStartGame(plugin)) {
                GameManager.startGame(plugin);
                player.sendMessage(config.getString(language + ".messages.game_start_success"));
            } else {
                player.sendMessage(config.getString(language + ".messages.game_start_fail"));
            }
        }
        else if (args[0].equalsIgnoreCase("stop")) {
            if (!player.hasPermission("hunter.stop")) {
                player.sendMessage(config.getString(language + ".messages.no_permission"));
                return false;
            }
            if (GameManager.isGameStarted()) {
                GameManager.endGame(plugin);
                Bukkit.broadcastMessage(config.getString(language + ".messages.game_stopped"));
            } else {
                player.sendMessage(config.getString(language + ".messages.game_not_started"));
            }
        } else {
            return false;
        }

        return true;
    }
}
