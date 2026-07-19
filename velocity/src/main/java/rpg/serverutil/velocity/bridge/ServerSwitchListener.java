package rpg.serverutil.velocity.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import rpg.serverutil.common.protocol.ProtocolCodec;
import rpg.serverutil.common.protocol.ServerSwitchNotify;

import java.util.UUID;

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
 */
public final class ServerSwitchListener {

    private final ChannelIdentifier channel;

    public ServerSwitchListener(ChannelIdentifier channel) {
        this.channel = channel;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String fromServer = event.getPreviousServer().map(server -> server.getServerInfo().getName()).orElse(null);
        String toServer = event.getServer().getServerInfo().getName();
        ServerSwitchNotify notify = new ServerSwitchNotify(playerId, fromServer, toServer);

        ServerConnection arrivalConnection = event.getPlayer().getCurrentServer().orElse(null);
        if (arrivalConnection != null) {
            arrivalConnection.sendPluginMessage(channel, ProtocolCodec.encodeServerSwitchNotify(notify));
        }

        event.getPreviousServer().ifPresent(previousServer -> sendLeaveNotify(previousServer, playerId, notify));
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
            carrier.getCurrentServer().ifPresent(connection ->
                    connection.sendPluginMessage(channel, ProtocolCodec.encodeServerSwitchLeaveNotify(notify)));
            return;
        }
    }
}
