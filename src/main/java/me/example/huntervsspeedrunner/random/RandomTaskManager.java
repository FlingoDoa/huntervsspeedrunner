package me.example.huntervsspeedrunner.random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.enchantments.Enchantment;
import java.util.*;
import java.util.Comparator;
import java.io.File;

public class RandomTaskManager implements Listener {
    private final Map<UUID, Task> playerTasks = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final Map<UUID, BossBar> hunterBossBars = new HashMap<>();
    private final BossBar nonRandomBossBar;
    private final HunterVSSpeedrunnerPlugin plugin;
    private boolean isRandomModeEnabled = false;
    private FileConfiguration taskConfig;
    private File taskFile;
    private final Map<UUID, Integer> playerScenarioIndex = new HashMap<>();
    private final Map<UUID, List<String>> playerScenarios = new HashMap<>();
    private List<String> currentScenario = new ArrayList<>();
    private static final BarColor[] BOSS_BAR_COLORS = {BarColor.YELLOW, BarColor.GREEN, BarColor.BLUE, BarColor.PURPLE, BarColor.RED, BarColor.PINK, BarColor.WHITE};


    public RandomTaskManager(File pluginFolder, FileConfiguration pluginConfig, HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
        nonRandomBossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
        nonRandomBossBar.setVisible(false);
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

    private boolean areEnchantmentsValid(String itemType, List<String> enchantNames) {
        if (enchantNames.isEmpty()) return false;

        for (String enchName : enchantNames) {
            Enchantment ench = Enchantment.getByName(enchName.toUpperCase());
            if (ench == null) return false;

            if (!canEnchantApplyToItem(itemType, ench)) {
                return false;
            }
        }

        if (enchantNames.size() > 1) {
            for (int i = 0; i < enchantNames.size(); i++) {
                for (int j = i + 1; j < enchantNames.size(); j++) {
                    Enchantment ench1 = Enchantment.getByName(enchantNames.get(i).toUpperCase());
                    Enchantment ench2 = Enchantment.getByName(enchantNames.get(j).toUpperCase());
                    if (ench1 != null && ench2 != null && ench1.conflictsWith(ench2)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean canEnchantApplyToItem(String itemType, Enchantment enchantment) {
        String itemLower = itemType.toLowerCase();
        org.bukkit.inventory.ItemStack testItem;

        if (itemLower.contains("sword")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD);
        } else if (itemLower.contains("pickaxe")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE);
        } else if (itemLower.contains("axe")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_AXE);
        } else if (itemLower.contains("shovel")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SHOVEL);
        } else if (itemLower.contains("hoe")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HOE);
        } else if (itemLower.contains("helmet")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET);
        } else if (itemLower.contains("chestplate") || itemLower.contains("chest")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE);
        } else if (itemLower.contains("leggings") || itemLower.contains("legs")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS);
        } else if (itemLower.contains("boots")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS);
        } else if (itemLower.contains("bow")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
        } else if (itemLower.contains("crossbow")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW);
        } else if (itemLower.contains("trident")) {
            testItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TRIDENT);
        } else {
            return true;
        }
        
        return enchantment.canEnchantItem(testItem);
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

        int attempts = 10;

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
                    int count;
                    if (random.nextDouble() < 0.8) {
                        count = 1 + random.nextInt(16);
                    } else {
                        count = 17 + random.nextInt(16);
                    }
                    String itemKey = taskKeys.get(random.nextInt(taskKeys.size()));
                    String item = taskSection.getString(itemKey);

                    if (item == null) {
                        plugin.getLogger().severe("❌ Ошибка: itemKey '" + itemKey + "' не найден в 'inventory'!");
                        continue;
                    }
                    
                    String itemLower = item.toLowerCase();
                    if (itemLower.equals("bedrock") || 
                        itemLower.equals("command_block") || 
                        itemLower.equals("chain_command_block") ||
                        itemLower.equals("repeating_command_block") ||
                        itemLower.equals("end_portal_frame") ||
                        itemLower.equals("barrier") ||
                        itemLower.equals("structure_block") ||
                        itemLower.equals("jigsaw") ||
                        itemLower.equals("spawner")) {
                        plugin.getLogger().warning("⚠ Skipping impossible item: " + item);
                        continue;
                    }

                    try {
                        org.bukkit.Material material = org.bukkit.Material.getMaterial(item.toUpperCase());
                        if (material == null) {
                            plugin.getLogger().warning("⚠ Skipping invalid item material: " + item);
                            continue;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("⚠ Skipping item due to error: " + item + " - " + e.getMessage());
                        continue;
                    }

                    String taskDescription = "Collect " + count + "x " + item.toUpperCase();
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [InventoryTask] Task for " + playerName + ": " + taskDescription);

                    return new InventoryTask(this).generate(player, Collections.singletonList(item), count);
                }

                case "kill": {
                    int count;
                    if (random.nextDouble() < 0.8) {
                        count = 1 + random.nextInt(16);
                    } else {
                        count = 17 + random.nextInt(16);
                    }
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

                case "tnt": {
                    int count;
                    if (random.nextDouble() < 0.8) {
                        count = 1 + random.nextInt(16);
                    } else {
                        count = 17 + random.nextInt(16);
                    }
                    String mobKey = taskKeys.get(random.nextInt(taskKeys.size()));
                    String mob = taskSection.getString(mobKey);

                    if (mob == null) {
                        plugin.getLogger().severe("❌ Ошибка: mobKey '" + mobKey + "' не найден в 'tnt'!");
                        continue;
                    }

                    String taskDescription = "Kill " + count + " " + mob.toUpperCase() + " with TNT";
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [TNTTask] Task for " + playerName + ": " + taskDescription);

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
                    ConfigurationSection itemSection = enchantSection.getConfigurationSection(itemKey);
                    if (itemSection == null) {
                        continue;
                    }
                    String item = itemSection.getString("name");
                    if (item == null) {
                        item = enchantSection.getString(itemKey);
                    }

                    ConfigurationSection typeSection = itemSection.getConfigurationSection("type");
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

                    if (!areEnchantmentsValid(item, selectedEnchants)) {
                        plugin.getLogger().warning("⚠ Invalid enchantment combination for " + item + ": " + selectedEnchants + ". Retrying...");
                        continue;
                    }

                    String taskDescription = "Enchant " + item.toUpperCase() + " with: " + String.join(", ", selectedEnchants);
                    String playerName = player != null ? player.getName() : "All Speedrunners";
                    plugin.getLogger().info("📌 [EnchantTask] Task for " + playerName + ": " + taskDescription);
                    return new EnchantTask(this).generate(player, item, selectedEnchants);
                }

                default:
                    plugin.getLogger().severe("❌ [ERROR] Неизвестное задание! Категория: " + selectedCategory);
            }
        }
        plugin.getLogger().severe("❌ [ERROR] Не удалось найти подходящее задание после 10 попыток!");
        return new Task("⚠ Unknown Task", p -> false);
    }


    public void enableRandomMode() {
        isRandomModeEnabled = true;
        hideNonRandomBossBar();
    }

    public void disableRandomMode() {
        isRandomModeEnabled = false;
        showNonRandomBossBar();
    }

    public boolean isRandomModeEnabled() {
        return isRandomModeEnabled;
    }

    public void showNonRandomBossBar() {
        if (!GameManager.isGameStarted()) {
            return;
        }
        
        FileConfiguration config = plugin.getConfig();
        String language = config.getString("language", "en");
        String goalText = config.getString(language + ".messages.kill_ender_dragon", "KILL ENDER DRAGON");
        
        nonRandomBossBar.setTitle(ChatColor.RED + goalText);
        nonRandomBossBar.setProgress(1.0);
        nonRandomBossBar.setVisible(true);

        nonRandomBossBar.removeAll();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                nonRandomBossBar.addPlayer(player);
            }
        }

        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter)) {
                Player target = plugin.getCompassManager().getCurrentTarget(hunter);
                if (target != null) {
                    showTaskToHunter(hunter, target);
                }
            }
        }
    }
    
    private void hideNonRandomBossBar() {
        nonRandomBossBar.setVisible(false);
        nonRandomBossBar.removeAll();
    }

    public void showTaskToPlayer(Player player, Task task) {
        if (task == null || player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(playerId);

        long speedrunnerCount = Bukkit.getOnlinePlayers().stream()
            .filter(p -> plugin.getLifeManager().isSpeedrunner(p))
            .count();
        
        if (bossBar == null) {
            List<Player> speedrunners = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getLifeManager().isSpeedrunner(p)) {
                    speedrunners.add(p);
                }
            }
            speedrunners.sort(Comparator.comparing(Player::getName));
            int playerIndex = speedrunners.indexOf(player);
            if (playerIndex < 0) playerIndex = 0;
            
            int colorIndex = playerIndex % BOSS_BAR_COLORS.length;
            bossBar = Bukkit.createBossBar("", BOSS_BAR_COLORS[colorIndex], BarStyle.SOLID);
            playerBossBars.put(playerId, bossBar);
        }

        String taskText = task.getDescription();
        if (speedrunnerCount > 1) {
            taskText = "[" + player.getName() + "] " + taskText;
        }
        
        bossBar.setTitle(ChatColor.YELLOW + taskText);
        bossBar.setProgress(0.0);
        bossBar.setVisible(true);
        bossBar.removePlayer(player);
        bossBar.addPlayer(player);

        updateHunterBossBarsForSpeedrunner(player);
    }

    private void updateHunterBossBarsForSpeedrunner(Player speedrunner) {
        if (!GameManager.isGameStarted()) {
            return;
        }
        
        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter)) {
                Player target = plugin.getCompassManager().getCurrentTarget(hunter);
                if (target != null && target.getUniqueId().equals(speedrunner.getUniqueId())) {
                    showTaskToHunter(hunter, speedrunner);
                }
            }
        }
    }

    public void showTaskToAllPlayers(Task task) {
        if (task == null) {
            plugin.getLogger().warning("⚠ Attempted to show a null task to players!");
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                Task playerTask = playerTasks.get(player.getUniqueId());
                if (playerTask != null) {
                    showTaskToPlayer(player, playerTask);
                }
            }
        }
    }

    public void completeTask(Player player) {
        if (!playerTasks.containsKey(player.getUniqueId())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removePlayer(player);
        }
        
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
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
        playerBossBars.clear();

        for (BossBar bossBar : hunterBossBars.values()) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
        hunterBossBars.clear();
        
        playerTasks.clear();
        playerScenarioIndex.clear();
        playerScenarios.clear();
        hideNonRandomBossBar();
    }

    public void showTaskToHunter(Player hunter, Player speedrunner) {
        if (hunter == null || speedrunner == null || !GameManager.isGameStarted()) {
            return;
        }
        
        UUID hunterId = hunter.getUniqueId();
        UUID speedrunnerId = speedrunner.getUniqueId();

        BossBar hunterBossBar = hunterBossBars.get(hunterId);
        if (hunterBossBar == null) {
            hunterBossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            hunterBossBars.put(hunterId, hunterBossBar);
        }

        hunterBossBar.removePlayer(hunter);
        
        if (isRandomModeEnabled) {
            Task task = playerTasks.get(speedrunnerId);
            if (task != null) {
                BossBar speedrunnerBossBar = playerBossBars.get(speedrunnerId);
                double progress = speedrunnerBossBar != null ? speedrunnerBossBar.getProgress() : 0.0;
                
                String taskText = "[" + speedrunner.getName() + "] " + task.getDescription();
                hunterBossBar.setTitle(ChatColor.BLUE + taskText);
                hunterBossBar.setProgress(progress);
                hunterBossBar.setVisible(true);
                hunterBossBar.addPlayer(hunter);
            } else {
                hunterBossBar.setVisible(false);
            }
        } else {
            FileConfiguration config = plugin.getConfig();
            String language = config.getString("language", "en");
            String goalText = config.getString(language + ".messages.kill_ender_dragon", "KILL ENDER DRAGON");
            hunterBossBar.setTitle(ChatColor.BLUE + "[" + speedrunner.getName() + "] " + goalText);
            hunterBossBar.setProgress(1.0);
            hunterBossBar.setVisible(true);
            hunterBossBar.addPlayer(hunter);
        }
    }

    public void updateHunterBossBars(Player speedrunner) {
        if (!GameManager.isGameStarted()) {
            return;
        }
        
        UUID speedrunnerId = speedrunner.getUniqueId();
        BossBar speedrunnerBossBar = playerBossBars.get(speedrunnerId);
        if (speedrunnerBossBar == null) {
            return;
        }
        
        double progress = speedrunnerBossBar.getProgress();

        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter)) {
                Player target = plugin.getCompassManager().getCurrentTarget(hunter);
                if (target != null && target.getUniqueId().equals(speedrunnerId)) {
                    BossBar hunterBossBar = hunterBossBars.get(hunter.getUniqueId());
                    if (hunterBossBar != null && hunterBossBar.isVisible()) {
                        hunterBossBar.setProgress(progress);
                    }
                }
            }
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin getMainPlugin() {
        return plugin;
    }

    public void updateTaskProgress(Player player, double progress) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar != null) {
            bossBar.setProgress(Math.min(progress, 1.0));
            updateHunterBossBars(player);
        }
    }

    public void reassignBossBar(Player player) {
        if (!GameManager.isGameStarted()) return;

        if (plugin.getLifeManager().isSpeedrunner(player)) {
            if (isRandomModeEnabled) {
                Task task = playerTasks.get(player.getUniqueId());
                if (task != null) {
                    showTaskToPlayer(player, task);
                    player.sendMessage(ChatColor.YELLOW + "📌 Текущее задание: " + task.getDescription());
                }
            } else {
                showNonRandomBossBar();
            }
        }
    }

    public void generateTasksForAllSpeedrunners() {
        if (!isRandomModeEnabled) {
            showNonRandomBossBar();
            return;
        }
        
        hideNonRandomBossBar();
        currentScenario = buildScenario();
        if (currentScenario.isEmpty()) {
            plugin.getLogger().warning("⚠ Сценарий задач пуст – задания не будут сгенерированы.");
            return;
        }

        playerTasks.clear();
        playerScenarioIndex.clear();
        playerScenarios.clear();

        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                UUID id = player.getUniqueId();
                playerScenarioIndex.put(id, 0);
                playerScenarios.put(id, new ArrayList<>(currentScenario));
                String difficulty = currentScenario.get(0);
                Task task = generateRandomTask(player, difficulty);
                playerTasks.put(id, task);
                startTaskChecking(player, task);
                showTaskToPlayer(player, task);
            }
        }

        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter)) {
                Player target = plugin.getCompassManager().getCurrentTarget(hunter);
                if (target != null) {
                    showTaskToHunter(hunter, target);
                }
            }
        }
    }

    public void showTaskToAllSpeedrunners() {
        if (!isRandomModeEnabled) {
            showNonRandomBossBar();
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player)) {
                Task task = playerTasks.get(player.getUniqueId());
                if (task != null) {
                    showTaskToPlayer(player, task);
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
        List<String> scenario = cfg.getStringList("random_tasks.scenario");
        if (scenario != null && !scenario.isEmpty()) {
            return new ArrayList<>(scenario);
        }

        java.util.Random random = new java.util.Random();
        int presetChoice = random.nextInt(9);
        
        scenario = new ArrayList<>();
        switch (presetChoice) {
            case 0:
                scenario.add("hard");
                scenario.add("medium");
                scenario.add("easy");
                break;
            case 1:
                scenario.add("easy");
                scenario.add("easy");
                scenario.add("easy");
                break;
            case 2:
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 3:
                scenario.add("hard");
                scenario.add("hard");
                break;
            case 4:
                scenario.add("easy");
                scenario.add("easy");
                scenario.add("medium");
                break;
            case 5:
                scenario.add("easy");
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 6:
                scenario.add("hard");
                scenario.add("easy");
                scenario.add("easy");
                break;
            case 7:
                scenario.add("medium");
                scenario.add("medium");
                scenario.add("medium");
                break;
            case 8:
                scenario.add("hard");
                scenario.add("hard");
                scenario.add("hard");
                break;
        }
        
        plugin.getLogger().info("Сценарий пуст, выбран случайный пресет: " + scenario);
        return scenario;
    }

    private void advanceScenarioOrFinish(Player player) {
        UUID id = player.getUniqueId();

        List<String> playerScenario = playerScenarios.getOrDefault(id, currentScenario);
        if (playerScenario.isEmpty()) {
            playerScenario = new ArrayList<>(currentScenario);
            playerScenarios.put(id, playerScenario);
        }

        if (!playerScenarioIndex.containsKey(id) || playerScenario.isEmpty()) {
            playerTasks.remove(id);
            playerScenarioIndex.remove(id);
            playerScenarios.remove(id);
            BossBar bossBar = playerBossBars.remove(id);
            if (bossBar != null) {
                bossBar.removeAll();
            }
        } else {
            int idx = playerScenarioIndex.get(id) + 1;
            if (idx < playerScenario.size()) {
                playerScenarioIndex.put(id, idx);
                String difficulty = playerScenario.get(idx);
                Task nextTask = generateRandomTask(player, difficulty);
                playerTasks.put(id, nextTask);
                startTaskChecking(player, nextTask);
                showTaskToPlayer(player, nextTask);
                player.sendMessage(ChatColor.AQUA + "📌 Новое задание: " + nextTask.getDescription());
                updateHunterBossBarsForSpeedrunner(player);
                return;
            } else {
                playerScenarioIndex.remove(id);
                playerScenarios.remove(id);
                playerTasks.remove(id);
                BossBar bossBar = playerBossBars.remove(id);
                if (bossBar != null) {
                    bossBar.removeAll();
                }
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

    public void transferTasksFromDeadSpeedrunner(Player deadSpeedrunner) {
        if (!isRandomModeEnabled || !GameManager.isGameStarted()) {
            return;
        }
        
        UUID deadId = deadSpeedrunner.getUniqueId();
        Task deadTask = playerTasks.get(deadId);
        Integer deadScenarioIndex = playerScenarioIndex.get(deadId);
        
        if (deadTask == null && deadScenarioIndex == null) {
            return;
        }

        List<Player> aliveSpeedrunners = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isSpeedrunner(player) && 
                player.getGameMode() != GameMode.SPECTATOR &&
                !player.getUniqueId().equals(deadId)) {
                aliveSpeedrunners.add(player);
            }
        }
        
        if (aliveSpeedrunners.isEmpty()) {
            playerTasks.remove(deadId);
            playerScenarioIndex.remove(deadId);
            BossBar bossBar = playerBossBars.remove(deadId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            return;
        }

        int remainingTasks = 0;
        if (deadScenarioIndex != null && !currentScenario.isEmpty()) {
            remainingTasks = currentScenario.size() - deadScenarioIndex;
        } else if (deadTask != null) {
            remainingTasks = 1;
        }
        
        if (remainingTasks <= 0) {
            playerTasks.remove(deadId);
            playerScenarioIndex.remove(deadId);
            BossBar bossBar = playerBossBars.remove(deadId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            return;
        }
        
        Random random = new Random();
        int tasksToTransfer = random.nextInt(remainingTasks + 1); 
        
        if (tasksToTransfer == 0) {
            playerTasks.remove(deadId);
            playerScenarioIndex.remove(deadId);
            BossBar bossBar = playerBossBars.remove(deadId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            return;
        }

        for (int i = 0; i < tasksToTransfer; i++) {
            if (aliveSpeedrunners.isEmpty()) break;
            
            Player recipient = aliveSpeedrunners.get(random.nextInt(aliveSpeedrunners.size()));
            UUID recipientId = recipient.getUniqueId();

            int deadTaskIndex = deadScenarioIndex != null ? deadScenarioIndex + i : 0;
            if (deadTaskIndex >= currentScenario.size()) {
                deadTaskIndex = currentScenario.size() - 1;
            }
            
            String difficulty = currentScenario.get(deadTaskIndex);
            Task newTask = generateRandomTask(recipient, difficulty);

            List<String> recipientScenario = playerScenarios.getOrDefault(recipientId, new ArrayList<>(currentScenario));
            if (recipientScenario.isEmpty()) {
                recipientScenario = new ArrayList<>(currentScenario);
            }

            if (!playerTasks.containsKey(recipientId)) {
                playerTasks.put(recipientId, newTask);
                playerScenarioIndex.put(recipientId, 0);
                playerScenarios.put(recipientId, new ArrayList<>(Collections.singletonList(difficulty)));
                startTaskChecking(recipient, newTask);
                showTaskToPlayer(recipient, newTask);
                recipient.sendMessage(ChatColor.AQUA + "📌 Получено задание от " + deadSpeedrunner.getName() + ": " + newTask.getDescription());
            } else {
                recipientScenario.add(difficulty);
                playerScenarios.put(recipientId, recipientScenario);
                recipient.sendMessage(ChatColor.AQUA + "📌 Получено дополнительное задание от " + deadSpeedrunner.getName() + ": " + newTask.getDescription() + " (будет после текущего)");
            }
        }

        playerTasks.remove(deadId);
        playerScenarioIndex.remove(deadId);
        BossBar bossBar = playerBossBars.remove(deadId);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (plugin.getLifeManager().isHunter(hunter)) {
                Player target = plugin.getCompassManager().getCurrentTarget(hunter);
                if (target != null && !target.getUniqueId().equals(deadId)) {
                    showTaskToHunter(hunter, target);
                }
            }
        }
    }

}
