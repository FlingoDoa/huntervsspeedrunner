package me.example.huntervsspeedrunner.random;

import org.bukkit.entity.Player;
import java.util.function.Predicate;

public class Task {
    private final String description;
    private final Predicate<Player> condition;

    public Task(String description, Predicate<Player> condition) {
        this.description = description;
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }
    public Predicate<Player> getCondition() {
        return condition;
    }
    public boolean isCompleted(Player player) {
        return condition.test(player);
    }
}
