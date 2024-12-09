package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class RandomTaskManager {

    private boolean isRandomModeEnabled = false;
    private final Map<UUID, Task> playerTasks = new HashMap<>(); // Связываем игрока с его задачей
    private final List<Task> tasks;

    public RandomTaskManager() {
        tasks = new ArrayList<>();
        initializeTasks();
    }

    private void initializeTasks() {
        tasks.add(new Task("Find and mine a diamond.", player -> player.getInventory().contains(Material.DIAMOND)));
        tasks.add(new Task("Kill a Blaze.", player -> player.hasMetadata("killed_blaze")));
        tasks.add(new Task("Craft a Netherite Ingot.", player -> player.getInventory().contains(Material.NETHERITE_INGOT)));
        tasks.add(new Task("Obtain a Totem of Undying.", player -> player.getInventory().contains(Material.TOTEM_OF_UNDYING)));
        tasks.add(new Task("Kill a Creeper using TNT.", player -> player.hasMetadata("killed_creeper_with_tnt")));
        tasks.add(new Task("Collect 5 Ender Pearls.", player -> countItems(player, Material.ENDER_PEARL) >= 5));
        tasks.add(new Task("Build a Nether portal.", player -> player.hasMetadata("built_nether_portal")));
        tasks.add(new Task("Tame a horse.", player -> player.hasMetadata("tamed_horse")));
        tasks.add(new Task("Enchant a diamond sword.", player -> player.hasMetadata("enchanted_diamond_sword")));
    }

    public void enableRandomMode() {
        isRandomModeEnabled = true;
    }

    public void disableRandomMode() {
        isRandomModeEnabled = false;
    }

    public boolean isRandomModeEnabled() {
        return isRandomModeEnabled;
    }

    public Task generateRandomTask(Player player) {
        Random random = new Random();
        Task task = tasks.get(random.nextInt(tasks.size())); // Получаем случайную задачу
        playerTasks.put(player.getUniqueId(), task); // Связываем задачу с игроком
        return task;
    }

    public String getPlayerTaskDescription(Player player) {
        Task task = playerTasks.get(player.getUniqueId());
        return task != null ? task.getDescription() : null;
    }

    public boolean checkTaskCompletion(Player player) {
        Task task = playerTasks.get(player.getUniqueId());
        return task != null && task.isCompleted(player); // Проверяем выполнение задачи
    }

    private static int countItems(Player player, Material material) {
        return player.getInventory().all(material).values().stream().mapToInt(itemStack -> itemStack.getAmount()).sum();
    }

    public void showTaskToPlayer(Player player, Task task) {
        player.sendMessage(ChatColor.GREEN + "Your task: " + ChatColor.YELLOW + task.getDescription());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "title " + player.getName() + " title {\"text\":\"" + task.getDescription() + "\",\"color\":\"yellow\"}");
    }

    public static class Task {
        private final String description;
        private final TaskCondition condition;

        public Task(String description, TaskCondition condition) {
            this.description = description;
            this.condition = condition;
        }

        public String getDescription() {
            return description;
        }

        public boolean isCompleted(Player player) {
            return condition.check(player);
        }

        @FunctionalInterface
        public interface TaskCondition {
            boolean check(Player player);
        }
    }
}
