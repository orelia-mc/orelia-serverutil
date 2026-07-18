package rpg.serverutil.paper.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Isolated PlaceholderAPI integration - only ever called from {@link PlaceholderService} after
 * it has already confirmed PlaceholderAPI is installed and enabled. Keeping the direct
 * {@code me.clip.placeholderapi} reference in its own class means the JVM never needs to
 * resolve it when the plugin isn't present, avoiding a {@code NoClassDefFoundError}.
 */
final class PlaceholderApiHook {

    private PlaceholderApiHook() {
    }

    static String resolve(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
