package me.example.huntervsspeedrunner.menus;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SetConfig {

    private final HunterVSSpeedrunnerPlugin plugin;
    private String currentSetting = null;

    public SetConfig(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    public void openConfigMenu(Player player) {
        Inventory configMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + getLocalizedMessage("advanced_settings_title"));
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");

        String langDisplay = ChatColor.AQUA + getLocalizedMessage("language") + ": " + ChatColor.YELLOW + lang;
        configMenu.setItem(9, createItem(Material.WRITABLE_BOOK, langDisplay));

        boolean giveCompass = config.getBoolean("hunter.giveCompassOnDeath", true);
        String compassToggle = ChatColor.YELLOW + getLocalizedMessage("give_compass_on_death") + ": " + (giveCompass ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌");
        configMenu.setItem(11, createItem(Material.RESPAWN_ANCHOR, compassToggle));

        int delay = config.getInt("hunter.teleportDelay", 30);
        String delayDisplay = ChatColor.LIGHT_PURPLE + getLocalizedMessage("teleport_delay") + ": " + ChatColor.YELLOW + delay + " " + getLocalizedMessage("seconds");
        configMenu.setItem(12, createItem(Material.ENDER_PEARL, delayDisplay));

        int compassTime = config.getInt("hunter.compassgive", 120);
        String compassDisplay = ChatColor.GOLD + getLocalizedMessage("compass_time") + ": " + ChatColor.YELLOW + compassTime + " " + getLocalizedMessage("seconds");
        configMenu.setItem(14, createItem(Material.CLOCK, compassDisplay));


        configMenu.setItem(15, createItem(Material.TNT, ChatColor.GOLD + "🚨 " + getLocalizedMessage("start_game_now")));
        configMenu.setItem(17, createItem(Material.ANVIL, ChatColor.RED + getLocalizedMessage("reload_plugin")));
        configMenu.setItem(22, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));
        if (currentSetting != null) {
            configMenu.setItem(2, createItem(Material.GREEN_CONCRETE, ChatColor.GREEN + "+10 " + getLocalizedMessage("seconds")));
            configMenu.setItem(3, createItem(Material.GREEN_WOOL, ChatColor.GREEN + "+1 " + getLocalizedMessage("second")));
            configMenu.setItem(4, createItem(Material.LIME_DYE, ChatColor.GREEN + getLocalizedMessage("done")));
            configMenu.setItem(5, createItem(Material.RED_WOOL, ChatColor.RED + "-1 " + getLocalizedMessage("second")));
            configMenu.setItem(6, createItem(Material.RED_CONCRETE, ChatColor.RED + "-10 " + getLocalizedMessage("seconds")));
        }
        player.openInventory(configMenu);
    }

    private ItemStack createItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }
    public String getMenuTitle() {
        return ChatColor.DARK_GREEN + getLocalizedMessage("advanced_settings_title");
    }
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language", "en");
        String path = language + ".messages.";

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 27) {
            return;
        }

        event.setCancelled(true);
        boolean shouldReopenMenu = true;

        switch (slot) {
            case 9: {
                String current = config.getString("language", "en");
                String next = current.equals("en") ? "ru" : "en";
                config.set("language", next);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + getLocalizedMessage("change_language") + next);
                break;
            }
            case 11:
                toggleBooleanConfig(player, "hunter.giveCompassOnDeath", "give_compass");
                break;
            case 12:
                currentSetting = "teleportDelay";
                break;
            case 14:
                currentSetting = "compassgive";
                break;
            case 15:
                player.sendMessage(ChatColor.GOLD + getLocalizedMessage("run_game_fast"));
                GameManager.startGame(plugin);
                shouldReopenMenu = false;
                break;
            case 17:
                player.closeInventory();
                plugin.reloadPlugin();
                player.sendMessage(ChatColor.GREEN + getLocalizedMessage("plugin_reloaded"));
                shouldReopenMenu = false;
                break;
            case 2:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, 10);
                }
                break;
            case 3:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, 1);
                }
                break;
            case 5:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, -1);
                }
                break;
            case 6:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, -10);
                }
                break;
            case 4:
                currentSetting = null;
                break;
            case 22:
                player.closeInventory();
                shouldReopenMenu = false;
                player.chat("/hunter");
                break;
            default:
                break;
        }

        if (shouldReopenMenu) {
            openConfigMenu(player);
        }
    }

    private void adjustSetting(Player player, FileConfiguration config, String setting, int change) {
        String configKey = "hunter." + setting;
        int current = config.getInt(configKey, setting.equals("teleportDelay") ? 30 : 120);
        int next = current + change;

        if (next < 0) next = 0;
        if (setting.equals("teleportDelay") && next > 999) next = 999;
        if (setting.equals("compassgive") && next > 999) next = 999;

        config.set(configKey, next);
        plugin.saveConfig();

        String messageKey = setting.equals("teleportDelay") ? "teleport_delay_changed" : "compass_time_changed";
        player.sendMessage(ChatColor.GREEN + getLocalizedMessage(messageKey, next));
    }

    private void toggleBooleanConfig(Player player, String key, String nameKey) {
        boolean value = plugin.getConfig().getBoolean(key, true);
        boolean newValue = !value;
        plugin.getConfig().set(key, newValue);
        plugin.saveConfig();
        String messageKey = newValue ? nameKey + "_toggled_on" : nameKey + "_toggled_off";
        player.sendMessage(ChatColor.GREEN + getLocalizedMessage(messageKey));
    }

    private String getLocalizedMessage(String key, Object... args) {
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");
        String format = config.getString(lang + ".messages." + key, "§c[Missing translation: " + key + "]");
        return String.format(format, args);
    }
}
