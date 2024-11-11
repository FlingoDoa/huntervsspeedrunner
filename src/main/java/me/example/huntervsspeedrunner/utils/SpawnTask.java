package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.World;

public class SpawnTask extends BukkitRunnable {
    private final HunterVSSpeedrunnerPlugin plugin;
    private final Player speedrunner;

    public SpawnTask(HunterVSSpeedrunnerPlugin plugin, Player speedrunner) {
        this.plugin = plugin;
        this.speedrunner = speedrunner;
    }

    @Override
    public void run() {
        if (speedrunner != null && speedrunner.isOnline()) {
            Location location = speedrunner.getLocation();
            // We get the world where the speedrunner is located
            World world = speedrunner.getWorld();
            // Setspawn this world
            world.setSpawnLocation(location);  // Set spawn to the speedrunner's location in his world
        } else {
            Bukkit.getLogger().warning("Speedrunner isn't online or doesn't exist!");
        }
    }
}
