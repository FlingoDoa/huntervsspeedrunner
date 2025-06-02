package me.example.huntervsspeedrunner.listeners;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.CompassTask;
import me.example.huntervsspeedrunner.utils.GameManager;
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
        if (!GameManager.isGameStarted()) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getLifeManager().isHunter(player)) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() != Material.COMPASS) {
            return;
        }

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        List<Player> speedrunners = plugin.getLifeManager().getSpeedrunners();
        if (speedrunners.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Нет спидраннеров для отслеживания.");
            return;
        }

        Player currentTarget = plugin.getCompassManager().getCurrentTarget(player);
        int currentIndex = currentTarget != null ? speedrunners.indexOf(currentTarget) : -1;
        Player nextTarget = speedrunners.get((currentIndex + 1) % speedrunners.size());
        plugin.getCompassManager().setCurrentTarget(player, nextTarget);
        if (plugin.getCompassManager().getCompassTask(player) != null) {
            plugin.getCompassManager().getCompassTask(player).cancel();
        }

        plugin.getCompassManager().setCompassTask(player,
                new CompassTask(plugin, player, nextTarget).runTaskTimer(plugin, 0L, 20L * 5));

        player.sendMessage(ChatColor.GREEN + "Теперь отслеживается: " + nextTarget.getName());
    }
}