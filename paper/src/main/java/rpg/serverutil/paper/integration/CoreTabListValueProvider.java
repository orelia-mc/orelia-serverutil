package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.serverutil.api.TabListValueProvider;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.Optional;

/** Shows a config-driven, placeholder-resolved value on the right side of the tab-list entry (level by default). */
final class CoreTabListValueProvider implements TabListValueProvider {

    private final PlaceholderService placeholders;
    private final String format;

    CoreTabListValueProvider(PlaceholderService placeholders, String format) {
        this.placeholders = placeholders;
        this.format = format;
    }

    @Override
    public String getId() {
        return "orelia-core-tablist-value";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Optional<String> getValue(Player target) {
        return Optional.of(placeholders.resolve(format, target));
    }
}
