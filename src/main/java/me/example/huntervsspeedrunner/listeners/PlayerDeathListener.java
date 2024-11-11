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

        if (lifeManager.isHunter(player) && config.getBoolean("hunter.giveCompassOnDeath", true)) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    player.getInventory().addItem(compass);
                    player.sendMessage(ChatColor.GREEN + "You received a compass after 10 seconds.");
                }
            }.runTaskLater(plugin, 20L * 10); // 10 секунд (20 тиков * 10)
        }


        if (lifeManager.isSpeedrunner(player)) {
            lifeManager.removeLife(player);


            if (lifeManager.getPlayerLives(player) <= 0) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "You are out of lives! You have been set to spectator mode.");
            }


            if (lifeManager.getTotalSpeedrunnerLives() <= 0) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "title @a title {\"text\":\"Hunters Win\", \"color\":\"#C90000\"}");


                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.getInventory().clear();
                }
                plugin.getGameManager().endGame();
            }
        }
    }
}
