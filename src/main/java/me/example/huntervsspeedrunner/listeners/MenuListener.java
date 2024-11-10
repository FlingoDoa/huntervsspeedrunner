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
import org.bukkit.event.inventory.InventoryCloseEvent; // Добавьте этот импорт
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public MenuListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Проверяем, открыто ли меню
        if (!plugin.isMenuOpen()) {
            return;  // Если меню не открыто, ничего не делаем
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
            player.sendMessage("Вы стали спидраннером!");
        } else if (displayName.equals("§4Hunter")) {
            lifeManager.setHunter(player);
            player.sendMessage("Вы стали охотником!");
        } else if (displayName.equals("§aДобавить жизнь")) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.addLife(player); // Добавляем жизнь спидраннеру
            } else {
                player.sendMessage(ChatColor.RED + "Только спидраннеры могут добавлять себе жизни!");
            }
        } else if (displayName.equals("§cУбрать жизнь")) {
            if (lifeManager.isSpeedrunner(player)) {
                lifeManager.removeLife(player); // Уменьшаем жизнь спидраннеру
            } else {
                player.sendMessage(ChatColor.RED + "Только спидраннеры могут уменьшать свои жизни!");
            }
        } else if (displayName.equals("§eЗапустить игру")) {
            if (player.isOp()) {
                if (GameManager.canStartGame(plugin)) {
                    Bukkit.getServer().getLogger().info("Attempting to start the game!");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "hunter start");
                } else {
                    player.sendMessage(ChatColor.RED + "Не хватает игроков для старта игры!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "ДОСТУПНО ТОЛЬКО ОПЕРАТОРУ!");
            }
        }

        event.setCancelled(true);  // Останавливаем дальнейшие действия
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Проверяем, был ли закрыт инвентарь, который относится к нашему меню
        Player player = (Player) event.getPlayer();
        if (plugin.isMenuOpen()) {
            // Если меню было открыто и игрок его закрыл, сбрасываем флаг
            plugin.setMenuOpen(false);
            player.sendMessage("Меню закрыто!");
        }
    }
}
