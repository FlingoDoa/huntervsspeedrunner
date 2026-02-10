package me.example.huntervsspeedrunner.utils;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class Feedback {
    private final JavaPlugin plugin;
    private final double currentVersion = 1.81;

    public Feedback(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void forceUpdateIfNeeded() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        boolean shouldUpdate = !plugin.getConfig().isSet("version")
                || plugin.getConfig().getDouble("version") < currentVersion;

        if (!shouldUpdate) return;

        plugin.getLogger().info("Обновление конфига до v" + currentVersion + "...");

        if (configFile.exists() && !configFile.delete()) {
            plugin.getLogger().warning("Не удалось удалить старый config.yml!");
            return;
        }

        plugin.saveResource("config.yml", true);
        plugin.reloadConfig();
        plugin.getConfig().set("version", currentVersion);
        plugin.saveConfig();

    }
}