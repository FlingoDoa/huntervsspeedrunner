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

    // Store lives for each speedrunner
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

    public int getPlayerLives(Player player) {
        return playerLives.getOrDefault(player.getName(), 0);
    }

    // Method to calculate total lives of all speedrunners
    public int getTotalSpeedrunnerLives() {
        return playerLives.values().stream().mapToInt(Integer::intValue).sum();
    }

    // Initialize scoreboard for all players
    public void initializeScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    // Update display of lives on the scoreboard
    public void updateScoreboard() {
        Objective objective = scoreboard.getObjective("dummy");

        // Reset current entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Update lives for each speedrunner
        for (Map.Entry<String, Integer> entry : playerLives.entrySet()) {
            String playerName = entry.getKey();
            int lives = entry.getValue();
            objective.getScore(ChatColor.AQUA + playerName + ": ").setScore(lives);
        }
    }

    // Add player to Hunters team
    public void setHunter(Player player) {
        speedrunners.removeEntry(player.getName());
        hunters.addEntry(player.getName());
        playerLives.remove(player.getName());
        updateScoreboard();
    }

    // Add player to Speedrunners team with default lives (1)
    public void setSpeedrunner(Player player) {
        hunters.removeEntry(player.getName());
        speedrunners.addEntry(player.getName());
        playerLives.put(player.getName(), 1);
        updateScoreboard();
    }

    // Check if player is a Speedrunner
    public boolean isSpeedrunner(Player player) {
        return speedrunners.hasEntry(player.getName());
    }

    // Check if player is a Hunter
    public boolean isHunter(Player player) {
        return hunters.hasEntry(player.getName());
    }

    // Add a life to a speedrunner
    public void addLife(Player player) {
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) + 1;
            playerLives.put(player.getName(), lives);
            player.sendMessage(ChatColor.GREEN + config.getString(language + ".messages.live_add") +" "+ lives);
            updateScoreboard();
        }
    }

    // Remove a life from a speedrunner
    public void removeLife(Player player) {
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (isSpeedrunner(player)) {
            int lives = playerLives.getOrDefault(player.getName(), 1) - 1;
            if (lives <= 0) {
                player.sendMessage(ChatColor.RED + config.getString(language + ".messages.live_null"));
                playerLives.remove(player.getName());
            } else {
                playerLives.put(player.getName(), lives);
                player.sendMessage(ChatColor.RED + config.getString(language + ".messages.live_remove") +" "+  lives);
            }
            updateScoreboard();
        }
    }

    // Get list of Hunters
    public List<String> getHunters() {
        return new ArrayList<>(hunters.getEntries());
    }

    // Get list of Speedrunners
    public List<String> getSpeedrunners() {
        return new ArrayList<>(speedrunners.getEntries());
    }
}
