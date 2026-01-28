package me.example.huntervsspeedrunner.random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import me.example.huntervsspeedrunner.utils.I18n;

public class EnchantTask implements Listener {
    private final RandomTaskManager taskManager;

    public EnchantTask(RandomTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public Task generate(Player player, String itemName, List<String> enchantments) {
        String materialName = convertItemNameToMaterial(itemName);

        Material mat = Material.getMaterial(materialName);
        String itemDisplay = (mat != null)
                ? I18n.materialName(taskManager.getMainPlugin(), mat)
                : I18n.taskName(taskManager.getMainPlugin(), "materials", materialName.toLowerCase());

        List<String> prettyEnchants = new ArrayList<>();
        for (String e : enchantments) {
            prettyEnchants.add(I18n.taskName(taskManager.getMainPlugin(), "enchants", String.valueOf(e).toLowerCase()));
        }

        String taskDescription = I18n.msg(taskManager.getMainPlugin(), "task_enchant", itemDisplay, String.join(", ", prettyEnchants));
        return new Task(taskDescription, p -> updateProgress(p, materialName, enchantments));
    }
    
    private String convertItemNameToMaterial(String itemName) {
        String itemLower = itemName.toLowerCase();
        if (itemLower.contains("sword")) {
            return "DIAMOND_SWORD";
        } else if (itemLower.contains("pickaxe")) {
            return "DIAMOND_PICKAXE";
        } else if (itemLower.contains("axe")) {
            return "DIAMOND_AXE";
        } else if (itemLower.contains("shovel")) {
            return "DIAMOND_SHOVEL";
        } else if (itemLower.contains("hoe")) {
            return "DIAMOND_HOE";
        } else if (itemLower.contains("helmet")) {
            return "DIAMOND_HELMET";
        } else if (itemLower.contains("chestplate") || itemLower.contains("chest")) {
            return "DIAMOND_CHESTPLATE";
        } else if (itemLower.contains("leggings") || itemLower.contains("legs")) {
            return "DIAMOND_LEGGINGS";
        } else if (itemLower.contains("boots")) {
            return "DIAMOND_BOOTS";
        } else if (itemLower.contains("bow")) {
            return "BOW";
        } else if (itemLower.contains("crossbow")) {
            return "CROSSBOW";
        } else if (itemLower.contains("trident")) {
            return "TRIDENT";
        }
        return itemName.toUpperCase();
    }

    private boolean updateProgress(Player player, String materialName, List<String> enchantments) {
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = tryFindAlternativeMaterial(materialName);
            if (material == null) return false;
        }

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
    
    private Material tryFindAlternativeMaterial(String materialName) {
        String[] variants = {"DIAMOND_", "IRON_", "NETHERITE_", "GOLDEN_", "WOODEN_", "STONE_"};
        String baseName = materialName;
        for (String prefix : variants) {
            if (materialName.startsWith(prefix)) {
                baseName = materialName.substring(prefix.length());
                break;
            }
        }

        Material mat = Material.getMaterial("DIAMOND_" + baseName);
        if (mat != null) return mat;

        for (String variant : variants) {
            mat = Material.getMaterial(variant + baseName);
            if (mat != null) return mat;
        }
        
        return null;
    }
}
