package me.example.huntervsspeedrunner.menus;

import me.example.huntervsspeedrunner.HunterVSSpeedrunnerPlugin;
import me.example.huntervsspeedrunner.utils.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SetConfig {

    private final HunterVSSpeedrunnerPlugin plugin;
    private String currentSetting = null;
    private final java.util.Map<java.util.UUID, PendingAdd> pendingAdds = new java.util.HashMap<>();

    public SetConfig(HunterVSSpeedrunnerPlugin plugin) {
        this.plugin = plugin;
    }

    public void openConfigMenu(Player player) {
        Inventory configMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + getLocalizedMessage("advanced_settings_title"));
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");

        String langDisplay = ChatColor.AQUA + getLocalizedMessage("language") + ": " + ChatColor.YELLOW + lang;
        configMenu.setItem(9, createItem(Material.WRITABLE_BOOK, langDisplay));

        boolean giveCompass = config.getBoolean("hunter.giveCompassOnDeath", true);
        String compassToggle = ChatColor.YELLOW + getLocalizedMessage("give_compass_on_death") + ": " + (giveCompass ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌");
        configMenu.setItem(11, createItem(Material.RESPAWN_ANCHOR, compassToggle));

        int delay = config.getInt("hunter.teleportDelay", 30);
        String delayDisplay = ChatColor.LIGHT_PURPLE + getLocalizedMessage("teleport_delay") + ": " + ChatColor.YELLOW + delay + " " + getLocalizedMessage("seconds");
        configMenu.setItem(12, createItem(Material.ENDER_PEARL, delayDisplay));

        int compassTime = config.getInt("hunter.compassgive", 120);
        String compassDisplay = ChatColor.GOLD + getLocalizedMessage("compass_time") + ": " + ChatColor.YELLOW + compassTime + " " + getLocalizedMessage("seconds");
        configMenu.setItem(14, createItem(Material.CLOCK, compassDisplay));

        // Книга настройки рандомных заданий
        String randomTasksDisplay = ChatColor.LIGHT_PURPLE + getLocalizedMessage("random_tasks_settings");
        configMenu.setItem(16, createItem(Material.ENCHANTED_BOOK, randomTasksDisplay));

        configMenu.setItem(15, createItem(Material.TNT, ChatColor.GOLD + "🚨 " + getLocalizedMessage("start_game_now")));
        configMenu.setItem(17, createItem(Material.ANVIL, ChatColor.RED + getLocalizedMessage("reload_plugin")));
        configMenu.setItem(22, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));
        if (currentSetting != null) {
            configMenu.setItem(2, createItem(Material.GREEN_CONCRETE, ChatColor.GREEN + "+10 " + getLocalizedMessage("seconds")));
            configMenu.setItem(3, createItem(Material.GREEN_WOOL, ChatColor.GREEN + "+1 " + getLocalizedMessage("second")));
            configMenu.setItem(4, createItem(Material.LIME_DYE, ChatColor.GREEN + getLocalizedMessage("done")));
            configMenu.setItem(5, createItem(Material.RED_WOOL, ChatColor.RED + "-1 " + getLocalizedMessage("second")));
            configMenu.setItem(6, createItem(Material.RED_CONCRETE, ChatColor.RED + "-10 " + getLocalizedMessage("seconds")));
        }
        player.openInventory(configMenu);
    }


    public void openDetailedDifficultyMenu(Player player) {
        Inventory difficultyMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + getLocalizedMessage("random_tasks_settings"));
        FileConfiguration config = plugin.getConfig();

        // фон
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < 27; i++) difficultyMenu.setItem(i, filler);

        // Включение/выключение рандомных заданий
        boolean randomTasksEnabled = config.getBoolean("random_tasks.enabled", true);
        String randomTasksStatus = ChatColor.AQUA + "Random Tasks: " + (randomTasksEnabled ? ChatColor.GREEN + " ✅" : ChatColor.RED + " ❌");
        difficultyMenu.setItem(4, createItem(Material.LEVER, randomTasksStatus));

        // Слот 13: Бумажка со сценарием задач
        java.util.List<String> scenario = config.getStringList("random_tasks.scenario");
        java.util.List<String> lore = new java.util.ArrayList<>();
        if (scenario == null || scenario.isEmpty()) {
            lore.add(ChatColor.GRAY + getLocalizedMessage("scenario_empty"));
        } else {
            lore.add(ChatColor.GRAY + getLocalizedMessage("scenario_pieces"));
            int idx = 1;
            for (String s : scenario) {
                String nice = s.equalsIgnoreCase("easy") ? ChatColor.GREEN + getLocalizedMessage("easy")
                        : s.equalsIgnoreCase("medium") ? ChatColor.GOLD + getLocalizedMessage("medium")
                        : s.equalsIgnoreCase("hard") ? ChatColor.RED + getLocalizedMessage("hard")
                        : ChatColor.YELLOW + s;
                lore.add(ChatColor.DARK_GRAY + "" + idx++ + ") " + nice);
            }
        }
        ItemStack summaryItem = new ItemStack(Material.PAPER);
        ItemMeta summaryMeta = summaryItem.getItemMeta();
        if (summaryMeta != null) {
            summaryMeta.setDisplayName(ChatColor.AQUA + getLocalizedMessage("task_scenario_summary"));
            summaryMeta.setLore(lore);
            summaryItem.setItemMeta(summaryMeta);
        }
        difficultyMenu.setItem(13, summaryItem);

        // Слоты 9, 10, 11: Кнопки добавления в список задач (легкое, среднее, сложное)
        difficultyMenu.setItem(9, createItem(Material.LIME_DYE, ChatColor.GREEN + getLocalizedMessage("add_easy_to_scenario")));
        difficultyMenu.setItem(10, createItem(Material.GOLD_INGOT, ChatColor.GOLD + getLocalizedMessage("add_medium_to_scenario")));
        difficultyMenu.setItem(11, createItem(Material.REDSTONE_BLOCK, ChatColor.RED + getLocalizedMessage("add_hard_to_scenario")));

        // Слоты 15, 16, 17: Кнопки просмотра заданий (легкие, средние, сложные)
        difficultyMenu.setItem(15, createItem(Material.LIME_CONCRETE, ChatColor.GREEN + "★ " + getLocalizedMessage("easy_tasks")));
        difficultyMenu.setItem(16, createItem(Material.YELLOW_CONCRETE, ChatColor.GOLD + "★★ " + getLocalizedMessage("medium_tasks")));
        difficultyMenu.setItem(17, createItem(Material.RED_CONCRETE, ChatColor.RED + "★★★ " + getLocalizedMessage("hard_tasks")));

        // Дополнительные кнопки управления сценарием
        difficultyMenu.setItem(18, createItem(Material.NETHER_STAR, ChatColor.AQUA + getLocalizedMessage("scenario_presets")));
        difficultyMenu.setItem(19, createItem(Material.PAPER, ChatColor.YELLOW + getLocalizedMessage("remove_last_from_scenario")));
        difficultyMenu.setItem(20, createItem(Material.BARREL, ChatColor.DARK_RED + getLocalizedMessage("clear_scenario")));

        // Кнопка назад
        difficultyMenu.setItem(22, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));

        player.openInventory(difficultyMenu);
    }


    private ItemStack createItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }
    public String getMenuTitle() {
        return ChatColor.DARK_GREEN + getLocalizedMessage("advanced_settings_title");
    }
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 27) {
            return;
        }

        event.setCancelled(true);
        boolean shouldReopenMenu = true;

        switch (slot) {
            case 9: {
                String current = config.getString("language", "en");
                String next = current.equals("en") ? "ru" : "en";
                config.set("language", next);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + getLocalizedMessage("change_language") + next);
                break;
            }
            case 11:
                toggleBooleanConfig(player, "hunter.giveCompassOnDeath", "give_compass");
                break;
            case 12:
                currentSetting = "teleportDelay";
                break;
            case 14:
                currentSetting = "compassgive";
                break;
            case 16:
                // Открываем меню настроек рандомных заданий
                player.closeInventory();
                openDetailedDifficultyMenu(player);
                shouldReopenMenu = false;
                break;
            case 15:
                player.sendMessage(ChatColor.GOLD + getLocalizedMessage("run_game_fast"));
                GameManager.startGame(plugin);
                shouldReopenMenu = false;
                break;
            case 17:
                player.closeInventory();
                plugin.reloadPlugin();
                player.sendMessage(ChatColor.GREEN + getLocalizedMessage("plugin_reloaded"));
                shouldReopenMenu = false;
                break;
            case 2:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, 10);
                }
                break;
            case 3:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, 1);
                }
                break;
            case 5:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, -1);
                }
                break;
            case 6:
                if (currentSetting != null) {
                    adjustSetting(player, config, currentSetting, -10);
                }
                break;
            case 4:
                currentSetting = null;
                break;
            case 22:
                player.closeInventory();
                shouldReopenMenu = false;
                player.chat("/hunter");
                break;
            default:
                break;
        }

        if (shouldReopenMenu) {
            openConfigMenu(player);
        }
    }


    public void handleDetailedDifficultyClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = plugin.getConfig();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 27) {
            return;
        }

        event.setCancelled(true);
        boolean shouldReopenMenu = true;

        switch (slot) {
            case 4: // Включение/выключение рандомных заданий
                boolean currentEnabled = config.getBoolean("random_tasks.enabled", true);
                boolean newEnabled = !currentEnabled;
                config.set("random_tasks.enabled", newEnabled);
                plugin.saveConfig();
                
                if (newEnabled) {
                    plugin.getRandomTaskManager().enableRandomMode();
                    player.sendMessage(ChatColor.GREEN + getLocalizedMessage("random_mode_enabled"));
                } else {
                    plugin.getRandomTaskManager().disableRandomMode();
                    player.sendMessage(ChatColor.RED + getLocalizedMessage("random_mode_disabled"));
                }
                break;
            case 9: { // Добавить лёгкое в сценарий
                java.util.List<String> scenario = new java.util.ArrayList<>(config.getStringList("random_tasks.scenario"));
                scenario.add("easy");
                config.set("random_tasks.scenario", scenario);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + getLocalizedMessage("easy_added"));
                break;
            }
            case 10: { // Добавить среднее в сценарий
                java.util.List<String> scenario = new java.util.ArrayList<>(config.getStringList("random_tasks.scenario"));
                scenario.add("medium");
                config.set("random_tasks.scenario", scenario);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GOLD + getLocalizedMessage("medium_added"));
                break;
            }
            case 11: { // Добавить сложное в сценарий
                java.util.List<String> scenario = new java.util.ArrayList<>(config.getStringList("random_tasks.scenario"));
                scenario.add("hard");
                config.set("random_tasks.scenario", scenario);
                plugin.saveConfig();
                player.sendMessage(ChatColor.RED + getLocalizedMessage("hard_added"));
                break;
            }
            case 15: // Меню лёгких задач
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openCategoryMenu(player, "easy");
                }, 1L);
                shouldReopenMenu = false;
                break;
            case 16: // Меню средних задач
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openCategoryMenu(player, "medium");
                }, 1L);
                shouldReopenMenu = false;
                break;
            case 17: // Меню сложных задач
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openCategoryMenu(player, "hard");
                }, 1L);
                shouldReopenMenu = false;
                break;
            case 22: // Назад
                player.closeInventory();
                openConfigMenu(player);
                shouldReopenMenu = false;
                break;
            case 2: // Кнопки настройки количества
                if (currentSetting != null && currentSetting.equals("taskCount")) {
                    adjustSetting(player, config, currentSetting, 10);
                }
                break;
            case 3:
                if (currentSetting != null && currentSetting.equals("taskCount")) {
                    adjustSetting(player, config, currentSetting, 1);
                }
                break;
            case 18:
                // ★ Пресеты
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openScenarioPresetsMenu(player);
                }, 1L);
                shouldReopenMenu = false;
                break;
            case 19:
                // ↩ Убрать последний кусочек
                java.util.List<String> scenario = new java.util.ArrayList<>(config.getStringList("random_tasks.scenario"));
                if (!scenario.isEmpty()) {
                    scenario.remove(scenario.size() - 1);
                    config.set("random_tasks.scenario", scenario);
                    plugin.saveConfig();
                    player.sendMessage(ChatColor.YELLOW + getLocalizedMessage("last_piece_removed"));
                } else {
                    player.sendMessage(ChatColor.RED + getLocalizedMessage("scenario_already_empty"));
                }
                break;
            case 20:
                // ✖ Очистить шаблон
                config.set("random_tasks.scenario", new java.util.ArrayList<>());
                plugin.saveConfig();
                player.sendMessage(ChatColor.DARK_RED + getLocalizedMessage("scenario_cleared"));
                break;
            default:
                break;
        }

        if (shouldReopenMenu) {
            openDetailedDifficultyMenu(player);
        }
    }

    public void openScenarioPresetsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + getLocalizedMessage("scenario_presets_title"));

        ItemStack filler = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        // Пресет 1: 1H + 1M + 1E
        ItemStack preset1 = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta1 = preset1.getItemMeta();
        if (meta1 != null) {
            meta1.setDisplayName(ChatColor.RED + getLocalizedMessage("preset_1h1m1e"));
            java.util.List<String> lore1 = new java.util.ArrayList<>();
            lore1.add(ChatColor.GRAY + "1 " + ChatColor.RED + getLocalizedMessage("hard") + ChatColor.GRAY + " + 1 " + ChatColor.GOLD + getLocalizedMessage("medium") + ChatColor.GRAY + " + 1 " + ChatColor.GREEN + getLocalizedMessage("easy"));
            meta1.setLore(lore1);
            preset1.setItemMeta(meta1);
        }
        inv.setItem(9, preset1);

        // Пресет 2: 3E
        ItemStack preset2 = new ItemStack(Material.LIME_DYE);
        ItemMeta meta2 = preset2.getItemMeta();
        if (meta2 != null) {
            meta2.setDisplayName(ChatColor.GREEN + getLocalizedMessage("preset_3e"));
            java.util.List<String> lore2 = new java.util.ArrayList<>();
            lore2.add(ChatColor.GRAY + "3x " + ChatColor.GREEN + getLocalizedMessage("easy"));
            meta2.setLore(lore2);
            preset2.setItemMeta(meta2);
        }
        inv.setItem(10, preset2);

        // Пресет 3: 2M
        ItemStack preset3 = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta3 = preset3.getItemMeta();
        if (meta3 != null) {
            meta3.setDisplayName(ChatColor.GOLD + getLocalizedMessage("preset_2m"));
            java.util.List<String> lore3 = new java.util.ArrayList<>();
            lore3.add(ChatColor.GRAY + "2x " + ChatColor.GOLD + getLocalizedMessage("medium"));
            meta3.setLore(lore3);
            preset3.setItemMeta(meta3);
        }
        inv.setItem(11, preset3);

        // Пресет 4: 2H
        ItemStack preset4 = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta4 = preset4.getItemMeta();
        if (meta4 != null) {
            meta4.setDisplayName(ChatColor.DARK_RED + getLocalizedMessage("preset_2h"));
            java.util.List<String> lore4 = new java.util.ArrayList<>();
            lore4.add(ChatColor.GRAY + "2x " + ChatColor.RED + getLocalizedMessage("hard"));
            meta4.setLore(lore4);
            preset4.setItemMeta(meta4);
        }
        inv.setItem(12, preset4);

        // Пресет 5: 2E + 1M
        ItemStack preset5 = new ItemStack(Material.PAPER);
        ItemMeta meta5 = preset5.getItemMeta();
        if (meta5 != null) {
            meta5.setDisplayName(ChatColor.YELLOW + getLocalizedMessage("preset_2e1m"));
            java.util.List<String> lore5 = new java.util.ArrayList<>();
            lore5.add(ChatColor.GRAY + "2x " + ChatColor.GREEN + getLocalizedMessage("easy") + ChatColor.GRAY + " + 1 " + ChatColor.GOLD + getLocalizedMessage("medium"));
            meta5.setLore(lore5);
            preset5.setItemMeta(meta5);
        }
        inv.setItem(13, preset5);

        // Пресет 6: 1E + 2M
        ItemStack preset6 = new ItemStack(Material.EMERALD);
        ItemMeta meta6 = preset6.getItemMeta();
        if (meta6 != null) {
            meta6.setDisplayName(ChatColor.GREEN + getLocalizedMessage("preset_1e2m"));
            java.util.List<String> lore6 = new java.util.ArrayList<>();
            lore6.add(ChatColor.GRAY + "1 " + ChatColor.GREEN + getLocalizedMessage("easy") + ChatColor.GRAY + " + 2x " + ChatColor.GOLD + getLocalizedMessage("medium"));
            meta6.setLore(lore6);
            preset6.setItemMeta(meta6);
        }
        inv.setItem(14, preset6);

        // Пресет 7: 1H + 2E
        ItemStack preset7 = new ItemStack(Material.DIAMOND);
        ItemMeta meta7 = preset7.getItemMeta();
        if (meta7 != null) {
            meta7.setDisplayName(ChatColor.AQUA + getLocalizedMessage("preset_1h2e"));
            java.util.List<String> lore7 = new java.util.ArrayList<>();
            lore7.add(ChatColor.GRAY + "1 " + ChatColor.RED + getLocalizedMessage("hard") + ChatColor.GRAY + " + 2x " + ChatColor.GREEN + getLocalizedMessage("easy"));
            meta7.setLore(lore7);
            preset7.setItemMeta(meta7);
        }
        inv.setItem(15, preset7);

        // Пресет 8: 3M
        ItemStack preset8 = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta8 = preset8.getItemMeta();
        if (meta8 != null) {
            meta8.setDisplayName(ChatColor.GOLD + getLocalizedMessage("preset_3m"));
            java.util.List<String> lore8 = new java.util.ArrayList<>();
            lore8.add(ChatColor.GRAY + "3x " + ChatColor.GOLD + getLocalizedMessage("medium"));
            meta8.setLore(lore8);
            preset8.setItemMeta(meta8);
        }
        inv.setItem(16, preset8);

        // Пресет 9: 3H
        ItemStack preset9 = new ItemStack(Material.BEACON);
        ItemMeta meta9 = preset9.getItemMeta();
        if (meta9 != null) {
            meta9.setDisplayName(ChatColor.DARK_PURPLE + getLocalizedMessage("preset_3h"));
            java.util.List<String> lore9 = new java.util.ArrayList<>();
            lore9.add(ChatColor.GRAY + "3x " + ChatColor.RED + getLocalizedMessage("hard"));
            meta9.setLore(lore9);
            preset9.setItemMeta(meta9);
        }
        inv.setItem(17, preset9);

        inv.setItem(22, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));
        player.openInventory(inv);
    }

    public void handleScenarioPresetsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        event.setCancelled(true);

        FileConfiguration config = plugin.getConfig();
        java.util.List<String> scenario = new java.util.ArrayList<>();
        switch (slot) {
            case 9: // 1H+1M+1E
                scenario.add("hard"); scenario.add("medium"); scenario.add("easy");
                break;
            case 10: // 3E
                scenario.add("easy"); scenario.add("easy"); scenario.add("easy");
                break;
            case 11: // 2M
                scenario.add("medium"); scenario.add("medium");
                break;
            case 12: // 2H
                scenario.add("hard"); scenario.add("hard");
                break;
            case 13: // 2E+1M
                scenario.add("easy"); scenario.add("easy"); scenario.add("medium");
                break;
            case 14: // 1E+2M
                scenario.add("easy"); scenario.add("medium"); scenario.add("medium");
                break;
            case 15: // 1H+2E
                scenario.add("hard"); scenario.add("easy"); scenario.add("easy");
                break;
            case 16: // 3M
                scenario.add("medium"); scenario.add("medium"); scenario.add("medium");
                break;
            case 17: // 3H
                scenario.add("hard"); scenario.add("hard"); scenario.add("hard");
                break;
            case 22:
                player.closeInventory();
                openDetailedDifficultyMenu(player);
                return;
            default:
                return;
        }

        config.set("random_tasks.scenario", scenario);
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + getLocalizedMessage("preset_applied"));
        player.closeInventory();
        openDetailedDifficultyMenu(player);
    }

    public void openCategoryMenu(Player player, String difficulty) {
        String title = ChatColor.DARK_AQUA + getLocalizedMessage("tasks_category_title") + " • " + prettyDifficulty(difficulty);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // украшаем стеклом
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.CHEST, ChatColor.GOLD + getLocalizedMessage("category_inventory") + " (" + taskCount(difficulty, "inventory") + ")"));
        inv.setItem(11, createItem(Material.IRON_SWORD, ChatColor.RED + getLocalizedMessage("category_kill") + " (" + taskCount(difficulty, "kill") + ")"));
        inv.setItem(12, createItem(Material.TNT, ChatColor.YELLOW + getLocalizedMessage("category_tnt") + " (" + taskCount(difficulty, "tnt") + ")"));
        inv.setItem(14, createItem(Material.ENCHANTING_TABLE, ChatColor.LIGHT_PURPLE + getLocalizedMessage("category_enchant") + " (" + taskCount(difficulty, "enchant") + ")"));
        inv.setItem(15, createItem(Material.BREWING_STAND, ChatColor.AQUA + getLocalizedMessage("category_effect") + " (" + taskCount(difficulty, "effect") + ")"));
        inv.setItem(16, createItem(Material.BOOK, ChatColor.GREEN + getLocalizedMessage("category_achievement") + " (" + taskCount(difficulty, "achievement") + ")"));

        inv.setItem(22, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));
        player.openInventory(inv);
    }

    public void handleCategoryMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        String title = event.getView().getTitle();
        String difficulty = parseDifficultyFromTitle(title);
        if (difficulty == null) {
            plugin.getLogger().warning("Failed to parse difficulty from title: " + title);
            return;
        }

        event.setCancelled(true);
        switch (slot) {
            case 10:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "inventory");
                }, 1L);
                break;
            case 11:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "kill");
                }, 1L);
                break;
            case 12:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "tnt");
                }, 1L);
                break;
            case 14:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "enchant");
                }, 1L);
                break;
            case 15:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "effect");
                }, 1L);
                break;
            case 16:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openTaskListMenu(player, difficulty, "achievement");
                }, 1L);
                break;
            case 22:
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openDetailedDifficultyMenu(player);
                }, 1L);
                break;
            default:
                break;
        }
    }

    private void openTaskListMenu(Player player, String difficulty, String category) {
        String title = ChatColor.DARK_BLUE + getLocalizedMessage("task_list_title", category) + " • " + prettyDifficulty(difficulty);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // фон
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); filler.setItemMeta(fm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        FileConfiguration cfg = plugin.getRandomTaskManager().getTaskConfig();
        ConfigurationSection diff = cfg.getConfigurationSection(difficulty);
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (diff != null) {
            if ("enchant".equals(category)) {
                ConfigurationSection enchRoot = diff.getConfigurationSection("enchant");
                if (enchRoot != null) {
                    ConfigurationSection itemSec = enchRoot.getConfigurationSection("item");
                    if (itemSec != null) {
                        keys.addAll(itemSec.getKeys(false));
                        java.util.Collections.sort(keys);
                        int[] slots = {10,11,12,13,14,15,16,19,20,21,23,24,25};
                        int idx = 0;
                        for (String k : keys) {
                            if (idx >= slots.length) break;
                            ConfigurationSection s = itemSec.getConfigurationSection(k);
                            if (s == null) continue;
                            String name = s.getString("name", "item");
                            ConfigurationSection typeSec = s.getConfigurationSection("type");
                            java.util.List<String> enchList = new java.util.ArrayList<>();
                            if (typeSec != null) {
                                java.util.List<String> tKeys = new java.util.ArrayList<>(typeSec.getKeys(false));
                                java.util.Collections.sort(tKeys);
                                for (String tk : tKeys) {
                                    enchList.add(typeSec.getString(tk, ""));
                                }
                            }
                            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
                            ItemMeta meta = icon.getItemMeta();
                            if (meta != null) {
                                meta.setDisplayName(ChatColor.LIGHT_PURPLE + getLocalizedMessage("item_enchant_display", name.toUpperCase()));
                                java.util.List<String> lore = new java.util.ArrayList<>();
                                lore.add(ChatColor.GRAY + getLocalizedMessage("enchants_label", String.join(", ", enchList)));
                                meta.setLore(lore);
                                icon.setItemMeta(meta);
                            }
                            inv.setItem(slots[idx++], icon);
                        }
                    }
                }
            } else {
                ConfigurationSection cat = diff.getConfigurationSection(category);
                if (cat != null) {
                    keys.addAll(cat.getKeys(false));
                    java.util.Collections.sort(keys);
                    int[] slots = {10,11,12,13,14,15,16,19,20,21,23,24,25};
                    int idx = 0;
                    for (String k : keys) {
                        if (idx >= slots.length) break;
                        String val = cat.getString(k, "");
                        ItemStack icon = createTaskIcon(category, val);
                        inv.setItem(slots[idx++], icon);
                    }
                }
            }
        }

        // кнопка добавления и назад
        inv.setItem(18, createItem(Material.BARRIER, ChatColor.RED + getLocalizedMessage("back")));
        inv.setItem(26, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + getLocalizedMessage("add_task_button")));

        player.openInventory(inv);
    }

    private ItemStack createTaskIcon(String category, String value) {
        Material mat;
        String display;
        java.util.List<String> lore = new java.util.ArrayList<>();

        switch (category) {
            case "kill":
            case "tnt":
                mat = Material.IRON_SWORD;
                display = ChatColor.RED + getLocalizedMessage("kill_task_display", value.toUpperCase());
                break;
            case "inventory":
                mat = Material.matchMaterial(value.toUpperCase());
                if (mat == null) mat = Material.CHEST;
                display = ChatColor.GOLD + getLocalizedMessage("collect_task_display", value.toUpperCase());
                break;
            case "effect":
                mat = Material.BREWING_STAND;
                display = ChatColor.AQUA + getLocalizedMessage("effect_task_display", value.toUpperCase());
                break;
            case "achievement":
                mat = Material.BOOK;
                display = ChatColor.GREEN + getLocalizedMessage("achievement_task_display", value);
                break;
            default:
                mat = Material.PAPER;
                display = ChatColor.YELLOW + value;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(display);
            lore.add(ChatColor.DARK_GRAY + "task.yml: " + value.toLowerCase());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int taskCount(String difficulty, String category) {
        FileConfiguration cfg = plugin.getRandomTaskManager().getTaskConfig();
        ConfigurationSection diff = cfg.getConfigurationSection(difficulty);
        if (diff == null) return 0;
        if ("enchant".equals(category)) {
            ConfigurationSection ench = diff.getConfigurationSection("enchant.item");
            return ench == null ? 0 : ench.getKeys(false).size();
        }
        ConfigurationSection cat = diff.getConfigurationSection(category);
        return cat == null ? 0 : cat.getKeys(false).size();
    }

    private String parseDifficultyFromTitle(String title) {
        if (title == null) return null;
        String stripped = ChatColor.stripColor(title);
        // Format: "Задачи {category} • {difficulty}" or "Tasks {category} • {difficulty}"
        // Check if title contains "•" separator
        if (stripped.contains("•")) {
            String[] parts = stripped.split("•", 2);
            if (parts.length == 2) {
                String tail = parts[1].trim();
                // Check both localized and hardcoded difficulty names
                String easyLocalized = getLocalizedMessage("easy");
                String mediumLocalized = getLocalizedMessage("medium");
                String hardLocalized = getLocalizedMessage("hard");
                if (tail.contains(easyLocalized) || tail.contains("Лёгкая") || tail.contains("Легкая") || tail.contains("Easy")) return "easy";
                if (tail.contains(mediumLocalized) || tail.contains("Средняя") || tail.contains("Medium")) return "medium";
                if (tail.contains(hardLocalized) || tail.contains("Сложная") || tail.contains("Hard")) return "hard";
            }
        }
        return null;
    }

    private String prettyDifficulty(String diff) {
        String localized;
        switch (diff) {
            case "easy": 
                localized = getLocalizedMessage("easy");
                return ChatColor.GREEN + localized;
            case "medium": 
                localized = getLocalizedMessage("medium");
                return ChatColor.GOLD + localized;
            case "hard": 
                localized = getLocalizedMessage("hard");
                return ChatColor.RED + localized;
            default: return diff;
        }
    }

    public void handleTaskListClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        String title = event.getView().getTitle();
        event.setCancelled(true);

        String difficulty = parseDifficultyFromTitle(title);
        String category = parseCategoryFromTitle(title);
        if (difficulty == null || category == null) {
            plugin.getLogger().warning("Failed to parse difficulty or category from title: " + title);
            return;
        }

        if (slot == 18) {
            player.closeInventory();
            // Small delay to ensure inventory closes before opening new one
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openCategoryMenu(player, difficulty);
            }, 1L);
            return;
        }
        if (slot == 26) {
            player.closeInventory();
            // Small delay to ensure inventory closes before opening anvil
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openAddAnvil(player, difficulty, category, 0, null);
            }, 1L);
        }
    }

    private String parseCategoryFromTitle(String title) {
        if (title == null) return null;
        String stripped = ChatColor.stripColor(title);
        // Format: "Задачи {category} • {difficulty}" or "Tasks {category} • {difficulty}"
        // Check if title starts with "Задачи " or "Tasks "
        if (stripped.startsWith("Задачи ") || stripped.startsWith("Tasks ")) {
            // Split by "•" first to separate category from difficulty
            String[] mainParts = stripped.split("•", 2);
            if (mainParts.length > 0) {
                String categoryPart = mainParts[0].trim();
                // Now split category part by spaces
                String[] parts = categoryPart.split(" ");
                if (parts.length >= 2) {
                    return parts[1]; // english category (second word after "Задачи" or "Tasks")
                }
            }
        }
        return null;
    }

    private void openAddAnvil(Player player, String difficulty, String category, int step, String cache) {
        String stepLabel = step == 0 ? "1" : "2";
        String title = "Добавить " + category + " • " + stepLabel;
        Inventory anvil = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL, title);
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            if ("enchant".equals(category) && step == 1) {
                meta.setDisplayName("sharpness,unbreaking");
            } else {
                meta.setDisplayName(category.equals("kill") ? "zombie" :
                        category.equals("inventory") ? "diamond_block" :
                        category.equals("tnt") ? "creeper" :
                        category.equals("effect") ? "speed" :
                        category.equals("achievement") ? "story/mine_diamond" :
                        "value");
            }
            paper.setItemMeta(meta);
        }
        anvil.setItem(0, paper);
        pendingAdds.put(player.getUniqueId(), new PendingAdd(difficulty, category, step, cache));
        player.openInventory(anvil);
    }

    public void handleAnvilClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != org.bukkit.event.inventory.InventoryType.ANVIL) return;
        Player player = (Player) event.getWhoClicked();
        PendingAdd pending = pendingAdds.get(player.getUniqueId());
        if (pending == null) return;

        if (event.getRawSlot() != 2) return; // result slot
        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;
        String input = result.getItemMeta().getDisplayName().trim();
        if (input.isEmpty()) return;

        event.setCancelled(true);

        try {
            if ("enchant".equals(pending.category)) {
                if (pending.step == 0) {
                    // получили название предмета
                    openAddAnvil(player, pending.difficulty, pending.category, 1, input.toLowerCase());
                    return;
                } else {
                    java.util.List<String> enchants = new java.util.ArrayList<>();
                    for (String part : input.split(",")) {
                        String val = part.trim().toLowerCase();
                        if (!val.isEmpty()) enchants.add(val);
                    }
                    plugin.getRandomTaskManager().addEnchantTask(pending.difficulty, pending.cache, enchants);
                    plugin.getRandomTaskManager().reloadTaskConfig();
                    player.sendMessage(ChatColor.GREEN + String.format(getLocalizedMessage("enchant_task_added"), pending.cache, String.join(", ", enchants)));
                }
            } else {
                plugin.getRandomTaskManager().addSimpleTask(pending.difficulty, pending.category, input.toLowerCase());
                plugin.getRandomTaskManager().reloadTaskConfig();
                player.sendMessage(ChatColor.GREEN + String.format(getLocalizedMessage("task_added"), pending.category, input));
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + String.format(getLocalizedMessage("save_error"), e.getMessage()));
            plugin.getLogger().severe("Failed to add task via anvil: " + e.getMessage());
        } finally {
            pendingAdds.remove(player.getUniqueId());
            player.closeInventory();
            openTaskListMenu(player, pending.difficulty, pending.category);
        }
    }

    private static class PendingAdd {
        final String difficulty;
        final String category;
        final int step;
        final String cache;

        PendingAdd(String difficulty, String category, int step, String cache) {
            this.difficulty = difficulty;
            this.category = category;
            this.step = step;
            this.cache = cache;
        }
    }

    private void adjustSetting(Player player, FileConfiguration config, String setting, int change) {
        String configKey;
        int current;
        int next;
        
        if (setting.equals("taskCount")) {
            configKey = "random_tasks.count";
            current = config.getInt(configKey, 1);
            next = current + change;
            if (next < 1) next = 1;
            if (next > 10) next = 10;
        } else {
            configKey = "hunter." + setting;
            current = config.getInt(configKey, setting.equals("teleportDelay") ? 30 : 120);
            next = current + change;
            if (next < 0) next = 0;
            if (setting.equals("teleportDelay") && next > 999) next = 999;
            if (setting.equals("compassgive") && next > 999) next = 999;
        }

        config.set(configKey, next);
        plugin.saveConfig();

        String messageKey;
        if (setting.equals("taskCount")) {
            messageKey = "task_count_changed";
        } else {
            messageKey = setting.equals("teleportDelay") ? "teleport_delay_changed" : "compass_time_changed";
        }
        player.sendMessage(ChatColor.GREEN + getLocalizedMessage(messageKey, next));
    }

    private void toggleBooleanConfig(Player player, String key, String nameKey) {
        boolean value = plugin.getConfig().getBoolean(key, true);
        boolean newValue = !value;
        plugin.getConfig().set(key, newValue);
        plugin.saveConfig();
        String messageKey = newValue ? nameKey + "_toggled_on" : nameKey + "_toggled_off";
        player.sendMessage(ChatColor.GREEN + getLocalizedMessage(messageKey));
    }

    private String getLocalizedMessage(String key, Object... args) {
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language", "en");
        String format = config.getString(lang + ".messages." + key, "§c[Missing translation: " + key + "]");
        return String.format(format, args);
    }
}
