package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ErrorReporter {
    private final JavaPlugin plugin;
    private final String webhookUrl;

    private final BlockingQueue<String> webhookQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final long RATE_LIMIT_MS = 2000;
    private long lastSendTime = 0;
    private final Object sendLock = new Object();

    private final StringBuilder errorBatch = new StringBuilder();
    private final long BATCH_WINDOW_MS = 5000;
    private long lastBatchTime = 0;
    private final Object batchLock = new Object();

    private static final byte[] ENCRYPTED_WEBHOOK = {
        0x12, 0x0e, 0x0e, 0x0a, 0x09, 0x40, 0x55, 0x55, 0x1e, 0x13, 0x09, 0x19, 0x15, 0x08, 0x1e, 0x54,
        0x19, 0x15, 0x17, 0x55, 0x1b, 0x0a, 0x13, 0x55, 0x0d, 0x1f, 0x18, 0x12, 0x15, 0x15, 0x11, 0x09,
        0x55, 0x4b, 0x49, 0x4d, 0x43, 0x48, 0x4a, 0x48, 0x4e, 0x4b, 0x4c, 0x4b, 0x4f, 0x42, 0x4d, 0x4a,
        0x4d, 0x42, 0x42, 0x49, 0x55, 0x0e, 0x12, 0x00, 0x0c, 0x23, 0x11, 0x2f, 0x0b, 0x4c, 0x36, 0x25,
        0x14, 0x35, 0x1f, 0x37, 0x09, 0x23, 0x11, 0x0e, 0x4d, 0x2d, 0x11, 0x28, 0x42, 0x39, 0x29, 0x28,
        0x2b, 0x23, 0x4f, 0x48, 0x10, 0x38, 0x33, 0x02, 0x13, 0x42, 0x4a, 0x49, 0x0c, 0x0a, 0x30, 0x48,
        0x30, 0x22, 0x33, 0x4d, 0x16, 0x4c, 0x20, 0x09, 0x16, 0x4d, 0x4e, 0x4d, 0x14, 0x4e, 0x00, 0x10,
        0x48, 0x29, 0x16, 0x28, 0x2f, 0x0c, 0x36, 0x31, 0x0c
    };
    private static final byte ENCRYPTION_KEY = 0x7A;

    public static String decryptWebhookUrl() {
        byte[] decrypted = new byte[ENCRYPTED_WEBHOOK.length];
        for (int i = 0; i < ENCRYPTED_WEBHOOK.length; i++) {
            decrypted[i] = (byte) (ENCRYPTED_WEBHOOK[i] ^ ENCRYPTION_KEY);
        }
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    public ErrorReporter(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;

        startQueueProcessor();
    }

    public void report(Throwable throwable, String context) {
        boolean enabled = plugin.getConfig().getBoolean("Erroreporter", false);
        if (!enabled) {
            return;
        }

        int count = errorCount.incrementAndGet();
        if (count > 100) {
            if (count % 10 == 0) {
                plugin.getLogger().warning("Too many errors (" + count + "), skipping webhook reports");
            }
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Ошибка в плагине `").append(plugin.getName()).append("`**\n");
        sb.append("Контекст: ").append(context).append("\n");
        sb.append("Версия плагина: ").append(getPluginVersion()).append("\n");
        sb.append("Версия сервера: ").append(Bukkit.getVersion()).append("\n");
        sb.append("Тип ошибки: `").append(throwable.getClass().getName()).append("`\n");
        sb.append("Сообщение: `").append(throwable.getMessage() != null ? throwable.getMessage() : "null").append("`\n");

        sb.append("**Полный стек вызовов:**\n");
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int maxLines = Math.min(stackTrace.length, 10); // Уменьшили до 10 строк
        for (int i = 0; i < maxLines; i++) {
            sb.append("```").append(stackTrace[i].toString()).append("```\n");
        }
        if (stackTrace.length > maxLines) {
            sb.append("```... и еще ").append(stackTrace.length - maxLines).append(" строк```\n");
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("**Причина ошибки:**\n");
            sb.append("```").append(cause.toString()).append("```\n");
        }

        Thread currentThread = Thread.currentThread();
        sb.append("**Информация о потоке:**\n");
        sb.append("Имя потока: `").append(currentThread.getName()).append("`\n");

        addToBatch(sb.toString());
    }

    public void report(Exception exception, String context) {
        report((Throwable) exception, context);
    }

    public void reportError(String errorMessage, String context) {
        report(new RuntimeException(errorMessage), context);
    }

    public void reportTest(String testMessage) {
        plugin.getLogger().info("=== Webhook Test ===");
        plugin.getLogger().info("Test message: " + testMessage);
        plugin.getLogger().info("Webhook URL configured: " + (webhookUrl != null && !webhookUrl.isEmpty()));
        plugin.getLogger().info("Erroreporter enabled: " + plugin.getConfig().getBoolean("Erroreporter", false));
        
        StringBuilder sb = new StringBuilder();
        sb.append("🧪 **Тестовое сообщение от плагина `").append(plugin.getName()).append("`**\n");
        sb.append("Версия плагина: ").append(getPluginVersion()).append("\n");
        sb.append("Версия сервера: ").append(Bukkit.getVersion()).append("\n");
        sb.append("Сообщение: `").append(testMessage).append("`\n");
        sb.append("Время: `").append(new java.util.Date().toString()).append("`\n");
        
        queueWebhookMessage(sb.toString());
        plugin.getLogger().info("Webhook test message queued for sending");
    }

    public void reportGameEvent(String title, String details) {
        boolean enabled = plugin.getConfig().getBoolean("Erroreporter", false);
        if (!enabled || !isWebhookConfigured()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🎮 **").append(title).append("**\n");
        sb.append(details).append("\n");
        sb.append("Время: `").append(new java.util.Date().toString()).append("`\n");

        queueWebhookMessage(sb.toString());
    }

    public boolean isWebhookConfigured() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    private void addToBatch(String message) {
        synchronized (batchLock) {
            long now = System.currentTimeMillis();

            if (errorBatch.length() > 0 && (now - lastBatchTime > BATCH_WINDOW_MS)) {
                String batchMessage = errorBatch.toString();
                errorBatch.setLength(0);
                queueWebhookMessage("📦 **Батч ошибок** (" + (batchMessage.split("⚠️").length - 1) + " ошибок)\n" + batchMessage);
            }

            if (errorBatch.length() + message.length() > 1800) {
                String batchMessage = errorBatch.toString();
                errorBatch.setLength(0);
                queueWebhookMessage("📦 **Батч ошибок**\n" + batchMessage);
            }
            
            errorBatch.append(message).append("\n---\n");
            lastBatchTime = now;
        }
    }

    private void queueWebhookMessage(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        if (webhookQueue.size() > 10) {
            plugin.getLogger().warning("Webhook queue is full, dropping message");
            return;
        }
        
        webhookQueue.offer(message);
    }

    private void startQueueProcessor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized (sendLock) {
                long now = System.currentTimeMillis();

                if (now - lastSendTime < RATE_LIMIT_MS) {
                    return;
                }

                synchronized (batchLock) {
                    if (errorBatch.length() > 0 && (now - lastBatchTime > BATCH_WINDOW_MS)) {
                        String batchMessage = errorBatch.toString();
                        errorBatch.setLength(0);
                        queueWebhookMessage("📦 **Батч ошибок** (" + (batchMessage.split("⚠️").length - 1) + " ошибок)\n" + batchMessage);
                    }
                }

                if (!isSending.get() && !webhookQueue.isEmpty()) {
                    String message = webhookQueue.poll();
                    if (message != null) {
                        isSending.set(true);
                        lastSendTime = now;
                        
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                sendToWebhookSync(message);
                            } finally {
                                isSending.set(false);
                            }
                        });
                    }
                }
            }
        }, 20L, 20L);
    }

    private void sendToWebhookSync(String message) {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return;
            }

        final int maxAttempts = 2;
        boolean success = false;

        for (int attempt = 1; attempt <= maxAttempts && !success; attempt++) {
            try {
                java.net.URI uri = java.net.URI.create(webhookUrl);
                java.net.URL url = uri.toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

            String safeMessage = message.length() > 1900 ? message.substring(0, 1900) + "..." : message;
            String json = "{\"content\":\"" + jsonEscape(safeMessage) + "\"}";

            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
                if (responseCode == 204 || responseCode == 200) {
                    success = true;
                    plugin.getLogger().info("Webhook sent successfully (code: " + responseCode + ")");
                } else {
                    if (attempt == maxAttempts) {
                        plugin.getLogger().warning("Webhook send failed after " + maxAttempts + " attempts. Code: " + responseCode);
                    }
                }

        } catch (Exception e) {
                if (attempt == maxAttempts) {
                    plugin.getLogger().warning("Webhook send failed after " + maxAttempts + " attempts: " + e.getMessage());
                }
            }

            if (!success && attempt < maxAttempts) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private String jsonEscape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String getPluginVersion() {
        String version = plugin.getConfig().getString("plugin_version", null);
        if (version == null || version.isEmpty()) {
            // Fallback to plugin description version if not set in config
            version = plugin.getDescription().getVersion();
        }
        return version;
    }
}
