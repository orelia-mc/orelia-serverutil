package rpg.serverutil.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import rpg.serverutil.velocity.bridge.PaperMessageListener;
import rpg.serverutil.velocity.bridge.ServerSwitchListener;
import rpg.serverutil.velocity.config.VelocityConfig;

import java.nio.file.Path;

/**
 * Plugin entry point for orelia-serverutil's Velocity module: handles hub-transfer requests
 * from Paper backends and pushes best-effort server-switch notifications back to them.
 */
@Plugin(
        id = "orelia-serverutil",
        name = "OreliaServerUtil",
        version = "1.0.0",
        description = "Orelia server utility plugin - hub transfer and server-switch notifications",
        authors = {"orelia-mc"}
)
public final class OreliaServerUtilVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private VelocityConfig config;

    @Inject
    public OreliaServerUtilVelocityPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = VelocityConfig.load(dataDirectory, logger);

        MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from(config.channel());
        proxyServer.getChannelRegistrar().register(channel);
        proxyServer.getEventManager().register(this, new PaperMessageListener(channel, proxyServer, config, logger));
        proxyServer.getEventManager().register(this, new ServerSwitchListener(channel, proxyServer, this, logger));

        logger.info("OreliaServerUtil(Velocity) initialized. channel={}, hub-server={}. "
                + "Each backend Paper server also needs velocity.enabled: true and a matching \"channel\" to receive these.",
                config.channel(), config.hubServerName());
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public VelocityConfig getConfig() {
        return config;
    }
}
