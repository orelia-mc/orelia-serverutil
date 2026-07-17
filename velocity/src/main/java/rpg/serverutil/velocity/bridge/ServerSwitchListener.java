package rpg.serverutil.velocity.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import rpg.serverutil.common.protocol.ProtocolCodec;
import rpg.serverutil.common.protocol.ServerSwitchNotify;
import rpg.serverutil.velocity.config.VelocityConfig;

/**
 * Best-effort notification to the destination Paper server whenever a player switches
 * backend servers, so it can show a title/announce. Purely cosmetic - a dropped message here
 * (e.g. destination server hasn't registered the channel yet) is not treated as an error.
 */
public final class ServerSwitchListener {

    private final ChannelIdentifier channel;
    private final VelocityConfig config;

    public ServerSwitchListener(ChannelIdentifier channel, VelocityConfig config) {
        this.channel = channel;
        this.config = config;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (!config.serverSwitchNotifyEnabled()) {
            return;
        }
        ServerConnection connection = event.getPlayer().getCurrentServer().orElse(null);
        if (connection == null) {
            return;
        }
        String fromServer = event.getPreviousServer().map(server -> server.getServerInfo().getName()).orElse(null);
        String toServer = event.getServer().getServerInfo().getName();
        ServerSwitchNotify notify = new ServerSwitchNotify(event.getPlayer().getUniqueId(), fromServer, toServer);
        connection.sendPluginMessage(channel, ProtocolCodec.encodeServerSwitchNotify(notify));
    }
}
