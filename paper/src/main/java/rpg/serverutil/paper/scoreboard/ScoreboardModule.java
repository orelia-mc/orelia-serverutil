package rpg.serverutil.paper.scoreboard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import rpg.serverutil.api.ScoreboardApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

public final class ScoreboardModule implements ServerUtilModule, Listener {

    private ScoreboardManager manager;

    @Override
    public String getName() {
        return "scoreboard";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        var config = plugin.getConfigManager().get("config.yml").get();
        String title = config.getString("scoreboard.title", "&6Orelia");
        long intervalTicks = config.getLong("scoreboard.update-interval-ticks", 20L);

        this.manager = new ScoreboardManager(title);
        plugin.getServer().getServicesManager().register(ScoreboardApi.class, manager, plugin, ServicePriority.Normal);
        plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer());
    }

    public ScoreboardManager getManager() {
        return manager;
    }
}
