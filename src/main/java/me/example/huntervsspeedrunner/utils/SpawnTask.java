package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
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
        HunterVSSpeedrunnerPlugin plugin = (HunterVSSpeedrunnerPlugin) Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner");
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        if (speedrunner != null && speedrunner.isOnline()) {
            Location location = speedrunner.getLocation();
            World world = speedrunner.getWorld();
            world.setSpawnLocation(location);
        } else {
            Bukkit.getLogger().warning(config.getString(language + "messages.online"));
        }
    }
}
