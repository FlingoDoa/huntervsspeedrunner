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
            // Получаем мир, где находится спидраннер
            World world = speedrunner.getWorld();
            // Устанавливаем спавн в этом мире
            world.setSpawnLocation(location);  // Устанавливаем спавн на местоположение спидраннера в его мире
        } else {
            Bukkit.getLogger().warning("Спидраннер не онлайн или не существует!");
        }
    }
}
