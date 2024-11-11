package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EnderDragonDeathListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public EnderDragonDeathListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            // Speedrunner victory message
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "title @a title {\"text\":\"Speedrunners Win\", \"color\":\"#00FF00\"}");

            // Set all players to spectator mode and clear inventories
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
            }

            // End the game
            plugin.getGameManager().endGame(); // Method to end the game
        }
    }
}
