package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
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
        if (event.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }
        if (plugin.getRandomTaskManager().isRandomModeEnabled()) {
            return;
        }
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String victoryMessage = config.getString(language + ".messages.speedrunners_win", "Speedrunners Win");
        String titleCommand = String.format("title @a title {\"text\":\"%s\", \"color\":\"#00FF00\"}", victoryMessage);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
        }
        GameManager.endGame(plugin, "Спидраннеры", "Эндер дракон убит");
    }
}
