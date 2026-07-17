package rpg.serverutil.paper;

import org.bukkit.plugin.java.JavaPlugin;
import rpg.serverutil.paper.announce.AnnounceModule;
import rpg.serverutil.paper.bridge.VelocityBridgeModule;
import rpg.serverutil.paper.command.SuAdminCommand;
import rpg.serverutil.paper.config.ConfigManager;
import rpg.serverutil.paper.healthcheck.HealthCheckModule;
import rpg.serverutil.paper.hub.HubModule;
import rpg.serverutil.paper.integration.CoreIntegrationModule;
import rpg.serverutil.paper.join.JoinMessageModule;
import rpg.serverutil.paper.message.MessageManager;
import rpg.serverutil.paper.module.ServerUtilModuleManager;
import rpg.serverutil.paper.scoreboard.ScoreboardModule;
import rpg.serverutil.paper.spawn.SpawnModule;
import rpg.serverutil.paper.tablist.TabListModule;
import rpg.serverutil.paper.worldsetup.WorldSetupModule;

/**
 * Plugin entry point for orelia-serverutil's Paper module: server-operations/UX tooling
 * (hub transfer, world setup, scoreboard/tablist API, join messages) that is deliberately
 * gameplay-independent. OreliaCore is a soft dependency only - every lookup of its APIs
 * (StatusApi/EconomyApi) must null-guard, see {@code rpg.serverutil.paper.integration}.
 *
 * <p>Unlike orelia-world/orelia-extra/orelia-debug, this plugin does not reuse orelia-core's
 * {@code ConfigManager}/{@code MessageManager} infrastructure classes - since OreliaCore may
 * not be installed at all, importing those classes directly would risk a
 * {@code NoClassDefFoundError} at startup. This plugin carries its own lightweight copies
 * (see {@code rpg.serverutil.paper.config}/{@code rpg.serverutil.paper.message}).
 */
public final class OreliaServerUtilPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private ServerUtilModuleManager moduleManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.register("config.yml");
        this.messageManager = new MessageManager(configManager.register("messages.yml"));

        this.moduleManager = new ServerUtilModuleManager(this);

        // Registration order doubles as dependency order, same convention as orelia-core.
        // Modules are added here across phases 2-4/6-7 of the implementation plan.
        moduleManager.register(new SpawnModule());
        moduleManager.register(new WorldSetupModule());
        moduleManager.register(new VelocityBridgeModule());
        moduleManager.register(new HubModule());
        moduleManager.register(new ScoreboardModule());
        moduleManager.register(new TabListModule());
        moduleManager.register(new JoinMessageModule());
        moduleManager.register(new AnnounceModule());
        moduleManager.register(new HealthCheckModule());
        // Always last: reaches for ScoreboardApi (registered above) and OreliaCore's own
        // published APIs, both of which must already exist by the time this runs.
        moduleManager.register(new CoreIntegrationModule());

        moduleManager.enableAll();

        SuAdminCommand suAdminCommand = new SuAdminCommand(this);
        getCommand("suadmin").setExecutor(suAdminCommand);
        getCommand("suadmin").setTabCompleter(suAdminCommand);

        // /hub is registered by HubModule once it's wired up (phase 2).
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
    }

    public void reload() {
        configManager.reloadAll();
        moduleManager.reloadAll();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ServerUtilModuleManager getModuleManager() {
        return moduleManager;
    }
}
