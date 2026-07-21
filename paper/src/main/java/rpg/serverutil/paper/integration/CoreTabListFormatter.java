package rpg.serverutil.paper.integration;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import rpg.api.JobApi;
import rpg.serverutil.api.TabListEntry;
import rpg.serverutil.api.TabListNameFormatter;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.Map;
import java.util.Optional;

/**
 * Colors a player's nametag/tab-list name by their current OreliaCore job (config-driven
 * job id -&gt; {@link ChatColor} map) and adds placeholder-resolved prefix/suffix text (suffix
 * defaults to the level; prefix is blank by default). Job id strings (e.g. {@code "FENCER"})
 * come straight from {@link JobApi#getCurrentJob}, so this never needs to know about
 * {@code rpg.job.model.JobType} directly - kept separate from {@link PlaceholderService}'s
 * {@code {job}} token (which resolves to the localized *display* name, not the raw id the
 * color map is keyed by). The prefix/suffix still show even when the player has no job yet -
 * only the color override is skipped in that case.
 */
final class CoreTabListFormatter implements TabListNameFormatter {

    private final JobApi jobApi;
    private final PlaceholderService placeholders;
    private final Map<String, ChatColor> jobColors;
    private final String prefixFormat;
    private final String suffixFormat;

    CoreTabListFormatter(JobApi jobApi, PlaceholderService placeholders, Map<String, ChatColor> jobColors,
                          String prefixFormat, String suffixFormat) {
        this.jobApi = jobApi;
        this.placeholders = placeholders;
        this.jobColors = jobColors;
        this.prefixFormat = prefixFormat;
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
        String job = jobApi != null ? jobApi.getCurrentJob(player.getUniqueId()).orElse(null) : null;
        String prefix = placeholders.resolve(prefixFormat, player);
        String suffix = placeholders.resolve(suffixFormat, player);
        ChatColor color = job != null ? jobColors.get(job) : null;
        return Optional.of(new TabListEntry(prefix, suffix, color));
    }
}
