package me.example.huntervsspeedrunner.random;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillTask implements Listener {
    private final RandomTaskManager taskManager;
    private final Map<UUID, KillTaskData> activeKillTasks = new HashMap<>();

    public KillTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
        Bukkit.getPluginManager().registerEvents(this, taskManager.getPlugin());
    }

    public Task generate(Player player, List<String> mobs, int amount) {
        String targetMob = mobs.get(0).toUpperCase();
        activeKillTasks.put(player.getUniqueId(), new KillTaskData(targetMob, amount, 0));

        String taskDescription = "Kill " + amount + " " + targetMob;
        taskManager.startTaskChecking(player, new Task(taskDescription, p -> hasKilledEnough(p)));

        return new Task(taskDescription, p -> hasKilledEnough(p));
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        UUID playerId = killer.getUniqueId();
        if (!activeKillTasks.containsKey(playerId)) return;

        KillTaskData taskData = activeKillTasks.get(playerId);
        EntityType killedType = event.getEntity().getType();

        if (killedType.name().equalsIgnoreCase(taskData.targetMob)) {
            taskData.kills++;
            double progress = (double) taskData.kills / taskData.requiredKills;
            taskManager.updateTaskProgress(killer, Math.min(progress, 1.0));

            if (taskData.kills >= taskData.requiredKills) {
                activeKillTasks.remove(playerId);
                taskManager.completeTask(killer);
            }
        }
    }

    private boolean hasKilledEnough(Player player) {
        KillTaskData taskData = activeKillTasks.get(player.getUniqueId());
        return taskData != null && taskData.kills >= taskData.requiredKills;
    }

    private static class KillTaskData {
        String targetMob;
        int requiredKills;
        int kills;

        KillTaskData(String targetMob, int requiredKills, int kills) {
            this.targetMob = targetMob;
            this.requiredKills = requiredKills;
            this.kills = kills;
        }
    }
}
