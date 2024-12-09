package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
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
        // Если убитый моб не дракон, выходим
        if (event.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }

        // Проверяем, включен ли рандом мод
        if (plugin.getRandomTaskManager().isRandomModeEnabled()) {
            return; // Если включен, не обрабатываем событие
        }

        // Получаем настройки из конфигурации
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String victoryMessage = config.getString(language + ".messages.speedrunners_win", "Speedrunners Win");

        // Сообщение о победе спидраннеров
        String titleCommand = String.format("title @a title {\"text\":\"%s\", \"color\":\"#00FF00\"}", victoryMessage);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);

        // Перевод всех игроков в режим наблюдателя и очистка инвентаря
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
        }

        // Завершение игры
        plugin.getGameManager().endGame();
    }
}
