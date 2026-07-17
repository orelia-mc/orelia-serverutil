package rpg.serverutil.velocity.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import rpg.serverutil.common.protocol.HubTransferRequest;
import rpg.serverutil.common.protocol.HubTransferResult;
import rpg.serverutil.common.protocol.ProtocolCodec;
import rpg.serverutil.velocity.config.VelocityConfig;

import java.util.Optional;

/**
 * Handles {@code HUB_TRANSFER_REQUEST} from a Paper backend: transfers the requesting player
 * to {@code hub.server-name} (decided here, never by the Paper payload - see
 * {@link HubTransferRequest}'s Javadoc) and replies with a {@code HUB_TRANSFER_RESULT}.
 */
public final class PaperMessageListener {

    private final ChannelIdentifier channel;
    private final ProxyServer proxyServer;
    private final VelocityConfig config;
    private final Logger logger;

    public PaperMessageListener(ChannelIdentifier channel, ProxyServer proxyServer, VelocityConfig config, Logger logger) {
        this.channel = channel;
        this.proxyServer = proxyServer;
        this.config = config;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel) || !(event.getSource() instanceof ServerConnection serverConnection)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        HubTransferRequest request;
        try {
            request = ProtocolCodec.decodeHubTransferRequest(event.getData());
        } catch (Exception e) {
            logger.warn("Failed to decode HubTransferRequest: {}", e.getMessage());
            return;
        }

        // Re-derive the sender's identity from the connection rather than trusting anything
        // self-reported in the payload (same anti-spoofing pattern as MultiAccount).
        Player player = serverConnection.getPlayer();

        Optional<RegisteredServer> hub = proxyServer.getServer(config.hubServerName());
        if (hub.isEmpty()) {
            logger.warn("hub.server-name '{}' is not a registered server - {}", config.hubServerName(), config.hubFallbackMessage());
            serverConnection.sendPluginMessage(channel, ProtocolCodec.encodeHubTransferResult(
                    HubTransferResult.failure(request.correlationId(), "hub.server-not-found")));
            return;
        }

        player.createConnectionRequest(hub.get()).connectWithIndication().thenAccept(success -> {
            HubTransferResult result = success
                    ? HubTransferResult.ok(request.correlationId())
                    : HubTransferResult.failure(request.correlationId(), "hub.connect-failed");
            serverConnection.sendPluginMessage(channel, ProtocolCodec.encodeHubTransferResult(result));
        });
    }
}
