package me.example.huntervsspeedrunner.utils;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public final class I18n {
    private I18n() {}

    public static String msg(HunterVSSpeedrunnerPlugin plugin, String key, Object... args) {
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");
        String format = config.getString(lang + ".messages." + key, "§c[Missing translation: " + key + "]");
        try {
            return String.format(format, args);
        } catch (Exception e) {
            return format;
        }
    }

    public static String taskName(HunterVSSpeedrunnerPlugin plugin, String category, String id) {
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");
        String path = lang + ".task_names." + category + "." + id;
        String configured = config.getString(path);
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return prettifyId(id);
    }

    public static String materialName(HunterVSSpeedrunnerPlugin plugin, Material material) {
        if (material == null) return taskName(plugin, "materials", "unknown");
        String id = material.name().toLowerCase();
        return taskName(plugin, "materials", id);
    }

    public static String prettifyId(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";
        String s = raw.trim().toLowerCase().replace("minecraft:", "");
        s = s.replace('_', ' ').replace('-', ' ');
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return out.toString().trim();
    }
}

