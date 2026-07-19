package rpg.serverutil.velocity.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import rpg.serverutil.common.protocol.ProtocolCodec;
import rpg.serverutil.common.protocol.ServerSwitchNotify;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Best-effort notification whenever a player switches backend servers: the destination server
 * gets an arrival notify (join-format chat message), and - if anyone else is still connected to
 * it - the source server gets a departure notify (leave-format chat message). Always sent
 * whenever the underlying channel is available - there used to be a
 * {@code server-switch-notify.enabled} flag here, but it was gating this unconditionally (not
 * just the title popup Paper used to show on arrival), which silently broke the join/leave
 * chat messages for anyone who turned the flag off expecting it to only affect the title. The
 * title feature was removed instead of fixing that ambiguity - see Paper's
 * {@code VelocityBridgeModule} history. A dropped message here (e.g. a server hasn't registered
 * the channel yet, or nobody remains on the source server to relay it) is not treated as an
 * error.
 *
 * <p>The arrival notify is sent over the switching player's own connection to the destination
 * server, which was only just established when {@link ServerConnectedEvent} fires - unlike the
 * departure notify below, which rides an already-settled connection (another player who was
 * already on the source server). Sending immediately in the same tick as the switch completing
 * was observed to silently drop the arrival notify (destination server falls back to a plain
 * join message) far more often than the departure one, consistent with the brand-new connection
 * not being fully ready for plugin messages the instant the event fires - so this schedules the
 * arrival send a short moment later instead of inline.
 */
public final class ServerSwitchListener {

    private static final long ARRIVAL_SEND_DELAY_MILLIS = 250;

    private final ChannelIdentifier channel;
    private final ProxyServer proxyServer;
    private final Object plugin;
    private final Logger logger;

    public ServerSwitchListener(ChannelIdentifier channel, ProxyServer proxyServer, Object plugin, Logger logger) {
        this.channel = channel;
        this.proxyServer = proxyServer;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String fromServer = event.getPreviousServer().map(server -> server.getServerInfo().getName()).orElse(null);
        String toServer = event.getServer().getServerInfo().getName();
        ServerSwitchNotify notify = new ServerSwitchNotify(playerId, fromServer, toServer);

        proxyServer.getScheduler().buildTask(plugin, () -> sendArrivalNotify(event, notify))
                .delay(ARRIVAL_SEND_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .schedule();

        event.getPreviousServer().ifPresent(previousServer -> sendLeaveNotify(previousServer, playerId, notify));
    }

    private void sendArrivalNotify(ServerConnectedEvent event, ServerSwitchNotify notify) {
        event.getPlayer().getCurrentServer().ifPresentOrElse(
                connection -> {
                    connection.sendPluginMessage(channel, ProtocolCodec.encodeServerSwitchNotify(notify));
                    logger.info("Sent SERVER_SWITCH_NOTIFY for {} (from={}, to={}).",
                            notify.playerId(), notify.fromServer(), notify.toServer());
                },
                () -> logger.warn("Could not send SERVER_SWITCH_NOTIFY for {} - player no longer has a current "
                        + "server connection (disconnected during the {}ms delay?).",
                        notify.playerId(), ARRIVAL_SEND_DELAY_MILLIS));
    }

    /**
     * The player who left {@code previousServer} is no longer connected to it by the time this
     * event fires, so their own connection can't carry the message - relay it through any other
     * player still on that server instead (same "needs an online carrier" limitation as the hub
     * transfer bridge). If nobody remains there, nobody would see the leave message anyway.
     */
    private void sendLeaveNotify(RegisteredServer previousServer, UUID departedPlayerId, ServerSwitchNotify notify) {
        for (Player carrier : previousServer.getPlayersConnected()) {
            if (carrier.getUniqueId().equals(departedPlayerId)) {
                continue;
            }
            carrier.getCurrentServer().ifPresent(connection -> {
                connection.sendPluginMessage(channel, ProtocolCodec.encodeServerSwitchLeaveNotify(notify));
                logger.info("Sent SERVER_SWITCH_LEAVE_NOTIFY for {} via carrier {} (from={}, to={}).",
                        departedPlayerId, carrier.getUniqueId(), notify.fromServer(), notify.toServer());
            });
            return;
        }
    }
}
