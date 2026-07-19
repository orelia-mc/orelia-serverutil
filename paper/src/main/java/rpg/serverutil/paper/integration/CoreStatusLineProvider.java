package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.serverutil.api.ScoreboardLineProvider;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.List;

/** Config-driven, {@link PlaceholderService}-resolved sidebar lines (level/money by default) - see {@code core-integration.scoreboard} in config.yml. */
final class CoreStatusLineProvider implements ScoreboardLineProvider {

    private final PlaceholderService placeholders;
    private final List<String> lines;

    CoreStatusLineProvider(PlaceholderService placeholders, List<String> lines) {
        this.placeholders = placeholders;
        this.lines = lines;
    }

    @Override
    public String getId() {
        return "orelia-core";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public List<String> getLines(Player player) {
        return lines.stream().map(line -> placeholders.resolve(line, player)).toList();
    }
}
