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
        // Проверка, что команду выполняет игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cКоманду может выполнить только игрок.");
            return false;
        }
        Player player = (Player) sender;

        // Обработка команды без аргументов - открытие меню выбора команды
        if (args.length == 0) {
            GameManager.openTeamSelectionMenu(player, plugin);
        }
        // Обработка команды start - запуск игры
        else if (args[0].equalsIgnoreCase("start")) {
            if (GameManager.isGameStarted()) {
                player.sendMessage("§cИгра уже началась.");
                return false;
            }
            if (GameManager.canStartGame(plugin)) {
                GameManager.startGame(plugin);
                player.sendMessage("§aИгра началась!");
            } else {
                player.sendMessage("§cНевозможно начать игру. Убедитесь, что есть игроки в обеих командах.");
            }
        }
        // Обработка команды stop - завершение игры
        else if (args[0].equalsIgnoreCase("stop")) {
            if (!player.hasPermission("hunter.stop")) {  // Проверка прав игрока
                player.sendMessage("§cУ вас нет прав для выполнения этой команды.");
                return false;
            }
            if (GameManager.isGameStarted()) {
                GameManager.endGame();  // Завершаем игру
                Bukkit.broadcastMessage("§cИгра была завершена.");
                org.bukkit.World world = Bukkit.getWorld("world");

            } else {
                player.sendMessage("§cИгра еще не началась.");
            }
        } else {
            return false;  // Если команда не распознана
        }

        return true;
    }
}
