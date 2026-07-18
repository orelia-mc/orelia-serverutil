package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.api.JobApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.BelownameValueProvider;

import java.util.Optional;

/**
 * Shows the player's OreliaCore level and current job display name below their nametag. Still
 * shows the level when the player has no job yet - {@code {job}} just resolves to an empty
 * string in that case (design the "format" template accordingly, e.g. don't hardcode brackets
 * around {@code {job}} that would look odd when it's blank).
 */
final class CoreBelownameProvider implements BelownameValueProvider {

    private final JobApi jobApi;
    private final StatusApi statusApi;
    private final String format;

    CoreBelownameProvider(JobApi jobApi, StatusApi statusApi, String format) {
        this.jobApi = jobApi;
        this.statusApi = statusApi;
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
        int level = statusApi != null ? statusApi.getLevel(target.getUniqueId()).orElse(1) : 1;
        String job = jobApi != null ? jobApi.getCurrentJobDisplayName(target.getUniqueId()).orElse("") : "";
        return Optional.of(format.replace("{level}", String.valueOf(level)).replace("{job}", job));
    }
}
