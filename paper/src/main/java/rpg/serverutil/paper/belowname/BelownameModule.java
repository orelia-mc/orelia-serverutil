package rpg.serverutil.paper.belowname;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;
import rpg.serverutil.api.BelownameApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

/** Always registers {@link BelownameApi} and starts ticking - {@link BelownameManager} itself
 *  auto-hides the BELOW_NAME objective whenever no provider is producing a value, so there is
 *  no separate config flag to flip before a provider (e.g. {@code CoreIntegrationModule}) works. */
public final class BelownameModule implements ServerUtilModule {

    private OreliaServerUtilPlugin plugin;
    private BelownameManager manager;
    private BukkitTask tickTask;

    @Override
    public String getName() {
        return "belowname";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfigManager().get("config.yml").get();
        String title = config.getString("belowname.title", "");
        long intervalTicks = config.getLong("belowname.update-interval-ticks", 20L);

        this.manager = new BelownameManager(plugin.getPlaceholderService(), title);
        plugin.getServer().getServicesManager().register(BelownameApi.class, manager, plugin, ServicePriority.Normal);
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReload() {
        var config = plugin.getConfigManager().get("config.yml").get();
        manager.setTitleTemplate(config.getString("belowname.title", ""));

        long intervalTicks = config.getLong("belowname.update-interval-ticks", 20L);
        tickTask.cancel();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
    }

    public BelownameManager getManager() {
        return manager;
    }
}
