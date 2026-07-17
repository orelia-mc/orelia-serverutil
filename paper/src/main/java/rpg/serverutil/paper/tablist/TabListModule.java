package rpg.serverutil.paper.tablist;

import org.bukkit.plugin.ServicePriority;
import rpg.serverutil.api.TabListApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

public final class TabListModule implements ServerUtilModule {

    private TabListManager manager;

    @Override
    public String getName() {
        return "tablist";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        long intervalTicks = plugin.getConfigManager().get("config.yml").get()
                .getLong("tablist.update-interval-ticks", 40L);

        this.manager = new TabListManager();
        plugin.getServer().getServicesManager().register(TabListApi.class, manager, plugin, ServicePriority.Normal);
        plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
    }

    public TabListManager getManager() {
        return manager;
    }
}
