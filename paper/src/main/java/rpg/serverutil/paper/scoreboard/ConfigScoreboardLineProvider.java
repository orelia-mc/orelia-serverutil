package rpg.serverutil.paper.scoreboard;

import org.bukkit.entity.Player;
import rpg.serverutil.api.ScoreboardLineProvider;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.List;

/**
 * Reads {@code scoreboard.lines} straight from config.yml every render (no cached state), so
 * edits take effect immediately without needing {@code /suadmin reload} to touch this class at
 * all. Registered by {@link ScoreboardModule} at the plugin's own default priority (0) so
 * higher-priority API providers (e.g. {@code CoreIntegrationModule}'s level/money line, priority
 * 100) render above it by default.
 */
final class ConfigScoreboardLineProvider implements ScoreboardLineProvider {

    private final OreliaServerUtilPlugin plugin;

    ConfigScoreboardLineProvider(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "orelia-serverutil-config-lines";
    }

    @Override
    public int getPriority() {
        return plugin.getConfigManager().get("config.yml").get().getInt("scoreboard.lines-priority", 0);
    }

    @Override
    public List<String> getLines(Player player) {
        List<String> lines = plugin.getConfigManager().get("config.yml").get().getStringList("scoreboard.lines");
        if (lines.isEmpty()) {
            return lines;
        }
        PlaceholderService placeholders = plugin.getPlaceholderService();
        return lines.stream().map(line -> placeholders.resolve(line, player)).toList();
    }
}
