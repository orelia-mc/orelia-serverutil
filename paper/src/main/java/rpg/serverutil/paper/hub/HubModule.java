package rpg.serverutil.paper.hub;

import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.command.HubCommand;
import rpg.serverutil.paper.module.ServerUtilModule;

public final class HubModule implements ServerUtilModule {

    private HubService hubService;

    @Override
    public String getName() {
        return "hub";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.hubService = new HubService(plugin);
        HubCommand hubCommand = new HubCommand(plugin, hubService);
        plugin.getCommand("hub").setExecutor(hubCommand);
    }

    @Override
    public void onDisable() {
    }

    public HubService getHubService() {
        return hubService;
    }
}
