package me.example.huntervsspeedrunner.random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public class EffectTask implements Listener {
    private final RandomTaskManager taskManager;

    public EffectTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public Task generate(Player player, List<String> effects) {
        Random random = new Random();
        int effectCount = random.nextInt(3) + 2;
        Collections.shuffle(effects);
        List<String> selectedEffects = effects.subList(0, Math.min(effectCount, effects.size()));
        String taskDescription = "Gain effects: " + String.join(", ", selectedEffects);
        return new Task(taskDescription, p -> updateProgress(p, selectedEffects));
    }

    private boolean updateProgress(Player player, List<String> effects) {
        int count = 0;
        for (String effect : effects) {
            if (player.hasPotionEffect(PotionEffectType.getByName(effect.toUpperCase()))) {
                count++;
            }
        }

        double progress = (double) count / effects.size();
        taskManager.updateTaskProgress(player, progress);
        return count == effects.size();
    }
}
