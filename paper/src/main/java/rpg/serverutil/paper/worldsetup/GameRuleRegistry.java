package rpg.serverutil.paper.worldsetup;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies config-driven {@code GameRule} values to a world, using Bukkit's own
 * {@link GameRule#getByName(String)} lookup table instead of hand-maintaining a config-key to
 * GameRule mapping - any vanilla gamerule name (e.g. {@code doMobSpawning}, {@code
 * keepInventory}) works out of the box in {@code config.yml}.
 */
final class GameRuleRegistry {

    private GameRuleRegistry() {
    }

    /**
     * Applies every key under {@code gamerules} to {@code world}. Returns the keys that
     * couldn't be applied (unknown gamerule name, or a value whose type doesn't match the
     * rule's expected type) - empty if everything applied cleanly or {@code gamerules} is null.
     */
    static List<String> apply(World world, ConfigurationSection gamerules) {
        List<String> invalidKeys = new ArrayList<>();
        if (gamerules == null) {
            return invalidKeys;
        }
        for (String key : gamerules.getKeys(false)) {
            GameRule<?> rule = GameRule.getByName(key);
            if (rule == null || !applyOne(world, rule, gamerules.get(key))) {
                invalidKeys.add(key);
            }
        }
        return invalidKeys;
    }

    private static <T> boolean applyOne(World world, GameRule<T> rule, Object rawValue) {
        try {
            T value = rule.getType().cast(rawValue);
            world.setGameRule(rule, value);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
