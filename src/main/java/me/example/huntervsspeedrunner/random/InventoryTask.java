package me.example.huntervsspeedrunner.random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class InventoryTask implements Listener {
    private final RandomTaskManager taskManager;

    public InventoryTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public Task generate(Player player, List<String> items, int count) {
        Random random = new Random();
        String selectedItem = items.get(random.nextInt(items.size()));

        String taskDescription = "Collect " + count + "x " + selectedItem;
        return new Task(taskDescription, p -> updateProgress(p, selectedItem, count));
    }

    private boolean updateProgress(Player player, String item, int requiredAmount) {
        Material material = Material.getMaterial(item.toUpperCase());
        if (material == null) {
            return false;
        }

        int currentAmount = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                currentAmount += itemStack.getAmount();
            }
        }

        double progress = (double) currentAmount / requiredAmount;
        taskManager.updateTaskProgress(player, Math.min(progress, 1.0));

        return currentAmount >= requiredAmount;
    }
}
