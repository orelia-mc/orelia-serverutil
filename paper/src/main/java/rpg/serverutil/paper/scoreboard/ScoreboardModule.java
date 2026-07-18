package rpg.serverutil.paper.scoreboard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;
import rpg.serverutil.api.ScoreboardApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

public final class ScoreboardModule implements ServerUtilModule, Listener {

    private OreliaServerUtilPlugin plugin;
    private ScoreboardManager manager;
    private BukkitTask tickTask;

    @Override
    public String getName() {
        return "scoreboard";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfigManager().get("config.yml").get();
        String title = config.getString("scoreboard.title", "&6Orelia");
        long intervalTicks = config.getLong("scoreboard.update-interval-ticks", 20L);
        boolean hideNumbers = config.getBoolean("scoreboard.hide-numbers", false);

        this.manager = new ScoreboardManager(title, hideNumbers);
        manager.registerProvider(new ConfigScoreboardLineProvider(plugin));
        plugin.getServer().getServicesManager().register(ScoreboardApi.class, manager, plugin, ServicePriority.Normal);
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReload() {
        var config = plugin.getConfigManager().get("config.yml").get();
        manager.updateSettings(
                config.getString("scoreboard.title", "&6Orelia"),
                config.getBoolean("scoreboard.hide-numbers", false));

        long intervalTicks = config.getLong("scoreboard.update-interval-ticks", 20L);
        tickTask.cancel();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::tick, intervalTicks, intervalTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer());
    }

    public ScoreboardManager getManager() {
        return manager;
    }
}
