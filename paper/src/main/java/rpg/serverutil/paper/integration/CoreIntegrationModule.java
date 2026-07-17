package rpg.serverutil.paper.integration;

import rpg.api.EconomyApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.ScoreboardApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

/**
 * Optional bridge to OreliaCore (soft dependency): if {@link StatusApi}/{@link EconomyApi}
 * happen to be published, registers a sample sidebar line showing level/balance. Demonstrates
 * the intended integration pattern for gameplay plugins wanting to feed the scoreboard - this
 * plugin works fine without OreliaCore installed at all (see plugin.yml {@code softdepend}).
 */
public final class CoreIntegrationModule implements ServerUtilModule {

    @Override
    public String getName() {
        return "core-integration";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        if (!plugin.getConfigManager().get("config.yml").get().getBoolean("core-integration.enabled", true)) {
            return;
        }

        StatusApi statusApi = plugin.getServer().getServicesManager().load(StatusApi.class);
        EconomyApi economyApi = plugin.getServer().getServicesManager().load(EconomyApi.class);
        if (statusApi == null && economyApi == null) {
            return; // OreliaCore not installed - nothing to integrate with.
        }

        ScoreboardApi scoreboardApi = plugin.getServer().getServicesManager().load(ScoreboardApi.class);
        if (scoreboardApi == null) {
            return;
        }
        scoreboardApi.registerProvider(new CoreStatusLineProvider(statusApi, economyApi));
    }

    @Override
    public void onDisable() {
    }
}
