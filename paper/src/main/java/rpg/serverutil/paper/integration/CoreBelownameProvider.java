package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.api.JobApi;
import rpg.serverutil.api.BelownameValueProvider;

import java.util.Optional;

/** Shows the player's current OreliaCore job display name below their nametag. */
final class CoreBelownameProvider implements BelownameValueProvider {

    private final JobApi jobApi;
    private final String format;

    CoreBelownameProvider(JobApi jobApi, String format) {
        this.jobApi = jobApi;
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
        return jobApi.getCurrentJobDisplayName(target.getUniqueId()).map(job -> format.replace("{job}", job));
    }
}
