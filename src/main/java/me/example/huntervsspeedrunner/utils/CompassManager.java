package me.example.huntervsspeedrunner.utils;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;

public class CompassManager {
    private final Map<Player, Boolean> compassEnabled = new HashMap<>();
    private final Map<Player, Player> currentTargets = new HashMap<>();
    private final Map<Player, BukkitTask> compassTasks = new HashMap<>();

    public boolean toggleCompass(Player player) {
        boolean isEnabled = compassEnabled.getOrDefault(player, false);
        compassEnabled.put(player, !isEnabled);
        return !isEnabled;
    }

    public boolean isCompassEnabled(Player player) {
        return compassEnabled.getOrDefault(player, false);
    }

    public Player getCurrentTarget(Player player) {
        return currentTargets.get(player);
    }

    public void setCurrentTarget(Player hunter, Player target) {
        currentTargets.put(hunter, target);
    }

    public void clearTarget(Player hunter) {
        currentTargets.remove(hunter);
    }

    public BukkitTask getCompassTask(Player player) {
        return compassTasks.get(player);
    }

    public void setCompassTask(Player player, BukkitTask task) {
        compassTasks.put(player, task);
    }

    public void cancelCompassTask(Player player) {
        BukkitTask task = compassTasks.get(player);
        if (task != null) {
            task.cancel();
        }
        compassTasks.remove(player);
    }
}