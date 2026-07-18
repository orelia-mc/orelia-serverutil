package rpg.serverutil.paper.integration;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import rpg.api.JobApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.TabListEntry;
import rpg.serverutil.api.TabListNameFormatter;

import java.util.Map;
import java.util.Optional;

/**
 * Colors a player's nametag/tab-list name by their current OreliaCore job (config-driven
 * job id -&gt; {@link ChatColor} map) and appends a level suffix. Job id strings (e.g.
 * {@code "FENCER"}) come straight from {@link JobApi#getCurrentJob}, so this never needs to
 * know about {@code rpg.job.model.JobType} directly.
 */
final class CoreTabListFormatter implements TabListNameFormatter {

    private final JobApi jobApi;
    private final StatusApi statusApi;
    private final Map<String, ChatColor> jobColors;
    private final String suffixFormat;

    CoreTabListFormatter(JobApi jobApi, StatusApi statusApi, Map<String, ChatColor> jobColors, String suffixFormat) {
        this.jobApi = jobApi;
        this.statusApi = statusApi;
        this.jobColors = jobColors;
        this.suffixFormat = suffixFormat;
    }

    @Override
    public String getId() {
        return "orelia-core-tablist";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Optional<TabListEntry> format(Player player) {
        String job = jobApi.getCurrentJob(player.getUniqueId()).orElse(null);
        if (job == null) {
            return Optional.empty();
        }
        int level = statusApi != null ? statusApi.getLevel(player.getUniqueId()).orElse(1) : 1;
        String suffix = suffixFormat.replace("{level}", String.valueOf(level));
        ChatColor color = jobColors.get(job);
        return Optional.of(new TabListEntry("", suffix, color));
    }
}
