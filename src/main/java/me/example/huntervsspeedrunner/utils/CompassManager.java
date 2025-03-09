package me.example.huntervsspeedrunner.utils;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CompassManager {
    private final Map<Player, Boolean> compassEnabled = new HashMap<>();
    private final Map<Player, Player> currentTargets = new HashMap<>();

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
}
