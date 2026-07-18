package rpg.serverutil.paper.announce;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;
import rpg.serverutil.paper.util.ColorUtil;

/** Sends a config-driven welcome announcement (chat lines + optional title) shortly after join. */
public final class AnnounceModule implements ServerUtilModule, Listener {

    private OreliaServerUtilPlugin plugin;

    @Override
    public String getName() {
        return "announce";
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
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("announce.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        long delayTicks = config.getLong("announce.delay-ticks", 20L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> announce(player, config), delayTicks);
    }

    private void announce(Player player, YamlConfiguration config) {
        if (!player.isOnline()) {
            return;
        }
        var placeholders = plugin.getPlaceholderService();
        for (String line : config.getStringList("announce.lines")) {
            player.sendMessage(ColorUtil.colorize(placeholders.resolve(line, player)));
        }
        if (config.getBoolean("announce.title.enabled", false)) {
            String title = ColorUtil.colorize(placeholders.resolve(config.getString("announce.title.title", ""), player));
            String subtitle = ColorUtil.colorize(placeholders.resolve(config.getString("announce.title.subtitle", ""), player));
            player.sendTitle(title, subtitle, 10, 60, 10);
        }
    }
}
