package rpg.serverutil.paper.integration;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import rpg.api.EconomyApi;
import rpg.api.JobApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.BelownameApi;
import rpg.serverutil.api.ChatApi;
import rpg.serverutil.api.ScoreboardApi;
import rpg.serverutil.api.TabListApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional bridge to OreliaCore (soft dependency): if any of {@link StatusApi}/
 * {@link EconomyApi}/{@link JobApi} happen to be published, registers config-driven,
 * {@link PlaceholderService}-resolved providers into every display API this plugin offers -
 * sidebar line, tab-list name color/prefix/suffix, tab-list right-side value, belowname, and
 * chat placeholder. This plugin works fine without OreliaCore installed at all (see
 * plugin.yml {@code softdepend}); each registration below independently null-guards the
 * specific API it needs.
 *
 * <p>{@code core-integration.enabled} is the single master switch (checked once in
 * {@link #onEnable}) - when {@code false}, none of the OreliaCore-sourced sections below
 * (each living under its own feature's top-level config block, e.g.
 * {@code tablist.name-color}/{@code scoreboard.core-lines}/{@code belowname.core-format}/
 * {@code chat.core-placeholder}) do anything, regardless of their own individual
 * {@code enabled} flag.
 */
public final class CoreIntegrationModule implements ServerUtilModule {

    private OreliaServerUtilPlugin plugin;

    @Override
    public String getName() {
        return "core-integration";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("core-integration.enabled", true)) {
            return;
        }

        StatusApi statusApi = plugin.getServer().getServicesManager().load(StatusApi.class);
        EconomyApi economyApi = plugin.getServer().getServicesManager().load(EconomyApi.class);
        JobApi jobApi = plugin.getServer().getServicesManager().load(JobApi.class);
        if (statusApi == null && economyApi == null && jobApi == null) {
            return; // OreliaCore not installed - nothing to integrate with.
        }

        PlaceholderService placeholders = plugin.getPlaceholderService();
        registerScoreboardLine(plugin, config, placeholders);
        registerTabListName(plugin, config, jobApi, statusApi, placeholders);
        registerTabListValue(plugin, config, statusApi, placeholders);
        registerBelowname(plugin, config, jobApi, statusApi, placeholders);
        registerChatPlaceholder(plugin, config, jobApi, statusApi, placeholders);
    }

    @Override
    public void onDisable() {
    }

    /**
     * Every {@code register*} method below re-registers under a stable provider id (each
     * {@code Core*Provider}'s {@code getId()} never changes), so simply re-running the whole
     * enable sequence picks up fresh config.yml formats without disturbing providers registered
     * by anything else - {@code Map#put} on an existing id just replaces that one entry.
     */
    @Override
    public void onReload() {
        onEnable(plugin);
    }

    private void registerScoreboardLine(OreliaServerUtilPlugin plugin, YamlConfiguration config, PlaceholderService placeholders) {
        if (!config.getBoolean("scoreboard.core-lines.enabled", true)) {
            return;
        }
        ScoreboardApi scoreboardApi = plugin.getServer().getServicesManager().load(ScoreboardApi.class);
        if (scoreboardApi == null) {
            return;
        }
        List<String> lines = config.getStringList("scoreboard.core-lines.lines");
        if (lines.isEmpty()) {
            return;
        }
        scoreboardApi.registerProvider(new CoreStatusLineProvider(placeholders, lines));
    }

    private void registerTabListName(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi,
                                      PlaceholderService placeholders) {
        if ((jobApi == null && statusApi == null) || !config.getBoolean("tablist.name-color.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        Map<String, ChatColor> jobColors = parseJobColors(config.getConfigurationSection("tablist.name-color.job-colors"));
        String prefixFormat = config.getString("tablist.name-color.prefix-format", "");
        String suffixFormat = config.getString("tablist.name-color.suffix-format", " &%7[Lv.{level}]");
        tabListApi.registerFormatter(new CoreTabListFormatter(jobApi, placeholders, jobColors, prefixFormat, suffixFormat));
    }

    private void registerTabListValue(OreliaServerUtilPlugin plugin, YamlConfiguration config, StatusApi statusApi,
                                       PlaceholderService placeholders) {
        if (statusApi == null || !config.getBoolean("tablist.value.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        String valueFormat = config.getString("tablist.value.format", "&%aLv.{level}");
        tabListApi.registerValueProvider(new CoreTabListValueProvider(placeholders, valueFormat));
    }

    private void registerBelowname(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi,
                                    PlaceholderService placeholders) {
        if ((jobApi == null && statusApi == null) || !config.getBoolean("belowname.core-format.enabled", true)) {
            return;
        }
        BelownameApi belownameApi = plugin.getServer().getServicesManager().load(BelownameApi.class);
        if (belownameApi == null) {
            return;
        }
        String format = config.getString("belowname.core-format.format", "&%7Lv.{level} {job}");
        belownameApi.registerProvider(new CoreBelownameProvider(placeholders, format));
    }

    private void registerChatPlaceholder(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi,
                                          PlaceholderService placeholders) {
        // Default format is "[Lv.{level}] {job}" - both tokens need StatusApi/JobApi, same
        // guard registerTabListName/registerBelowname already apply, so this doesn't sit in
        // every chat message as literal "[Lv.{level}] {job}" text on a partial OreliaCore
        // install (e.g. only EconomyApi present).
        if ((jobApi == null && statusApi == null) || !config.getBoolean("chat.core-placeholder.enabled", true)) {
            return;
        }
        ChatApi chatApi = plugin.getServer().getServicesManager().load(ChatApi.class);
        if (chatApi == null) {
            return;
        }
        String format = config.getString("chat.core-placeholder.format", "&%7[Lv.{level}] &%b{job}&r ");
        chatApi.registerProvider(new CoreChatPlaceholderProvider(placeholders, format));
    }

    /**
     * Parses {@code job-colors} entries. Deliberately plain legacy 2-char {@code &}-codes
     * (e.g. {@code "&b"}), NOT this plugin's {@code &%}-prefixed custom hex palette used
     * everywhere else - a job color ends up as a scoreboard {@code Team}'s color
     * ({@code TabListManager#tick}), and {@code org.bukkit.scoreboard.Team#setColor} only
     * accepts the 16 legacy {@link ChatColor} values, never arbitrary hex.
     */
    private Map<String, ChatColor> parseJobColors(ConfigurationSection section) {
        Map<String, ChatColor> colors = new HashMap<>();
        if (section == null) {
            return colors;
        }
        for (String jobId : section.getKeys(false)) {
            String raw = section.getString(jobId, "").trim();
            if (raw.length() != 2 || raw.charAt(0) != '&' || ChatColor.getByChar(raw.charAt(1)) == null) {
                plugin.getLogger().warning("tablist.name-color.job-colors." + jobId + " (\"" + raw + "\") isn't a "
                        + "legacy &-color code (e.g. \"&b\") - job colors can't use the &% custom palette "
                        + "since scoreboard Team colors don't support hex. Skipping this entry.");
                continue;
            }
            colors.put(jobId, ChatColor.getByChar(raw.charAt(1)));
        }
        return colors;
    }
}
