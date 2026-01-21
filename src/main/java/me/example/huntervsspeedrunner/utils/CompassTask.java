package me.example.huntervsspeedrunner.utils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;

public class CompassTask extends BukkitRunnable {

    private final Player hunter;
    private final Player target;
    private final HunterVSSpeedrunnerPlugin plugin;
    private static final int MAX_HEIGHT = 319;

    public CompassTask(HunterVSSpeedrunnerPlugin plugin, Player hunter, Player target) {
        this.plugin = plugin;
        this.hunter = hunter;
        this.target = target;
    }

    @Override
    public void run() {
        if (!GameManager.isGameStarted()) return;
        if (target == null || !target.isOnline()) return;

        World hunterWorld = hunter.getWorld();
        World targetWorld = target.getWorld();

        if (!hunterWorld.equals(targetWorld)) return;

        if (targetWorld.getEnvironment() != World.Environment.NORMAL &&
                !plugin.getCompassManager().isCompassEnabled(hunter)) {
            return;
        }

        ItemStack compass = getCompassFromInventory(hunter);
        if (compass == null) return;

        updateCompass(compass, target.getLocation(), targetWorld, target.getName());
    }

    private void updateCompass(ItemStack compass, Location targetLoc, World targetWorld, String targetName) {
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(getCompassColor(targetWorld) + "Tracking: " + targetName);

        Location lodestoneLoc = targetLoc.clone();
        lodestoneLoc.setY(MAX_HEIGHT);

        Block block = targetWorld.getBlockAt(lodestoneLoc);
        block.setType(Material.LODESTONE);

        meta.setLodestone(lodestoneLoc);
        meta.setLodestoneTracked(false);

        Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.AIR), 1L);

        compass.setItemMeta(meta);
    }

    private ChatColor getCompassColor(World world) {
        switch (world.getEnvironment()) {
            case NETHER: return ChatColor.RED;
            case THE_END: return ChatColor.DARK_PURPLE;
            default: return ChatColor.GREEN;
        }
    }

    private ItemStack getCompassFromInventory(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return (item != null && item.getType() == Material.COMPASS) ? item : null;
    }
}