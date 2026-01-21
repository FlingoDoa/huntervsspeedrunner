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
    private final BossBar bossBar;
    private final HunterVSSpeedrunnerPlugin plugin;
    private boolean isRandomModeEnabled = false;
    private FileConfiguration taskConfig;
    private File taskFile;
    private final Map<UUID, Integer> playerScenarioIndex = new HashMap<>();
    private List<String> currentScenario = new ArrayList<>();


    public RandomTaskManager(File pluginFolder, FileConfiguration pluginConfig, HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
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

    public void reloadTaskConfig() {
        loadTaskConfig();
    }

    public FileConfiguration getTaskConfig() {
        return taskConfig;
    }

    public void addSimpleTask(String difficulty, String category, String value) throws Exception {
        ConfigurationSection root = taskConfig.getConfigurationSection(difficulty);
        if (root == null) {
            root = taskConfig.createSection(difficulty);
        }
        ConfigurationSection cat = root.getConfigurationSection(category);
        if (cat == null) {
            cat = root.createSection(category);
        }
        int next = cat.getKeys(false).size() + 1;
        cat.set(String.valueOf(next), value.toLowerCase());
        saveTaskConfig();
    }

    public void addEnchantTask(String difficulty, String itemName, List<String> enchants) throws Exception {
        ConfigurationSection root = taskConfig.getConfigurationSection(difficulty);
        if (root == null) root = taskConfig.createSection(difficulty);
        ConfigurationSection enchant = root.getConfigurationSection("enchant");
        if (enchant == null) enchant = root.createSection("enchant");
        ConfigurationSection itemSec = enchant.getConfigurationSection("item");
        if (itemSec == null) itemSec = enchant.createSection("item");

        int nextItem = itemSec.getKeys(false).size() + 1;
        ConfigurationSection newItem = itemSec.createSection(String.valueOf(nextItem));
        newItem.set("name", itemName.toLowerCase());
        ConfigurationSection typeSec = newItem.createSection("type");
        int idx = 1;
        for (String ench : enchants) {
            typeSec.set(String.valueOf(idx++), ench.toLowerCase());
        }
        saveTaskConfig();
    }

    private void saveTaskConfig() throws Exception {
        taskConfig.save(taskFile);
    }

    public Task generateRandomTask(Player player) {
        FileConfiguration pluginConfig = plugin.getConfig();
        String difficulty = pluginConfig.getString("random_tasks.difficulty", "easy");
        return generateRandomTask(player, difficulty);
    }

    public Task generateRandomTask(Player player, String difficulty) {
        Random random = new Random();

        if (taskConfig == null) {
            plugin.getLogger().severe("❌ ERROR: task.yml is not loaded!");
            return new Task("⚠ No tasks available (task.yml not loaded)", p -> false);
        }
        
        ConfigurationSection difficultySection = taskConfig.getConfigurationSection(difficulty);
        if (difficultySection == null) {
            plugin.getLogger().severe("❌ ERROR: Difficulty section '" + difficulty + "' is missing in task.yml!");
            return new Task("⚠ No tasks available (difficulty error)", p -> false);
        }

        List<String> categories = new ArrayList<>(difficultySection.getKeys(false));
        if (categories.isEmpty()) {
            plugin.getLogger().warning("⚠ No task categories found! Using default task.");
            return new Task("⚠ No categories available", p -> false);
        }

        int attempts = 3;

        while (attempts > 0) {
            attempts--;

            String selectedCategory = categories.get(random.nextInt(categories.size()));
            plugin.getLogger().info("🔍 Попытка создать задание, выбрана категория: " + selectedCategory);
            ConfigurationSection taskSection = difficultySection.getConfigurationSection(selectedCategory);

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
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [InventoryTask] Task for " + playerName + ": " + taskDescription);

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
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [KillTask] Task for " + playerName + ": " + taskDescription);

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
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [EffectTask] Task for " + playerName + ": " + taskDescription);

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
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [AchievementTask] Task for " + playerName + ": " + taskDescription);

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
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [EnchantTask] Task for " + playerName + ": " + taskDescription);
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
        if (!playerTasks.containsKey(player.getUniqueId())) {
            return;
        }

        bossBar.setVisible(false);
        player.sendMessage(ChatColor.GREEN + "✅ Вы выполнили задание!");

        advanceScenarioOrFinish(player);
    }


    public void startTaskChecking(Player player, Task task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!GameManager.isGameStarted()) {
                    cancel();
                    return;
                }
                
                if (!plugin.getLifeManager().isSpeedrunner(player)) {
                    cancel();
                    return;
                }
                
                boolean completed = task.getCondition().test(player);

                if (completed) {
                    completeTask(player);
                        cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }


    public void hideTaskFromAllPlayers() {
        bossBar.setVisible(false);
        bossBar.removeAll();
        playerTasks.clear();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void updateTaskProgress(Player player, double progress) {
        bossBar.setProgress(progress);
    }

    public void reassignBossBar(Player player) {
        if (!GameManager.isGameStarted()) return;

        if (plugin.getLifeManager().isSpeedrunner(player) || plugin.getLifeManager().isHunter(player)) {
            if (bossBar.isVisible()) {
                bossBar.addPlayer(player);
            }
            Task task = playerTasks.get(player.getUniqueId());
            if (task != null) {
                player.sendMessage(ChatColor.YELLOW + "📌 Текущее задание: " + task.getDescription());
            }
        }
    }

    public void generateTasksForAllSpeedrunners() {
        if (!isRandomModeEnabled) return;
        
        currentScenario = buildScenario();
        if (currentScenario.isEmpty()) {
            plugin.getLogger().warning("⚠ Сценарий задач пуст – задания не будут сгенерированы.");
            return;
        }

        playerTasks.clear();
        playerScenarioIndex.clear();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                UUID id = player.getUniqueId();
                playerScenarioIndex.put(id, 0);
                String difficulty = currentScenario.get(0);
                Task task = generateRandomTask(player, difficulty);
                playerTasks.put(id, task);
                startTaskChecking(player, task);
            }
        }
        
        // Показываем первое задание (берем первое найденное)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                Task task = playerTasks.get(player.getUniqueId());
                if (task != null) {
                    showTaskToAllPlayers(task);
                    break;
                }
            }
        }
    }

    public void showTaskToAllSpeedrunners() {
        if (!isRandomModeEnabled) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                Task task = playerTasks.get(player.getUniqueId());
                if (task != null) {
                    showTaskToAllPlayers(task);
                    break; // Показываем только одно задание для всех
                }
            }
        }
    }

    public void startTaskCheckingForAllSpeedrunners() {
        if (!isRandomModeEnabled) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                Task task = playerTasks.get(player.getUniqueId());
                if (task != null) {
                    startTaskChecking(player, task);
                }
            }
        }
    }

    private List<String> buildScenario() {
        FileConfiguration cfg = plugin.getConfig();
        // Новый формат – явный список сценария random_tasks.scenario: [easy, hard, easy, ...]
        List<String> scenario = cfg.getStringList("random_tasks.scenario");
        if (scenario != null && !scenario.isEmpty()) {
            return new ArrayList<>(scenario);
        }

        // Если сценарий пуст, выбираем рандомный пресет
        java.util.Random random = new java.util.Random();
        int presetChoice = random.nextInt(9); // 0-8 для 9 пресетов
        
        scenario = new ArrayList<>();
        switch (presetChoice) {
            case 0: // 1H + 1M + 1E
                scenario.add("hard");
                scenario.add("medium");
                scenario.add("easy");
                break;
            case 1: // 3E
                scenario.add("easy");
                scenario.add("easy");
                scenario.add("easy");
                break;
            case 2: // 2M
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 3: // 2H
                scenario.add("hard");
                scenario.add("hard");
                break;
            case 4: // 2E + 1M
                scenario.add("easy");
                scenario.add("easy");
                scenario.add("medium");
                break;
            case 5: // 1E + 2M
                scenario.add("easy");
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 6: // 1H + 2E
                scenario.add("hard");
                scenario.add("easy");
                scenario.add("easy");
                break;
            case 7: // 3M
                scenario.add("medium");
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 8: // 3H
                scenario.add("hard");
                scenario.add("hard");
                scenario.add("hard");
                break;
        }
        
        plugin.getLogger().info("📋 Сценарий пуст, выбран случайный пресет: " + scenario);
        return scenario;
    }

    private void advanceScenarioOrFinish(Player player) {
        UUID id = player.getUniqueId();

        if (!playerScenarioIndex.containsKey(id) || currentScenario.isEmpty()) {
            playerTasks.remove(id);
        } else {
            int idx = playerScenarioIndex.get(id) + 1;
            if (idx < currentScenario.size()) {
                playerScenarioIndex.put(id, idx);
                String difficulty = currentScenario.get(idx);
                Task nextTask = generateRandomTask(player, difficulty);
                playerTasks.put(id, nextTask);
                startTaskChecking(player, nextTask);
                showTaskToAllPlayers(nextTask);
                player.sendMessage(ChatColor.AQUA + "📌 Новое задание: " + nextTask.getDescription());
                return;
            } else {
                playerScenarioIndex.remove(id);
                playerTasks.remove(id);
            }
        }

        boolean allTasksCompleted = true;
        for (Player speedrunner : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(speedrunner) &&
                    playerTasks.containsKey(speedrunner.getUniqueId())) {
                allTasksCompleted = false;
                break;
            }
        }

        if (allTasksCompleted) {
            FileConfiguration config = plugin.getConfig();
            String language = config.getString("language", "en");
            String victoryMessage = config.getString(language + ".messages.speedrunners_win", "🏆 Speedrunners Win!");

            Bukkit.broadcastMessage(ChatColor.GOLD + victoryMessage);

            String titleCommand = String.format(
                    "title @a title {\"text\":\"%s\", \"color\":\"gold\"}", victoryMessage
            );
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), titleCommand);

            GameManager.endGame(plugin, "Спидраннеры", "Все задания выполнены");
        }
    }

}
