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
            // Сообщение о победе спидраннеров
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "title @a title {\"text\":\"Спидраннеры выиграли\", \"color\":\"#00FF00\"}");

            // Перевод всех игроков в режим наблюдателя и очистка инвентаря
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
            }

            // Завершение игры
            plugin.getGameManager().endGame(); // Метод завершения игры
        }
    }
}
