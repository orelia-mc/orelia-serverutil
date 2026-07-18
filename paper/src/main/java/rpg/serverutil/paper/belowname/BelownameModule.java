package rpg.serverutil.paper.belowname;

import org.bukkit.plugin.ServicePriority;
import rpg.serverutil.api.BelownameApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

/** Off by default (matches TAB's own default) - registering an objective in BELOW_NAME shows
 *  the configured title to every player even without a registered provider, which most servers
 *  won't want as unsolicited noise. */
public final class BelownameModule implements ServerUtilModule {

    private BelownameManager manager;

    @Override
    public String getName() {
        return "belowname";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        var config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("belowname.enabled", false)) {
            return;
        }
        String title = config.getString("belowname.title", "");
        long intervalTicks = config.getLong("belowname.update-interval-ticks", 20L);

        this.manager = new BelownameManager(title);
        plugin.getServer().getServicesManager().register(BelownameApi.class, manager, plugin, ServicePriority.Normal);
        plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
    }

    public BelownameManager getManager() {
        return manager;
    }
}
