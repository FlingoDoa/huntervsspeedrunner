package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public MenuListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Проверка, открыт ли меню
        if (!plugin.isMenuOpen()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        Player player = (Player) event.getWhoClicked();
        String displayName = meta.getDisplayName();

        LifeManager lifeManager = plugin.getLifeManager();
        FileConfiguration config = plugin.getConfig();

        // Извлечение языка и подготовка пути к сообщениям
        String language = config.getString("language");
        String path = language + ".messages.";

        // Проверка и обработка выбора игрока
        if (displayName.equals(config.getString(language + ".menu.speedrunner.name"))) {
            lifeManager.setSpeedrunner(player);
            player.sendMessage(config.getString(path + "selected_speedrunner"));
        } else if (displayName.equals(config.getString(language + ".menu.hunter.name"))) {
            lifeManager.setHunter(player);
            player.sendMessage(config.getString(path + "selected_hunter"));
        } else if (displayName.equals(config.getString(language + ".menu.add_life.name"))) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.addLife(player);
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "only_speedrunner_can_add_life"));
            }
        } else if (displayName.equals(config.getString(language + ".menu.remove_life.name"))) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.removeLife(player);
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "only_speedrunner_can_remove_life"));
            }
        } else if (displayName.equals(config.getString(language + ".menu.start.name"))) {
            if (player.isOp()) {
                if (GameManager.canStartGame(plugin)) {
                    Bukkit.getServer().getLogger().info("Attempting to start the game!");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hunter start");
                } else {
                    player.sendMessage(ChatColor.RED + config.getString(path + "not_enough_players"));
                }
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "start_game_op_only"));
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (plugin.isMenuOpen()) {
            plugin.setMenuOpen(false);
            player.sendMessage(plugin.getConfig().getString(plugin.getConfig().getString("language") + ".messages.menu_closed"));
        }
    }
}
