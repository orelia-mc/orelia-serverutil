package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.serverutil.api.BelownameValueProvider;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.Optional;

/** Shows a config-driven, placeholder-resolved value below the nametag (level + job by default). */
final class CoreBelownameProvider implements BelownameValueProvider {

    private final PlaceholderService placeholders;
    private final String format;

    CoreBelownameProvider(PlaceholderService placeholders, String format) {
        this.placeholders = placeholders;
        this.format = format;
    }

    @Override
    public String getId() {
        return "orelia-core-belowname";
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
