package rpg.serverutil.paper.healthcheck;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

import java.util.List;
import java.util.stream.Collectors;

/** Shows operators a quick TPS/online-count summary on join, so issues surface immediately. */
public final class HealthCheckModule implements ServerUtilModule, Listener {

    private static final List<String> ORELIA_PLUGIN_NAMES =
            List.of("OreliaCore", "OreliaWorld", "OreliaExtra", "OreliaDebug");

    private OreliaServerUtilPlugin plugin;

    @Override
    public String getName() {
        return "admin-healthcheck";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("admin-healthcheck.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        double tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        int online = Bukkit.getOnlinePlayers().size();
        plugin.getMessageManager().send(player, "healthcheck.summary",
                "tps", String.format("%.1f", tps), "online", online);

        if (config.getBoolean("core-integration.enabled", true)) {
            plugin.getMessageManager().send(player, "healthcheck.orelia-plugins",
                    "plugins", describeOreliaPlugins());
        }
    }

    private String describeOreliaPlugins() {
        return ORELIA_PLUGIN_NAMES.stream()
                .map(this::describePlugin)
                .collect(Collectors.joining("&%7, "));
    }

    private String describePlugin(String name) {
        Plugin found = Bukkit.getPluginManager().getPlugin(name);
        return found != null
                ? "&%a" + name + " &%7v" + found.getPluginMeta().getVersion()
                : "&%8" + name + " (未導入)";
    }
}
