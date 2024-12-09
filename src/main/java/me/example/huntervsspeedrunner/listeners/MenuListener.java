package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.RandomTaskManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
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

        // Проверка, что клик был именно в правильном инвентаре
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !inventory.equals(event.getView().getTopInventory())) {
            return; // Игнорируем клики по другим инвентарям
        }

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return; // Игнорируем клики по пустым слотам
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return; // Игнорируем клики по слотам без метаданных
        }

        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String path = language + ".messages.";

        // Проверка нажатой кнопки
        String displayName = meta.getDisplayName();

        if (displayName.equals(config.getString(language + ".menu.speedrunner.name"))) {
            plugin.getLifeManager().setSpeedrunner(player);
            player.sendMessage(config.getString(path + "selected_speedrunner"));
        } else if (displayName.equals(config.getString(language + ".menu.hunter.name"))) {
            plugin.getLifeManager().setHunter(player);
            player.sendMessage(config.getString(path + "selected_hunter"));
        } else if (displayName.equals(config.getString(language + ".menu.add_life.name"))) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                plugin.getLifeManager().addLife(player);
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "only_speedrunner_can_add_life"));
            }
        } else if (displayName.equals(config.getString(language + ".menu.remove_life.name"))) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                plugin.getLifeManager().removeLife(player);
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "only_speedrunner_can_remove_life"));
            }
        } else if (displayName.equals(config.getString(language + ".menu.start.name"))) {
            if (player.isOp()) {
                if (GameManager.canStartGame(plugin)) {
                    Bukkit.getServer().getLogger().info("Attempting to start the game!");
                    GameManager.startGame(plugin);
                } else {
                    player.sendMessage(ChatColor.RED + config.getString(path + "not_enough_players"));
                }
            } else {
                player.sendMessage(ChatColor.RED + config.getString(path + "start_game_op_only"));
            }
        }

        // Проверка по слотам
        switch (event.getSlot()) {
            case 10: // Включение/выключение рандомного режима
                RandomTaskManager randomTaskManager = plugin.getRandomTaskManager();
                if (randomTaskManager.isRandomModeEnabled()) {
                    randomTaskManager.disableRandomMode();
                    player.sendMessage(ChatColor.RED + config.getString(path + "random_mode_disabled"));
                } else {
                    randomTaskManager.enableRandomMode();
                    player.sendMessage(ChatColor.GREEN + config.getString(path + "random_mode_enabled"));
                }
                break;

            case 12: // Пересоздать миры
                plugin.executeWorldCommands(player);
                player.sendMessage(ChatColor.GREEN + config.getString(path + "world_restarting"));
                break;

            case 14: // Включение/выключение компаса
                player.sendMessage(ChatColor.RED + "This feature is temporarily unavailable."); // Заготовка
                break;

            case 16: // Перезагрузка плагина
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + config.getString(path + "plugin_reloaded"));
                plugin.reloadPlugin();
                break;
        }

        // Отмена действия клика, чтобы не взаимодействовать с инвентарем
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
