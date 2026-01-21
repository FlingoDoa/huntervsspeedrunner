package me.example.huntervsspeedrunner.random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class EnchantTask implements Listener {
    private final RandomTaskManager taskManager;

    public EnchantTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public Task generate(Player player, List<String> enchantments) {
        Random random = new Random();
        List<String> items = Arrays.asList("DIAMOND_SWORD", "IRON_PICKAXE", "NETHERITE_BOOTS", "GOLDEN_LEGGINGS", "DIAMOND_HELMET");
        String selectedItem = items.get(random.nextInt(items.size()));
        int enchCount = random.nextInt(2) + 2;
        Collections.shuffle(enchantments);
        List<String> selectedEnchants = enchantments.subList(0, Math.min(enchCount, enchantments.size()));
        String taskDescription = "Enchant " + selectedItem + " with: " + String.join(", ", selectedEnchants);
        return new Task(taskDescription, p -> updateProgress(p, selectedItem, selectedEnchants));
    }

    private boolean updateProgress(Player player, String item, List<String> enchantments) {
        Material material = Material.getMaterial(item.toUpperCase());
        if (material == null) return false;

        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                int count = 0;
                for (String ench : enchantments) {
                    Enchantment enchantment = Enchantment.getByName(ench.toUpperCase());
                    if (enchantment != null && itemStack.containsEnchantment(enchantment)) {
                        count++;
                    }
                }

                double progress = (double) count / enchantments.size();
                taskManager.updateTaskProgress(player, progress);
                return count == enchantments.size();
            }
        }
        return false;
    }
}
