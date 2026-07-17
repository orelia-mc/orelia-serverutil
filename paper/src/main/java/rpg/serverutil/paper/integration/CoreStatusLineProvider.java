package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.api.EconomyApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.ScoreboardLineProvider;

import java.util.ArrayList;
import java.util.List;

/** Sample {@link ScoreboardLineProvider} sourcing lines from OreliaCore's published APIs. */
final class CoreStatusLineProvider implements ScoreboardLineProvider {

    private final StatusApi statusApi;
    private final EconomyApi economyApi;

    CoreStatusLineProvider(StatusApi statusApi, EconomyApi economyApi) {
        this.statusApi = statusApi;
        this.economyApi = economyApi;
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
        List<String> lines = new ArrayList<>();
        if (statusApi != null) {
            statusApi.getLevel(player.getUniqueId()).ifPresent(level -> lines.add("&fLv. &e" + level));
        }
        if (economyApi != null) {
            lines.add("&fG: &6" + (long) economyApi.getBalance(player.getUniqueId()));
        }
        return lines;
    }
}
