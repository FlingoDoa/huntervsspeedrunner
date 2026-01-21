package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifeManager {

    private final HunterVSSpeedrunnerPlugin plugin;
    private final Scoreboard scoreboard;
    private Team hunters;
    private Team speedrunners;

    private final Map<String, Integer> playerLives = new HashMap<>();

    public LifeManager(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        this.scoreboard = scoreboardManager.getMainScoreboard();

        if (scoreboard.getTeam("speedrunners") == null) {
            speedrunners = scoreboard.registerNewTeam("speedrunners");
            speedrunners.setDisplayName(ChatColor.GREEN + "Speedrunners");
        } else {
            speedrunners = scoreboard.getTeam("speedrunners");
        }

        if (scoreboard.getTeam("hunters") == null) {
            hunters = scoreboard.registerNewTeam("hunters");
            hunters.setDisplayName(ChatColor.RED + "Hunters");
        } else {
            hunters = scoreboard.getTeam("hunters");
        }

        Objective objective = scoreboard.getObjective("dummy");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("dummy", "dummy", ChatColor.YELLOW + "Lives");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        initializeScoreboard();
    }

    public int getLives(Player player) {
        return playerLives.getOrDefault(player.getUniqueId(), 0);
    }


    public void resetPlayers() {
        if (hunters != null) {
            for (String playerName : hunters.getEntries()) {
                hunters.removeEntry(playerName);
            }
        }

        if (speedrunners != null) {
            for (String playerName : speedrunners.getEntries()) {
                speedrunners.removeEntry(playerName);
            }
        }
        playerLives.clear();
        updateScoreboard();
    }


    public int getPlayerLives(Player player) {
        return playerLives.getOrDefault(player.getName(), 0);
    }

    public int getTotalSpeedrunnerLives() {
        return playerLives.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void initializeScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    public void updateScoreboard() {
        Objective objective = scoreboard.getObjective("dummy");

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        for (Map.Entry<String, Integer> entry : playerLives.entrySet()) {
            String playerName = entry.getKey();
            int lives = entry.getValue();
            objective.getScore(ChatColor.AQUA + playerName + ": ").setScore(lives);
        }
    }

    public void setHunter(Player player) {
        speedrunners.removeEntry(player.getName());
        hunters.addEntry(player.getName());
        playerLives.remove(player.getName());
        updateScoreboard();
    }

    public void setSpeedrunner(Player player) {
        hunters.removeEntry(player.getName());
        speedrunners.addEntry(player.getName());
        playerLives.put(player.getName(), 1);
        updateScoreboard();
    }

    public boolean isSpeedrunner(Player player) {
        return speedrunners.hasEntry(player.getName());
    }

    public boolean isHunter(Player player) {
        return hunters.hasEntry(player.getName());
    }

    public void addLife(Player player) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) + 1;
            playerLives.put(player.getName(), lives);
            player.sendMessage(ChatColor.GREEN + config.getString(language + ".messages.live_add") + " " + lives);
            updateScoreboard();
        }
    }

    public void removeLife(Player player) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) - 1;
            if (lives <= 0) {
                player.sendMessage(ChatColor.RED + config.getString(language + ".messages.live_null"));
                playerLives.remove(player.getName());
            } else {
                playerLives.put(player.getName(), lives);
                player.sendMessage(ChatColor.RED + config.getString(language + ".messages.live_remove") + " " + lives);
            }
            updateScoreboard();
        }
    }

    public List<Player> getHunters() {
        List<Player> players = new ArrayList<>();
        for (String playerName : hunters.getEntries()) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public List<Player> getSpeedrunners() {
        List<Player> players = new ArrayList<>();
        for (String playerName : speedrunners.getEntries()) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}
