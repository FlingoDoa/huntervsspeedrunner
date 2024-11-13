package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.LifeManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerDeathListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;
    private final LifeManager lifeManager;

    public PlayerDeathListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        FileConfiguration config = plugin.getConfig(); // Получаем конфигурацию
        String language = config.getString("language");
        String path = language + ".messages.";

        // Если игрок - охотник и включено получение компаса после смерти
        if (lifeManager.isHunter(player) && config.getBoolean("hunter.giveCompassOnDeath", true)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    player.getInventory().addItem(compass);
                    player.sendMessage(ChatColor.GREEN + config.getString(path + "compass_received"));
                }
            }.runTaskLater(plugin, 20L * 20); // Задержка в 10 секунд (20 тиков * 10)
        }

        // Если игрок - спидраннер, уменьшаем количество жизней
        if (lifeManager.isSpeedrunner(player)) {
            lifeManager.removeLife(player);

            // Если жизней больше нет, игрок становится наблюдателем
            if (lifeManager.getPlayerLives(player) <= 0) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + config.getString(path + "out_of_lives"));
            }

            // Если у всех спидраннеров закончились жизни, игра заканчивается
            if (lifeManager.getTotalSpeedrunnerLives() <= 0) {
                String titleCommand = String.format("title @a title {\"text\":\"%s\", \"color\":\"#C90000\"}", config.getString(path + "hunters_win"));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.getInventory().clear();
                }
                plugin.getGameManager().endGame();
            }
        }
    }
}
