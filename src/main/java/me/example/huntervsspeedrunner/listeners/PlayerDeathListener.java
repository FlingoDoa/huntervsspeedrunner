package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.LifeManager;
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

        if (lifeManager.isHunter(player)) {
            // Задержка на выдачу компаса в 10 секунд после смерти хантера
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    player.getInventory().addItem(compass);
                    player.sendMessage(ChatColor.GREEN + "Вам выдан компас после 10 секунд.");
                }
            }.runTaskLater(plugin, 20L * 10); // 10 секунд (20 тиков * 10)
        }

        if (lifeManager.isHunter(player)) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            player.getInventory().addItem(compass);
        }

        // Если погиб спидраннер
        if (lifeManager.isSpeedrunner(player)) {
            lifeManager.removeLife(player);

            // Проверка оставшихся жизней спидраннера
            if (lifeManager.getPlayerLives(player) <= 0) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "У вас закончились жизни! Вы переведены в режим наблюдателя.");
            }

            // Проверка суммарных жизней всех спидраннеров
            if (lifeManager.getTotalSpeedrunnerLives() <= 0) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "title @a title {\"text\":\"Хантеры выиграли\", \"color\":\"#C90000\"}");

                // Очистка инвентарей и перевод в наблюдатели
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.getInventory().clear();
                }
                plugin.getGameManager().endGame(); // Завершение игры
            }
        }
    }
}
