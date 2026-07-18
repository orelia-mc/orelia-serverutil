package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.api.StatusApi;
import rpg.serverutil.api.TabListValueProvider;

import java.util.Optional;

/** Shows the player's OreliaCore level on the right side of their tab-list entry. */
final class CoreTabListValueProvider implements TabListValueProvider {

    private final StatusApi statusApi;
    private final String format;

    CoreTabListValueProvider(StatusApi statusApi, String format) {
        this.statusApi = statusApi;
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
        return statusApi.getLevel(target.getUniqueId()).map(level -> format.replace("{level}", String.valueOf(level)));
    }
}
