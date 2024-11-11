package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent; // Add this import
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public MenuListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the menu is open
        if (!plugin.isMenuOpen()) {
            return;  // If the menu is not open, do nothing
        }

        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        Player player = (Player) event.getWhoClicked();
        String displayName = meta.getDisplayName();

        LifeManager lifeManager = plugin.getLifeManager();

        if (displayName.equals("§9Speedrunner")) {
            lifeManager.setSpeedrunner(player);
            player.sendMessage("You are now a speedrunner!");
        } else if (displayName.equals("§4Hunter")) {
            lifeManager.setHunter(player);
            player.sendMessage("You are now a hunter!");
        } else if (displayName.equals("§aAdd Life")) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.addLife(player); // Add life to the speedrunner
            } else {
                player.sendMessage(ChatColor.RED + "Only speedrunners can add lives!");
            }
        } else if (displayName.equals("§cRemove Life")) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.removeLife(player); // Remove life from the speedrunner
            } else {
                player.sendMessage(ChatColor.RED + "Only speedrunners can remove lives!");
            }
        } else if (displayName.equals("§eStart Game")) {
            if (player.isOp()) {
                if (GameManager.canStartGame(plugin)) {
                    Bukkit.getServer().getLogger().info("Attempting to start the game!");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hunter start");
                } else {
                    player.sendMessage(ChatColor.RED + "Not enough players to start the game!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "AVAILABLE TO OPERATORS ONLY!");
            }
        }

        event.setCancelled(true);  // Prevent further actions
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Check if the closed inventory is related to our menu
        Player player = (Player) event.getPlayer();
        if (plugin.isMenuOpen()) {
            // If the menu was open and the player closed it, reset the flag
            plugin.setMenuOpen(false);
            player.sendMessage("Menu closed!");
        }
    }
}
