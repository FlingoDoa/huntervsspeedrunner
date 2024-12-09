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

        // Переход из основного мира в Нижний мир
        if (fromWorld.getName().equalsIgnoreCase(eventWorldName)
                && event.getTo() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            World targetNether = plugin.getServer().getWorld(eventWorldName + "_nether");
            if (targetNether != null) {
                Location portalLocation = findOrCreatePortal(targetNether, event.getFrom());
                event.setTo(portalLocation);
            }
        }
        // Переход из Нижнего мира в основной
        else if (fromWorld.getName().equalsIgnoreCase(eventWorldName + "_nether")) {
            World targetWorld = plugin.getServer().getWorld(eventWorldName);
            if (targetWorld != null) {
                Location portalLocation = findOrCreatePortal(targetWorld, event.getFrom());
                event.setTo(portalLocation);
            }
        }
        // Переход в Энд
        else if (fromWorld.getName().equalsIgnoreCase(eventWorldName)
                && event.getTo() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            World targetEnd = plugin.getServer().getWorld(eventWorldName + "_the_end");
            if (targetEnd != null) {
                Location platformLocation = getEndPlatformLocation(targetEnd);
                event.setTo(platformLocation);
            }
        }
        // Переход из Энда в основной мир
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

        // Попытка найти ближайший портал
        Location nearestPortal = targetWorld.getBlockAt(targetLocation).getLocation(); // Упростите или добавьте поиск порталов

        if (nearestPortal != null) {
            return nearestPortal;
        }

        // Создать портал вручную, если нет
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
        // Получаем случайную позицию для платформы в радиусе 100 блоков от (0, 0),
        // исключая зону от -60 до 60 по осям X и Z
        Location platformLocation = locateEndPlatformLocation(endWorld);

        // Создаем платформу на найденной локации
        createEndPlatform(platformLocation);
        return platformLocation;
    }


    private Location locateEndPlatformLocation(World endWorld) {
        int x, z;

        do {
            // Генерация случайных координат в пределах от -100 до 100 по X и Z
            x = (int) (Math.random() * 201 - 100); // От -100 до 100
            z = (int) (Math.random() * 201 - 100); // От -100 до 100
        } while (Math.abs(x) < 60 && Math.abs(z) < 60); // Исключаем координаты в зоне от -60 до 60

        // Создаем новую локацию с полученными координатами
        Location platformLocation = new Location(endWorld, x, 50, z);

        // Проверяем, находится ли локация на главном острове
        if (isOnMainIsland(platformLocation)) {
            return platformLocation;
        }

        // Если не нашли подходящее место, пробуем снова
        return locateEndPlatformLocation(endWorld);
    }

    private boolean isOnMainIsland(Location location) {
        // Проверяем, если под игроком есть блоки основного острова (например, эндерняк)
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
