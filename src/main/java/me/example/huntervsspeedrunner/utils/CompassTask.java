package me.example.huntervsspeedrunner.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassTask extends BukkitRunnable {

    private final Player hunter;
    private Player target;

    // Конструктор для логирования
    public CompassTask(Player hunter, Player target) {
        this.hunter = hunter;
        this.target = target;
        Bukkit.getLogger().info("Создана задача для отслеживания спидраннера: " + (target != null ? target.getName() : "неизвестен"));
    }

    @Override
    public void run() {
        // Логируем, что задача запустилась
        Bukkit.getLogger().info("Запуск задачи отслеживания спидраннера.");

        // Проверяем, что цель существует и онлайн
        if (target != null && target.isOnline()) {
            // Получаем мир спидраннера
            World world = target.getWorld();

            // Логируем текущие координаты спидраннера
            Bukkit.getLogger().info("Обновление спавна на координатах спидраннера: " + target.getLocation().toString());

            // Устанавливаем глобальный спавн мира на позицию спидраннера
            world.setSpawnLocation(target.getLocation());

            // Логируем установку глобального спавна
            Bukkit.getLogger().info("Глобальный спавн мира " + world.getName() + " обновлен на координаты: " + target.getLocation().toString());

            // Устанавливаем личный спавн для всех игроков
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setBedSpawnLocation(target.getLocation(), true);

                // Логируем установку персонального спавна для каждого игрока
                Bukkit.getLogger().info("Персональный спавн игрока " + player.getName() + " обновлен на координаты спидраннера: " + target.getLocation().toString());
            }

            // Дополнительно: выводим сообщение в консоль о том, что задача была выполнена
            Bukkit.getLogger().info("Задача по обновлению спавна для всех игроков завершена.");
        } else {
            // Если цель не онлайн, выводим предупреждение в консоль
            Bukkit.getLogger().warning("Спидраннер " + (target != null ? target.getName() : "неизвестен") + " не онлайн. Спавн не обновлен.");
        }
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    // Статический метод для запуска задачи
    public static void startTracking(Player hunter, Player target) {
        Bukkit.getLogger().info("Запуск задачи для отслеживания спидраннера...");
        new CompassTask(hunter, target).runTaskTimer(Bukkit.getPluginManager().getPlugin("HunterVSSpeedrunner"), 0L, 100L); // 100 тиков = 5 секунд
    }
}
