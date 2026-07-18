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

import java.util.HashMap;
import java.util.Map;

/**
 * Optional bridge to OreliaCore (soft dependency): if any of {@link StatusApi}/
 * {@link EconomyApi}/{@link JobApi} happen to be published, registers config-driven
 * providers into every display API this plugin offers - sidebar line, tab-list name
 * color/suffix, tab-list right-side value, belowname, and chat placeholder. This plugin
 * works fine without OreliaCore installed at all (see plugin.yml {@code softdepend}); each
 * registration below independently null-guards the specific API it needs.
 */
public final class CoreIntegrationModule implements ServerUtilModule {

    @Override
    public String getName() {
        return "core-integration";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
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

        registerScoreboardLine(plugin, config, statusApi, economyApi);
        registerTabListName(plugin, config, jobApi, statusApi);
        registerTabListValue(plugin, config, statusApi);
        registerBelowname(plugin, config, jobApi);
        registerChatPlaceholder(plugin, config, jobApi, statusApi);
    }

    @Override
    public void onDisable() {
    }

    private void registerScoreboardLine(OreliaServerUtilPlugin plugin, YamlConfiguration config, StatusApi statusApi, EconomyApi economyApi) {
        ScoreboardApi scoreboardApi = plugin.getServer().getServicesManager().load(ScoreboardApi.class);
        if (scoreboardApi != null) {
            scoreboardApi.registerProvider(new CoreStatusLineProvider(statusApi, economyApi));
        }
    }

    private void registerTabListName(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi) {
        if (jobApi == null || !config.getBoolean("core-integration.tablist.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        Map<String, ChatColor> jobColors = parseJobColors(config.getConfigurationSection("core-integration.tablist.job-colors"));
        String suffixFormat = config.getString("core-integration.tablist.suffix-format", " &7[Lv.{level}]");
        tabListApi.registerFormatter(new CoreTabListFormatter(jobApi, statusApi, jobColors, suffixFormat));
    }

    private void registerTabListValue(OreliaServerUtilPlugin plugin, YamlConfiguration config, StatusApi statusApi) {
        if (statusApi == null || !config.getBoolean("core-integration.tablist-value.enabled", true)) {
            return;
        }
        TabListApi tabListApi = plugin.getServer().getServicesManager().load(TabListApi.class);
        if (tabListApi == null) {
            return;
        }
        String valueFormat = config.getString("core-integration.tablist-value.format", "&aLv.{level}");
        tabListApi.registerValueProvider(new CoreTabListValueProvider(statusApi, valueFormat));
    }

    private void registerBelowname(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi) {
        if (jobApi == null || !config.getBoolean("core-integration.belowname.enabled", true)) {
            return;
        }
        BelownameApi belownameApi = plugin.getServer().getServicesManager().load(BelownameApi.class);
        if (belownameApi == null) {
            return;
        }
        String format = config.getString("core-integration.belowname.format", "&7{job}");
        belownameApi.registerProvider(new CoreBelownameProvider(jobApi, format));
    }

    private void registerChatPlaceholder(OreliaServerUtilPlugin plugin, YamlConfiguration config, JobApi jobApi, StatusApi statusApi) {
        if (!config.getBoolean("core-integration.chat.enabled", true)) {
            return;
        }
        ChatApi chatApi = plugin.getServer().getServicesManager().load(ChatApi.class);
        if (chatApi == null) {
            return;
        }
        String format = config.getString("core-integration.chat.placeholder-format", "&7[Lv.{level}] &b{job}&r ");
        chatApi.registerProvider(new CoreChatPlaceholderProvider(jobApi, statusApi, format));
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
