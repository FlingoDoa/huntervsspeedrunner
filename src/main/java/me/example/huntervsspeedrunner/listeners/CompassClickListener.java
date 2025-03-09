package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CompassClickListener implements Listener {
    private final HunterVSSpeedrunnerPlugin plugin;

    public CompassClickListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() != Material.COMPASS) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        List<Player> speedrunners = plugin.getLifeManager().getSpeedrunners();
        if (speedrunners.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no speedrunners to track.");
            return;
        }
        Player currentTarget = plugin.getCompassManager().getCurrentTarget(player);
        int currentIndex = speedrunners.indexOf(currentTarget);
        Player nextTarget = speedrunners.get((currentIndex + 1) % speedrunners.size());
        plugin.getCompassManager().setCurrentTarget(player, nextTarget);
        player.setCompassTarget(nextTarget.getLocation());
        player.sendMessage(ChatColor.GREEN + "Compass is now pointing to: " + ChatColor.YELLOW + nextTarget.getName());
    }
}
