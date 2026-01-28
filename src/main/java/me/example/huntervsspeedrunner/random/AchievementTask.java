package me.example.huntervsspeedrunner.random;

import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import java.util.*;
import me.example.huntervsspeedrunner.utils.I18n;

public class AchievementTask implements Listener {
    private final RandomTaskManager taskManager;

    public AchievementTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public Task generate(Player player, List<String> achievements) {
        Random random = new Random();
        int count = random.nextInt(3) + 1;
        Collections.shuffle(achievements);
        List<String> selectedAchievements = achievements.subList(0, Math.min(count, achievements.size()));
        List<String> pretty = new ArrayList<>();
        for (String a : selectedAchievements) {
            pretty.add(I18n.taskName(taskManager.getMainPlugin(), "advancements", String.valueOf(a).toLowerCase()));
        }
        String taskDescription = I18n.msg(taskManager.getMainPlugin(), "task_achievements", String.join(", ", pretty));
        return new Task(taskDescription, p -> updateProgress(p, selectedAchievements));
    }

    private boolean updateProgress(Player player, List<String> achievements) {
        int count = 0;
        for (String achievement : achievements) {
            Advancement advancement = Bukkit.getAdvancement(new org.bukkit.NamespacedKey("minecraft", achievement));
            if (advancement != null && player.getAdvancementProgress(advancement).isDone()) {
                count++;
            }
        }
        double progress = (double) count / achievements.size();
        taskManager.updateTaskProgress(player, progress);
        return count == achievements.size();
    }
}
