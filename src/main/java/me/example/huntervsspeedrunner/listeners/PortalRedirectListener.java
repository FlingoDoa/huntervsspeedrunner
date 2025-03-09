package me.example.huntervsspeedrunner.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.util.Vector;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;

public class PortalRedirectListener implements Listener {

    private final HunterVSSpeedrunnerPlugin plugin;

    public PortalRedirectListener(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) return;

        String eventWorldName = plugin.getConfig().getString("event.worldName");
        if (fromWorld.getName().equalsIgnoreCase(eventWorldName)
                && event.getTo() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            World targetNether = plugin.getServer().getWorld(eventWorldName + "_nether");
            if (targetNether != null) {
                Location portalLocation = findOrCreatePortal(targetNether, event.getFrom());
                event.setTo(portalLocation);
            }
        }
        else if (fromWorld.getName().equalsIgnoreCase(eventWorldName + "_nether")) {
            World targetWorld = plugin.getServer().getWorld(eventWorldName);
            if (targetWorld != null) {
                Location portalLocation = findOrCreatePortal(targetWorld, event.getFrom());
                event.setTo(portalLocation);
            }
        }
        else if (fromWorld.getName().equalsIgnoreCase(eventWorldName)
                && event.getTo() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            World targetEnd = plugin.getServer().getWorld(eventWorldName + "_the_end");
            if (targetEnd != null) {
                Location platformLocation = getEndPlatformLocation(targetEnd);
                event.setTo(platformLocation);
            }
        }
        else if (fromWorld.getName().equalsIgnoreCase(eventWorldName + "_the_end")) {
            World targetWorld = plugin.getServer().getWorld(eventWorldName);
            if (targetWorld != null) {
                Location portalLocation = findOrCreatePortal(targetWorld, event.getFrom());
                event.setTo(portalLocation);
            }
        }
    }

    private Location findOrCreatePortal(World targetWorld, Location fromLocation) {
        Vector scaledCoords = scaleCoordinates(fromLocation, targetWorld.getEnvironment());
        Location targetLocation = new Location(targetWorld, scaledCoords.getX(), scaledCoords.getY(), scaledCoords.getZ());
        Location nearestPortal = targetWorld.getBlockAt(targetLocation).getLocation(); // Упростите или добавьте поиск порталов
        if (nearestPortal != null) {
            return nearestPortal;
        }
        return createPortal(targetWorld, targetLocation);
    }

    private Vector scaleCoordinates(Location fromLocation, World.Environment targetEnvironment) {
        double scaleFactor = (targetEnvironment == World.Environment.NETHER) ? 0.125 : 8.0;
        return fromLocation.toVector().multiply(scaleFactor);
    }

    private Location createPortal(World world, Location location) {
        Location portalBase = new Location(world, location.getX(), world.getHighestBlockYAt(location), location.getZ());

        // Генерация рамки портала
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                portalBase.clone().add(x, y, 0).getBlock().setType(Material.OBSIDIAN);
            }
        }
        for (int y = 1; y <= 2; y++) {
            portalBase.clone().add(0, y, 0).getBlock().setType(Material.NETHER_PORTAL);
        }
        return portalBase;
    }

    private Location getEndPlatformLocation(World endWorld) {
        Location platformLocation = locateEndPlatformLocation(endWorld);
        createEndPlatform(platformLocation);
        return platformLocation;
    }


    private Location locateEndPlatformLocation(World endWorld) {
        int x, z;

        do {
            x = (int) (Math.random() * 201 - 100);
            z = (int) (Math.random() * 201 - 100);
        } while (Math.abs(x) < 60 && Math.abs(z) < 60);

        Location platformLocation = new Location(endWorld, x, 50, z);

        if (isOnMainIsland(platformLocation)) {
            return platformLocation;
        }
        return locateEndPlatformLocation(endWorld);
    }

    private boolean isOnMainIsland(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.END_STONE;
    }

    private void createEndPlatform(Location location) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location blockLocation = location.clone().add(x, 0, z);
                blockLocation.getBlock().setType(Material.OBSIDIAN);
            }
        }
    }
}
