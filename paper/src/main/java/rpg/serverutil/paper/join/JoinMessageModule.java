package rpg.serverutil.paper.join;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.message.MessageManager;
import rpg.serverutil.paper.module.ServerUtilModule;

/** Customizes join/quit chat messages, distinguishing a player's very first join. */
public final class JoinMessageModule implements ServerUtilModule, Listener {

    private OreliaServerUtilPlugin plugin;

    @Override
    public String getName() {
        return "join-message";
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
        MessageManager messages = plugin.getMessageManager();
        String key = event.getPlayer().hasPlayedBefore() ? "join.join-message" : "join.first-join-message";
        event.setJoinMessage(messages.format(key, "player", event.getPlayer().getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(plugin.getMessageManager().format("join.quit-message", "player", event.getPlayer().getName()));
    }
}
