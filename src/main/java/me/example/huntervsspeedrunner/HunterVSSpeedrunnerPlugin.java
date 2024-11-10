package me.example.huntervsspeedrunner;

import me.example.huntervsspeedrunner.listeners.PlayerDeathListener;
import me.example.huntervsspeedrunner.listeners.EnderDragonDeathListener;
import me.example.huntervsspeedrunner.utils.GameManager;
import me.example.huntervsspeedrunner.utils.LifeManager;
import me.example.huntervsspeedrunner.listeners.MenuListener;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HunterVSSpeedrunnerPlugin extends JavaPlugin {

    private LifeManager lifeManager;
    private GameManager gameManager;  // Добавляем поле для GameManager
    private boolean isMenuOpen = false;  // Флаг, показывающий, открыто ли меню выбора

    @Override
    public void onEnable() {
        getLogger().info("Плагин загружен успешно!");
        this.saveDefaultConfig();  // Сохраняем дефолтную конфигурацию
        this.lifeManager = new LifeManager(this);
        this.gameManager = new GameManager();  // Инициализируем GameManager

        // Регистрируем слушателей событий
        getServer().getPluginManager().registerEvents(new EnderDragonDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        // Регистрация команд
        if (getCommand("hunter") != null) {
            getCommand("hunter").setExecutor(this);
        } else {
            getLogger().warning("Команда 'hunter' не найдена в plugin.yml");
        }

        if (getCommand("start") != null) {
            getCommand("start").setExecutor(this);  // Регистрация команды start
        } else {
            getLogger().warning("Команда 'start' не найдена в plugin.yml");
        }

        if (getCommand("stop") != null) {
            getCommand("stop").setExecutor(this);   // Регистрация команды stop
        } else {
            getLogger().warning("Команда 'stop' не найдена в plugin.yml");
        }

        if (getCommand("hunterworld") != null) {
            getCommand("hunterworld").setExecutor(this); // Регистрация новой команды "hunterworld"
        } else {
            getLogger().warning("Команда 'hunterworld' не найдена в plugin.yml");
        }
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public GameManager getGameManager() {
        return gameManager;  // Метод для получения GameManager
    }

    public boolean isMenuOpen() {
        return isMenuOpen;
    }

    public void setMenuOpen(boolean menuOpen) {
        this.isMenuOpen = menuOpen;
    }

    // Добавление метода для получения предмета компаса из конфигурации
    public ItemStack getCompassItem() {
        FileConfiguration config = this.getConfig();
        String materialName = config.getString("hunter.compass.item");
        String displayName = config.getString("hunter.compass.name");

        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Неизвестный материал: " + materialName);
        }

        ItemStack compassItem = new ItemStack(material);
        ItemMeta meta = compassItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            compassItem.setItemMeta(meta);
        }

        return compassItem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Команда /hunter (открыть меню)
        if (command.getName().equalsIgnoreCase("hunter")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (gameManager.isGameStarted()) {  // Проверка на статус игры через GameManager
                        player.sendMessage("§cИгра уже началась! Невозможно открыть меню.");
                    } else {
                        if (!isMenuOpen()) {  // Проверяем, не открыто ли меню
                            setMenuOpen(true);  // Устанавливаем флаг, что меню открыто
                            gameManager.openTeamSelectionMenu(player, this);  // Открытие меню через GameManager
                        } else {
                            player.sendMessage("§cМеню уже открыто!");
                        }
                    }
                } else {
                    sender.sendMessage("Команду могут использовать только игроки.");
                }
                return true;
            }

            // Если аргумент "start" или "stop" — для операторов
            if (args[0].equalsIgnoreCase("start")) {
                if (!sender.isOp()) {  // Проверка, является ли отправитель оператором
                    sender.sendMessage("§cУ вас нет прав для использования этой команды.");
                    return true;
                }
                if (gameManager.canStartGame(this)) {  // Старт игры через GameManager
                    gameManager.startGame(this);
                    sender.sendMessage("§aИгра началась!");
                } else {
                    sender.sendMessage("§cИгра не может начаться: должны быть хотя бы один Охотник и один Спидраннер.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Команду /stop могут использовать только игроки.");
                    return false;
                }
                if (!sender.isOp()) {  // Проверка, является ли отправитель оператором
                    sender.sendMessage("§cУ вас нет прав для использования этой команды.");
                    return true;
                }
                if (gameManager.isGameStarted()) {  // Завершение игры через GameManager
                    gameManager.endGame();
                    sender.sendMessage("§aИгра завершена!");
                } else {
                    sender.sendMessage("§cИгра не может быть завершена, так как она не началась.");
                }
                return true;
            }
        }

        // Новая команда для выполнения последовательности команд с задержкой
        if (command.getName().equalsIgnoreCase("hunterworld")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cКоманду может выполнить только игрок.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("hunter.world")) {
                player.sendMessage("§cУ вас нет прав для выполнения этой команды.");
                return false;
            }

            // Выполнение команд с задержкой
            executeWorldCommands(player);
            return true;
        }

        return false;
    }

    // Метод для выполнения последовательности команд с задержкой
    private void executeWorldCommands(Player player) {
        player.sendMessage("Началось пересоздание миров подождите...");
        new BukkitRunnable() {
            int step = 0;
            final String[] commands = {
                    "mv delete Event",
                    "mv confirm",
                    "mv delete Event_nether",
                    "mv confirm",
                    "mv delete Event_end",
                    "mv confirm",
                    "mv create Event world",
                    "mv create Event_nether nether",
                    "mv create Event_end end"
            };


            @Override

            public void run() {
                if (step < commands.length) {
                    // Выполнение команды
                    String command = commands[step];
                    getServer().dispatchCommand(getServer().getConsoleSender(), command);
                    step++;
                } else {
                    cancel(); // Завершаем выполнение всех команд
                    player.sendMessage("Все команды выполнены.");
                }
            }
        }.runTaskTimer(this, 0L, 60L); // 60L тиков = 3 секунды
    }
}
