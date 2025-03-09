package me.example.huntervsspeedrunner.random;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.io.File;

public class RandomTaskManager implements Listener {
    private final Map<UUID, Task> playerTasks = new HashMap<>();
    private final Map<String, List<Task>> categorizedTasks = new HashMap<>();
    private final BossBar bossBar;
    private final Plugin plugin;
    private final String language;
    private boolean isRandomModeEnabled = false;
    private FileConfiguration taskConfig;
    private File taskFile;


    public RandomTaskManager(File pluginFolder, FileConfiguration pluginConfig, Plugin plugin) {
        this.plugin = plugin;
        this.language = pluginConfig.getString("language", "en");
        bossBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setVisible(false);
        loadTaskConfig();

        Bukkit.getPluginManager().registerEvents(new InventoryTask(this), plugin);
        Bukkit.getPluginManager().registerEvents(new KillTask(this), plugin);
        Bukkit.getPluginManager().registerEvents(new EffectTask(this), plugin);
        Bukkit.getPluginManager().registerEvents(new AchievementTask(this), plugin);
        Bukkit.getPluginManager().registerEvents(new EnchantTask(this), plugin);
    }

    private void loadTaskConfig() {
        taskFile = new File(plugin.getDataFolder(), "task.yml");

        if (!taskFile.exists()) {
            plugin.saveResource("task.yml", false);
        }

        taskConfig = YamlConfiguration.loadConfiguration(taskFile);
    }

    public Task generateRandomTask(Player player) {
        Random random = new Random();

        if (taskConfig == null) {
            plugin.getLogger().severe("❌ ERROR: task.yml is not loaded!");
            return new Task("⚠ No tasks available (task.yml not loaded)", p -> false);
        }

        ConfigurationSection categoriesSection = taskConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().severe("❌ ERROR: 'categories' section is missing in task.yml!");
            return new Task("⚠ No tasks available (config error)", p -> false);
        }

        List<String> categories = new ArrayList<>(categoriesSection.getKeys(false));
        if (categories.isEmpty()) {
            plugin.getLogger().warning("⚠ No task categories found! Using default task.");
            return new Task("⚠ No categories available", p -> false);
        }

        int attempts = 3;

        while (attempts > 0) {
            attempts--;

            String selectedCategory = categories.get(random.nextInt(categories.size()));
            plugin.getLogger().info("🔍 Попытка создать задание, выбрана категория: " + selectedCategory);
            ConfigurationSection taskSection = categoriesSection.getConfigurationSection(selectedCategory);

            if (taskSection == null || taskSection.getKeys(false).isEmpty()) {
                plugin.getLogger().warning("⚠ No tasks found in category: " + selectedCategory + ". Retrying...");
                continue;
            }

            List<String> taskKeys = new ArrayList<>(taskSection.getKeys(false));

            switch (selectedCategory) {
                case "inventory": {
                    int count = random.nextInt(64) + 1;
                    String itemKey = taskKeys.get(random.nextInt(taskKeys.size()));
                    String item = taskSection.getString(itemKey);

                    if (item == null) {
                        plugin.getLogger().severe("❌ Ошибка: itemKey '" + itemKey + "' не найден в 'inventory'!");
                        continue;
                    }

                    String taskDescription = "Collect " + count + "x " + item.toUpperCase();
                    plugin.getLogger().info("📌 [InventoryTask] Task for " + player.getName() + ": " + taskDescription);

                    return new InventoryTask(this).generate(player, Collections.singletonList(item), count);
                }

                case "kill": {
                    int count = random.nextInt(40) + 1;
                    String mobKey = taskKeys.get(random.nextInt(taskKeys.size()));
                    String mob = taskSection.getString(mobKey);

                    if (mob == null) {
                        plugin.getLogger().severe("❌ Ошибка: mobKey '" + mobKey + "' не найден в 'kill'!");
                        continue;
                    }

                    String taskDescription = "Kill " + count + " " + mob.toUpperCase();
                    plugin.getLogger().info("📌 [KillTask] Task for " + player.getName() + ": " + taskDescription);

                    return new KillTask(this).generate(player, Collections.singletonList(mob), count);
                }

                case "effect": {
                    int count = random.nextInt(3) + 1;
                    Collections.shuffle(taskKeys);
                    List<String> effectKeys = taskKeys.subList(0, Math.min(count, taskKeys.size()));

                    List<String> effects = new ArrayList<>();
                    for (String key : effectKeys) {
                        String effect = taskSection.getString(key);
                        if (effect != null) effects.add(effect);
                    }

                    if (effects.isEmpty()) {
                        plugin.getLogger().severe("❌ Ошибка: effects не найдены в 'effect'!");
                        continue;
                    }

                    String taskDescription = "Gain effects: " + String.join(", ", effects);
                    plugin.getLogger().info("📌 [EffectTask] Task for " + player.getName() + ": " + taskDescription);

                    return new EffectTask(this).generate(player, effects);
                }

                case "achievement": {
                    int count = random.nextInt(3) + 1;
                    Collections.shuffle(taskKeys);
                    List<String> achievementKeys = taskKeys.subList(0, Math.min(count, taskKeys.size()));

                    List<String> achievements = new ArrayList<>();
                    for (String key : achievementKeys) {
                        String achievement = taskSection.getString(key);
                        if (achievement != null) achievements.add(achievement);
                    }

                    if (achievements.isEmpty()) {
                        plugin.getLogger().severe("❌ Ошибка: achievements не найдены в 'achievement'!");
                        continue;
                    }

                    String taskDescription = "Unlock achievements: " + String.join(", ", achievements);
                    plugin.getLogger().info("📌 [AchievementTask] Task for " + player.getName() + ": " + taskDescription);

                    return new AchievementTask(this).generate(player, achievements);
                }

                case "enchant": {
                    ConfigurationSection enchantSection = taskSection.getConfigurationSection("item");
                    if (enchantSection == null) {
                        plugin.getLogger().severe("❌ Ошибка: 'item' секция отсутствует в 'enchant'!");
                        continue;
                    }

                    List<String> itemKeys = new ArrayList<>(enchantSection.getKeys(false));
                    String itemKey = itemKeys.get(random.nextInt(itemKeys.size()));
                    String item = enchantSection.getString(itemKey);

                    ConfigurationSection typeSection = enchantSection.getConfigurationSection(itemKey + ".type");
                    if (typeSection == null) {
                        plugin.getLogger().severe("❌ Ошибка: 'type' секция отсутствует для предмета '" + item + "' в 'enchant'!");
                        continue;
                    }

                    List<String> enchKeys = new ArrayList<>(typeSection.getKeys(false));
                    int enchCount = random.nextInt(2) + 2;
                    Collections.shuffle(enchKeys);
                    List<String> selectedEnchants = new ArrayList<>();
                    for (int i = 0; i < Math.min(enchCount, enchKeys.size()); i++) {
                        selectedEnchants.add(typeSection.getString(enchKeys.get(i)));
                    }

                    if (selectedEnchants.isEmpty()) {
                        plugin.getLogger().severe("❌ Ошибка: зачарования не найдены для '" + item + "'!");
                        continue;
                    }

                    String taskDescription = "Enchant " + item.toUpperCase() + " with: " + String.join(", ", selectedEnchants);
                    plugin.getLogger().info("📌 [EnchantTask] Task for " + player.getName() + ": " + taskDescription);

                    return new EnchantTask(this).generate(player, selectedEnchants);
                }

                default:
                    plugin.getLogger().severe("❌ [ERROR] Неизвестное задание! Категория: " + selectedCategory);
            }
        }

