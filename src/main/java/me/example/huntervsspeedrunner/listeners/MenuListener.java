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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public MenuListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            Inventory inventory = event.getClickedInventory();
            Player player = (Player) event.getWhoClicked();

            if (inventory == null) return;

            String title = event.getView().getTitle();
            if (title.equals(plugin.getSetConfig().getMenuTitle())) {
                event.setCancelled(true);
                plugin.getSetConfig().handleClick(event);
                return;
            }
            
            if (title.equals(ChatColor.DARK_PURPLE + getLocalizedMessage("random_tasks_settings"))) {
                event.setCancelled(true);
                plugin.getSetConfig().handleDetailedDifficultyClick(event);
                return;
            }
            String strippedTitle2 = ChatColor.stripColor(title);
            String categoryTitle = getLocalizedMessage("tasks_category_title");
            if (strippedTitle2.startsWith("Задания •") || strippedTitle2.startsWith(categoryTitle + " •") || strippedTitle2.startsWith("Tasks •")) {
                event.setCancelled(true);
                plugin.getSetConfig().handleCategoryMenuClick(event);
                return;
            }
            String strippedTitle = ChatColor.stripColor(title);
            if (strippedTitle.startsWith("Задачи ") || strippedTitle.startsWith("Tasks ")) {
                event.setCancelled(true);
                plugin.getSetConfig().handleTaskListClick(event);
                return;
            }
            if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.ANVIL
                    && (ChatColor.stripColor(title).startsWith("Добавить ") || ChatColor.stripColor(title).startsWith("Add "))) {
                event.setCancelled(true);
                plugin.getSetConfig().handleAnvilClick(event);
                return;
            }
            String strippedTitle3 = ChatColor.stripColor(title);
            String presetsTitle = getLocalizedMessage("scenario_presets_title");
            if (strippedTitle3.equals("Пресеты сценариев") || strippedTitle3.equals(presetsTitle)) {
                event.setCancelled(true);
                plugin.getSetConfig().handleScenarioPresetsClick(event);
                return;
            }

            String strippedMain = ChatColor.stripColor(title);
            if (!"Select a Team".equals(strippedMain)) {
                return;
            }

        if (!inventory.equals(event.getView().getTopInventory())) {
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

        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language", "en");
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
                if (player.isOp()) {
                    player.closeInventory();
                    plugin.getSetConfig().openConfigMenu(player);
                }
                else {
                    player.sendMessage(ChatColor.RED + config.getString(path + "start_game_op_only"));
                }
                break;
            default:
                break;
        }
        } catch (Throwable e) {
            plugin.getLogger().severe("Ошибка в MenuListener.onInventoryClick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = ChatColor.stripColor(event.getView().getTitle());
        if ("Select a Team".equals(title)) {
            String lang = plugin.getConfig().getString("language", "en");
            String msg = plugin.getConfig().getString(lang + ".messages.menu_closed");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }

    private void updateAllTeamItems(Inventory inventory, FileConfiguration config, String language) {
        List<Player> speedrunners = plugin.getLifeManager().getSpeedrunners();
        List<Player> hunters = plugin.getLifeManager().getHunters();

        updateTeamMenuItem(inventory, 2, config.getString(language + ".menu.speedrunner.name"), speedrunners, config);
        updateTeamMenuItem(inventory, 6, config.getString(language + ".menu.hunter.name"), hunters, config);
    }

    private void updateTeamMenuItem(Inventory inventory, int slot, String name, List<Player> players, FileConfiguration config) {
        String language = config.getString("language", "en");
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " (" + players.size() + ")");

            List<String> lore = new ArrayList<>();
            if (players.isEmpty()) {
                lore.add(ChatColor.RED + config.getString(language + ".messages.not_take"));
            } else {
                for (Player p : players) {
                    lore.add(ChatColor.GRAY + "- " + p.getName());
                }
            }
            meta.setLore(lore);

            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS
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

    private String getLocalizedMessage(String key, Object... args) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");
        String format = config.getString(lang + ".messages." + key, "§c[Missing translation: " + key + "]");
        return String.format(format, args);
    }
}
