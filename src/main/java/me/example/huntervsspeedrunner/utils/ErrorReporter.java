package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ErrorReporter {
    private final JavaPlugin plugin;
    private final String webhookUrl;

    public ErrorReporter(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
    }

    public void report(Exception exception, String context) {
        boolean enabled = plugin.getConfig().getBoolean("ErrorReporter", false);
        if (!enabled) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Ошибка в плагине `").append(plugin.getName()).append("`**\n");
        sb.append("Контекст: ").append(context).append("\n");
        sb.append("Версия плагина: ").append(plugin.getDescription().getVersion()).append("\n");
        sb.append("Версия сервера: ").append(Bukkit.getVersion()).append("\n");
        sb.append("Ошибка: `").append(exception.toString()).append("`\n");

        int limit = 5;
        for (int i = 0; i < Math.min(exception.getStackTrace().length, limit); i++) {
            sb.append("```").append(exception.getStackTrace()[i].toString()).append("```\n");
        }

        sendToWebhook(sb.toString());
    }

    private void sendToWebhook(String message) {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

            java.net.URL url = new java.net.URL(webhookUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String safeMessage = message.length() > 1900 ? message.substring(0, 1900) + "..." : message;
            String json = "{\"content\":\"" + jsonEscape(safeMessage) + "\"}";

            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204 && responseCode != 200) {
            }

            connection.getInputStream().close();
        } catch (Exception e) {
        }
    }

    private String jsonEscape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