        plugin.getLogger().severe("❌ [ERROR] Не удалось найти подходящее задание после 3 попыток!");
        return new Task("⚠ Unknown Task", p -> false);
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

    public void showTaskToAllPlayers(Task task) {
        if (task == null) {
            plugin.getLogger().warning("⚠ Attempted to show a null task to players!");
            return;
        }

        bossBar.setTitle(ChatColor.YELLOW + task.getDescription());
        bossBar.setProgress(0.0);
        bossBar.setVisible(true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    public void completeTask(Player player) {


        bossBar.setVisible(false);
        player.sendMessage(ChatColor.GREEN + "✅ Вы выполнили задание!");

        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language", "en");
        String victoryMessage = config.getString(language + ".messages.speedrunners_win", "🏆 Speedrunners Win!");

        Bukkit.broadcastMessage(ChatColor.GOLD + victoryMessage);

        String titleCommand = String.format(
                "title @a title {\"text\":\"%s\", \"color\":\"gold\"}", victoryMessage
        );
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);

        GameManager.endGame((HunterVSSpeedrunnerPlugin) plugin);
    }


    public void startTaskChecking(Player player, Task task) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!GameManager.isGameStarted()) {
                    cancel();
                    return;
                }
                boolean completed = task.getCondition().test(player);

                if (completed) {
                    player.sendMessage(ChatColor.GREEN + "✅ You have completed the task: " + task.getDescription());
                    bossBar.setVisible(false);
                    cancel();

                    FileConfiguration config = plugin.getConfig();
                    String language = config.getString("language", "en");
                    String victoryMessage = config.getString(language + ".messages.speedrunners_win", "🏆 Speedrunners Win!");

                    Bukkit.broadcastMessage(ChatColor.GOLD + victoryMessage);

                    String titleCommand = String.format(
                            "title @a title {\"text\":\"%s\", \"color\":\"gold\"}", victoryMessage
                    );
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);
                    ;

                    GameManager.endGame((HunterVSSpeedrunnerPlugin) plugin);
                } else {
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }


    public void hideTaskFromAllPlayers() {
        bossBar.setVisible(false);
        bossBar.removeAll();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void updateTaskProgress(Player player, double progress) {
        bossBar.setProgress(progress);
        plugin.getLogger().info("🔄 [DEBUG] Прогресс задания для " + player.getName() + ": " + (int) (progress * 100) + "%");
    }

}
