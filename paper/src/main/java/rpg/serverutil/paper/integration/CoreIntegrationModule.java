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
 * sidebar line, tab-list name color/suffix, tab-list right-side value, belowname, and chat
 * placeholder. This plugin works fine without OreliaCore installed at all (see plugin.yml
 * {@code softdepend}); each registration below independently null-guards the specific API it
 * needs.
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
        registerChatPlaceholder(plugin, config, placeholders);
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
        if (!config.getBoolean("core-integration.scoreboard.enabled", true)) {
            return;
        }
        ScoreboardApi scoreboardApi = plugin.getServer().getServicesManager().load(ScoreboardApi.class);
        if (scoreboardApi == null) {
            return;
        }
        List<String> lines = config.getStringList("core-integration.scoreboard.lines");
        if (lines.isEmpty()) {
            return;
        }
        scoreboardApi.registerProvider(new CoreStatusLineProvider(placeholders, lines));
    }

    private void registerTabListName(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi,
                                      PlaceholderService placeholders) {
        if ((jobApi == null && statusApi == null) || !config.getBoolean("core-integration.tablist.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        Map<String, ChatColor> jobColors = parseJobColors(config.getConfigurationSection("core-integration.tablist.job-colors"));
        String suffixFormat = config.getString("core-integration.tablist.suffix-format", " &7[Lv.{level}]");
        tabListApi.registerFormatter(new CoreTabListFormatter(jobApi, placeholders, jobColors, suffixFormat));
    }

    private void registerTabListValue(OreliaServerUtilPlugin plugin, YamlConfiguration config, StatusApi statusApi,
                                       PlaceholderService placeholders) {
        if (statusApi == null || !config.getBoolean("core-integration.tablist-value.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        String valueFormat = config.getString("core-integration.tablist-value.format", "&aLv.{level}");
        tabListApi.registerValueProvider(new CoreTabListValueProvider(placeholders, valueFormat));
    }

    private void registerBelowname(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi,
                                    PlaceholderService placeholders) {
        if ((jobApi == null && statusApi == null) || !config.getBoolean("core-integration.belowname.enabled", true)) {
            return;
        }
        BelownameApi belownameApi = plugin.getServer().getServicesManager().load(BelownameApi.class);
        if (belownameApi == null) {
            return;
        }
        String format = config.getString("core-integration.belowname.format", "&7Lv.{level} {job}");
        belownameApi.registerProvider(new CoreBelownameProvider(placeholders, format));
    }

    private void registerChatPlaceholder(OreliaServerUtilPlugin plugin, YamlConfiguration config, PlaceholderService placeholders) {
        if (!config.getBoolean("core-integration.chat.enabled", true)) {
            return;
        }
        ChatApi chatApi = plugin.getServer().getServicesManager().load(ChatApi.class);
        if (chatApi == null) {
            return;
        }
        String format = config.getString("core-integration.chat.placeholder-format", "&7[Lv.{level}] &b{job}&r ");
        chatApi.registerProvider(new CoreChatPlaceholderProvider(placeholders, format));
    }

    /** Parses {@code job-colors} entries written in the same {@code &}-code style as every other config color in this plugin (e.g. {@code "&b"}). */
    private Map<String, ChatColor> parseJobColors(ConfigurationSection section) {
        Map<String, ChatColor> colors = new HashMap<>();
        if (section == null) {
            return colors;
        }
        for (String jobId : section.getKeys(false)) {
            String raw = section.getString(jobId, "").trim();
            if (raw.length() != 2 || raw.charAt(0) != '&') {
                continue;
            }
            ChatColor color = ChatColor.getByChar(raw.charAt(1));
            if (color != null) {
                colors.put(jobId, color);
            }
        }
        return colors;
    }
}
