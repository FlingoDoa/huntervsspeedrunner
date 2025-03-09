package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.random.RandomTaskManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class MenuListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public MenuListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isMenuOpen()) {
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !inventory.equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language");
        String path = language + ".messages.";
        int slot = event.getSlot();

        event.setCancelled(true);

        switch (slot) {
            case 0:
                if (plugin.getLifeManager().isSpeedrunner(player)) {
                    plugin.getLifeManager().addLife(player);
                    String lifeAddedMessage = config.getString(path + "life_added");
                    if (lifeAddedMessage != null) {
                        player.sendMessage(ChatColor.GREEN + lifeAddedMessage);
                    }
                } else {
                    String onlySpeedrunnerMessage = config.getString(path + "only_speedrunner_can_add_life");
                    if (onlySpeedrunnerMessage != null) {
                        player.sendMessage(ChatColor.RED + onlySpeedrunnerMessage);
                    }
                }
                break;

            case 2:
                plugin.getLifeManager().setSpeedrunner(player);
                player.sendMessage(config.getString(path + "selected_speedrunner"));
                updateAllTeamItems(inventory, config, language);
                break;

            case 4:
                if (player.isOp()) {
                    if (GameManager.canStartGame(plugin)) {
                        Bukkit.getServer().getLogger().info("Attempting to start the game!");
                        player.sendMessage(ChatColor.GREEN + "Игра запускается!");
                        GameManager.startGame(plugin);
                    } else {
                        player.sendMessage(ChatColor.RED + config.getString(path + "not_enough_players"));
                    }
                } else {
                    player.sendMessage(ChatColor.RED + config.getString(path + "start_game_op_only"));
                }
                break;

            case 6:
                plugin.getLifeManager().setHunter(player);
                player.sendMessage(config.getString(path + "selected_hunter"));
                updateAllTeamItems(inventory, config, language);
                break;

            case 8:
                if (plugin.getLifeManager().isSpeedrunner(player)) {
                    plugin.getLifeManager().removeLife(player);
                    String lifeRemovedMessage = config.getString(path + "life_removed");
                    if (lifeRemovedMessage != null) {
                        player.sendMessage(ChatColor.GREEN + lifeRemovedMessage);
                    }
                } else {
                    String onlySpeedrunnerMessage = config.getString(path + "only_speedrunner_can_remove_life");
                    if (onlySpeedrunnerMessage != null) {
                        player.sendMessage(ChatColor.RED + onlySpeedrunnerMessage);
                    }
                }
                break;

            case 10:
                RandomTaskManager randomTaskManager = plugin.getRandomTaskManager();
                boolean randomEnabled = !randomTaskManager.isRandomModeEnabled();
                if (randomEnabled) {
                    randomTaskManager.enableRandomMode();
                    player.sendMessage(ChatColor.GREEN + config.getString(path + "random_mode_enabled"));
                } else {
                    randomTaskManager.disableRandomMode();
                    player.sendMessage(ChatColor.RED + config.getString(path + "random_mode_disabled"));
                }

                updateMenuItem(inventory, 10, config.getString(language + ".menu.toggle_random.name"),
                        randomEnabled ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌");
                break;

            case 12:
                plugin.executeWorldCommands(player);
                player.sendMessage(ChatColor.GREEN + config.getString(path + "world_restarting"));
                break;
            case 14:
                boolean compassEnabled = plugin.getCompassManager().toggleCompass(player);
                if (compassEnabled) {
                    player.sendMessage(ChatColor.GREEN + config.getString(path + "compass_enabled"));
                } else {
                    player.sendMessage(ChatColor.RED + config.getString(path + "compass_disabled"));
                }
                updateMenuItem(inventory, 14, config.getString(language + ".menu.toggle_compass.name"),
                        compassEnabled ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌");
                break;
            case 16:
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + config.getString(path + "plugin_reloaded"));
                plugin.reloadPlugin();
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестный слот: " + slot);
                break;
        }
    }

        @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (plugin.isMenuOpen()) {
            plugin.setMenuOpen(false);
            player.sendMessage(plugin.getConfig().getString(plugin.getConfig().getString("language") + ".messages.menu_closed"));
        }
    }

    private void updateAllTeamItems(Inventory inventory, FileConfiguration config, String language) {
        List<Player> speedrunners = plugin.getLifeManager().getSpeedrunners();
        List<Player> hunters = plugin.getLifeManager().getHunters();

        updateTeamMenuItem(inventory, 2, config.getString(language + ".menu.speedrunner.name"), speedrunners);
        updateTeamMenuItem(inventory, 6, config.getString(language + ".menu.hunter.name"), hunters);
    }

    private void updateTeamMenuItem(Inventory inventory, int slot, String name, List<Player> players) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " (" + players.size() + ")");

            List<String> lore = new ArrayList<>();
            if (players.isEmpty()) {
                lore.add(ChatColor.RED + "Пока никто не выбрал этот класс!");
            } else {
                for (Player p : players) {
                    lore.add(ChatColor.GRAY + "- " + p.getName());
                }
            }
            meta.setLore(lore);

            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_DYE,
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_POTION_EFFECTS,
                    ItemFlag.HIDE_PLACED_ON
            );

            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }


    private void updateMenuItem(Inventory inventory, int slot, String name, String extraInfo) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " " + extraInfo);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }
}
