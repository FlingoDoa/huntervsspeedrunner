package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassTask extends BukkitRunnable {

    private final Player hunter;
    private Player target;

    // Constructor for logging
    public CompassTask(Player hunter, Player target) {
        this.hunter = hunter;
        this.target = target;
        Bukkit.getLogger().info("Tracking task created for speedrunner: " + (target != null ? target.getName() : "unknown"));
    }

    @Override
    public void run() {
        // Log that the task has started
        Bukkit.getLogger().info("Starting speedrunner tracking task.");

        // Check if the target exists and is online
        if (target != null && target.isOnline()) {
            // Get the speedrunner's world
            World world = target.getWorld();

            // Log current coordinates of the speedrunner
            Bukkit.getLogger().info("Updating spawn to speedrunner's coordinates: " + target.getLocation().toString());

            // Set the global world spawn to the speedrunner's position
            world.setSpawnLocation(target.getLocation());

            // Log the global spawn update
            Bukkit.getLogger().info("Global spawn for world " + world.getName() + " updated to coordinates: " + target.getLocation().toString());

            // Set personal spawn for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setBedSpawnLocation(target.getLocation(), true);

                // Log personal spawn update for each player
                Bukkit.getLogger().info("Personal spawn for player " + player.getName() + " updated to speedrunner's coordinates: " + target.getLocation().toString());
            }

            // Additional log to indicate task completion
            Bukkit.getLogger().info("Spawn update task for all players completed.");
        } else {
            // If the target is not online, log a warning
            Bukkit.getLogger().warning("Speedrunner " + (target != null ? target.getName() : "unknown") + " is not online. Spawn not updated.");
        }
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    // Static method to start the task
    public static void startTracking(Player hunter, Player target) {
        Bukkit.getLogger().info("Starting tracking task for speedrunner...");
        new CompassTask(hunter, target).runTaskTimer(Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner"), 0L, 100L); // 100 ticks = 5 seconds
    }
}
