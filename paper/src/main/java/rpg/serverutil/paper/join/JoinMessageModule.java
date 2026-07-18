package rpg.serverutil.paper.join;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rpg.serverutil.common.protocol.ServerSwitchNotify;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.bridge.VelocityBridgeModule;
import rpg.serverutil.paper.message.MessageManager;
import rpg.serverutil.paper.module.ServerUtilModule;

/**
 * Customizes join/quit chat messages, distinguishing a player's very first join. When behind a
 * Velocity proxy running OreliaServerUtil(Velocity), a network server switch instead gets the
 * "{player} | {from} -> {to}" format: the vanilla message is suppressed, and
 * {@link VelocityBridgeModule#awaitArrival}/{@code awaitDeparture} broadcast it the moment the
 * matching SERVER_SWITCH_(LEAVE_)NOTIFY is available - immediately if it already arrived, or as
 * soon as it does, falling back to the normal join/quit text only if nothing arrives within
 * {@code join.server-switch-wait-ticks} (fresh login / full disconnect from the proxy, or
 * velocity.enabled is off, in which case this behaves exactly as before with zero delay).
 */
public final class JoinMessageModule implements ServerUtilModule, Listener {

    private OreliaServerUtilPlugin plugin;
    private VelocityBridgeModule velocityBridge;

    @Override
    public String getName() {
        return "join-message";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        this.velocityBridge = plugin.getModuleManager().get(VelocityBridgeModule.class).orElse(null);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (velocityBridge == null || !velocityBridge.isEnabled()) {
            event.setJoinMessage(plainJoinMessage(event.getPlayer()));
            return;
        }
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        velocityBridge.awaitArrival(player.getUniqueId(), notify -> broadcastJoin(player, notify), waitTicks());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (velocityBridge == null || !velocityBridge.isEnabled()) {
            event.setQuitMessage(plainQuitMessage(event.getPlayer()));
            return;
        }
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        velocityBridge.awaitDeparture(player.getUniqueId(), notify -> broadcastQuit(player, notify), waitTicks());
    }

    private void broadcastJoin(Player player, ServerSwitchNotify notify) {
        String message = notify != null && notify.fromServer() != null
                ? plugin.getMessageManager().format("join.server-switch-message",
                        "player", player.getName(), "from", notify.fromServer(), "to", notify.toServer())
                : plainJoinMessage(player);
        Bukkit.broadcastMessage(message);
    }

    private void broadcastQuit(Player player, ServerSwitchNotify notify) {
        String message = notify != null
                ? plugin.getMessageManager().format("join.server-switch-leave-message",
                        "player", player.getName(), "from", notify.fromServer(), "to", notify.toServer())
                : plainQuitMessage(player);
        Bukkit.broadcastMessage(message);
    }

    private String plainJoinMessage(Player player) {
        MessageManager messages = plugin.getMessageManager();
        String key = player.hasPlayedBefore() ? "join.join-message" : "join.first-join-message";
        return messages.format(key, "player", player.getName());
    }

    private String plainQuitMessage(Player player) {
        return plugin.getMessageManager().format("join.quit-message", "player", player.getName());
    }

    private long waitTicks() {
        return plugin.getConfigManager().get("config.yml").get().getLong("join.server-switch-wait-ticks", 5L);
    }
}
