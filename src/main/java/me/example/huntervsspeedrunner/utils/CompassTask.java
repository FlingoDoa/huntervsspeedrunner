package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassTask extends BukkitRunnable {

    private final Player hunter;
    private Player target;
    private final HunterVSSpeedrunnerPlugin plugin;

    public CompassTask(HunterVSSpeedrunnerPlugin plugin, Player hunter, Player target) {
        this.plugin = plugin;
        this.hunter = hunter;
        this.target = target;

        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String message = config.getString(language + ".messages.tracking_task_created", "Tracking task created for speedrunner: %s");
    }

    @Override
    public void run() {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");

        if (target != null && target.isOnline()) {
            World world = target.getWorld();

            String updatingSpawnMsg = config.getString(language + ".messages.updating_spawn", "Updating spawn to speedrunner's coordinates: %s");

            world.setSpawnLocation(target.getLocation());

            String globalSpawnUpdatedMsg = config.getString(language + ".messages.global_spawn_updated", "Global spawn for world %s updated to coordinates: %s");

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setBedSpawnLocation(target.getLocation(), true);

                String personalSpawnUpdatedMsg = config.getString(language + ".messages.personal_spawn_updated", "Personal spawn for player %s updated to speedrunner's coordinates: %s");
            }
        } else {
            String offlineMsg = config.getString(language + ".messages.speedrunner_offline", "Speedrunner %s is not online. Spawn not updated.");
        }
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public static void startTracking(HunterVSSpeedrunnerPlugin plugin, Player hunter, Player target) {
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String startingTrackingMsg = config.getString(language + ".messages.tracking_task_started", "Starting tracking task for speedrunner...");

        new CompassTask(plugin, hunter, target).runTaskTimer(plugin, 0L, 100L);
    }
}