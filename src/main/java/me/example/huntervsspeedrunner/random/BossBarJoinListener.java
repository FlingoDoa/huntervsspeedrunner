package me.example.huntervsspeedrunner.random;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BossBarJoinListener implements Listener {
    private final RandomTaskManager manager;

    public BossBarJoinListener(RandomTaskManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            manager.reassignBossBar(event.getPlayer());
            if (manager.isRandomModeEnabled() && manager.getPlugin() instanceof me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin) {
                me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin plugin = (me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin) manager.getPlugin();
                if (plugin.getLifeManager().isSpeedrunner(event.getPlayer())) {
                    manager.showTaskToAllSpeedrunners();
                    manager.startTaskCheckingForAllSpeedrunners();
                }
            }
        }, 20L);
    }
}
