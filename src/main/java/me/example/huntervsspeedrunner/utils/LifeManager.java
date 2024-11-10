package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifeManager {

    private final HunterVSSpeedrunnerPlugin plugin;
    private final Scoreboard scoreboard;
    private Team hunters;
    private Team speedrunners;

    // Хранение жизней для каждого спидраннера
    private final Map<String, Integer> playerLives = new HashMap<>();

    public LifeManager(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        this.scoreboard = scoreboardManager.getMainScoreboard();


        if (scoreboard.getTeam("speedrunners") == null) {
            speedrunners = scoreboard.registerNewTeam("speedrunners");
            speedrunners.setDisplayName(ChatColor.GREEN + "Спидраннеры");
        } else {
            speedrunners = scoreboard.getTeam("speedrunners");
        }

        if (scoreboard.getTeam("hunters") == null) {
            hunters = scoreboard.registerNewTeam("hunters");
            hunters.setDisplayName(ChatColor.RED + "Охотники");
        } else {
            hunters = scoreboard.getTeam("hunters");
        }

        Objective objective = scoreboard.getObjective("dummy");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("dummy", "dummy", ChatColor.YELLOW + "Жизни");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        initializeScoreboard();
    }
    public int getPlayerLives(Player player) {
        return playerLives.getOrDefault(player.getName(), 0);
    }

    // Метод для подсчета суммарных жизней всех спидраннеров
    public int getTotalSpeedrunnerLives() {
        return playerLives.values().stream().mapToInt(Integer::intValue).sum();
    }

    // Инициализация scoreboard для всех игроков
    public void initializeScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    // Обновление отображения жизней на scoreboard
    public void updateScoreboard() {
        Objective objective = scoreboard.getObjective("dummy");

        // Сброс текущих записей
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Обновление жизней для каждого спидраннера
        for (Map.Entry<String, Integer> entry : playerLives.entrySet()) {
            String playerName = entry.getKey();
            int lives = entry.getValue();
            objective.getScore(ChatColor.AQUA + playerName + ": " ).setScore(lives);
        }
    }

    // Добавление игрока в команду Охотников
    public void setHunter(Player player) {
        speedrunners.removeEntry(player.getName());
        hunters.addEntry(player.getName());
        playerLives.remove(player.getName());
        updateScoreboard();
    }

    // Добавление игрока в команду Спидраннеров с дефолтным количеством жизней (1)
    public void setSpeedrunner(Player player) {
        hunters.removeEntry(player.getName());
        speedrunners.addEntry(player.getName());
        playerLives.put(player.getName(), 1);
        updateScoreboard();
    }

    // Проверка, является ли игрок Спидраннером
    public boolean isSpeedrunner(Player player) {
        return speedrunners.hasEntry(player.getName());
    }

    // Проверка, является ли игрок Охотником
    public boolean isHunter(Player player) {
        return hunters.hasEntry(player.getName());
    }

    // Добавить жизнь спидраннеру
    public void addLife(Player player) {
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) + 1;
            playerLives.put(player.getName(), lives);
            player.sendMessage(ChatColor.GREEN + "Вам добавлена жизнь! Текущие жизни: " + lives);
            updateScoreboard();
        }
    }

    // Уменьшить жизнь спидраннеру
    public void removeLife(Player player) {
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) - 1;
            if (lives <= 0) {
                player.sendMessage(ChatColor.RED + "Ваши жизни закончились!");
                playerLives.remove(player.getName());
            } else {
                playerLives.put(player.getName(), lives);
                player.sendMessage(ChatColor.RED + "Ваша жизнь уменьшена! Текущие жизни: " + lives);
            }
            updateScoreboard();
        }
    }

    // Получить список охотников
    public List<String> getHunters() {
        return new ArrayList<>(hunters.getEntries());
    }

    // Получить список спидраннеров
    public List<String> getSpeedrunners() {
        return new ArrayList<>(speedrunners.getEntries());
    }
}
