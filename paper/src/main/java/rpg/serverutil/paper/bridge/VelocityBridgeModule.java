package rpg.serverutil.paper.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import rpg.serverutil.common.ServerUtilConstants;
import rpg.serverutil.common.protocol.HubTransferRequest;
import rpg.serverutil.common.protocol.HubTransferResult;
import rpg.serverutil.common.protocol.MessageType;
import rpg.serverutil.common.protocol.ProtocolCodec;
import rpg.serverutil.common.protocol.ServerSwitchNotify;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Paper -&gt; Velocity command bridge for {@code hub.mode: PROXY}, plus the receiving side of
 * incoming {@code SERVER_SWITCH_(LEAVE_)NOTIFY} that {@code JoinMessageModule} uses for its
 * server-switch join/leave chat messages. Only registers its plugin messaging channels when
 * {@code velocity.enabled} is true in config.yml - the plugin messaging channel requires an
 * online {@link Player} to send through, so console-triggered hub requests aren't supported
 * (same limitation as MultiAccount's {@code PaperCommandBridge}).
 */
public final class VelocityBridgeModule implements ServerUtilModule, PluginMessageListener {

    private record PendingRequest(Consumer<HubTransferResult> callback, BukkitTask timeoutTask) {
    }

    private OreliaServerUtilPlugin plugin;
    private boolean enabled;
    private String channel;
    private final Map<UUID, PendingRequest> pending = new ConcurrentHashMap<>();
    // SERVER_SWITCH_(LEAVE_)NOTIFY and the matching PlayerJoinEvent/PlayerQuitEvent race each
    // other - either can arrive first, since one comes from Velocity over the network and the
    // other from Paper's own login sequence. Whichever arrives first stashes itself here;
    // whichever arrives second (or the timeout in awaitArrival/awaitDeparture) consumes it. This
    // used to be a fixed-delay "wait N ticks then check" in JoinMessageModule, which lost the
    // race whenever the notify took longer than that delay to arrive - see awaitArrival/
    // awaitDeparture below for the fix.
    private final Map<UUID, ServerSwitchNotify> pendingArrivals = new ConcurrentHashMap<>();
    private final Map<UUID, ServerSwitchNotify> pendingDepartures = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<ServerSwitchNotify>> arrivalWaiters = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<ServerSwitchNotify>> departureWaiters = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "velocity-bridge";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        applyConfig();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReload() {
        if (enabled) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        }
        applyConfig();
    }

    private void applyConfig() {
        var config = plugin.getConfigManager().get("config.yml").get();
        this.enabled = config.getBoolean("velocity.enabled", false);
        this.channel = config.getString("velocity.channel", ServerUtilConstants.CHANNEL);
        if (!enabled) {
            // Silently doing nothing here made hub.mode: PROXY and server-switch join/leave
            // messages look "broken" with zero diagnostic trail in past reports - log it
            // explicitly instead.
            plugin.getLogger().info("velocity.enabled is false - /hub PROXY mode and "
                    + "server-switch join/leave messages are disabled. Set velocity.enabled: "
                    + "true (and make sure \"channel\" matches the Velocity side's config.yml) "
                    + "to use them.");
            return;
        }
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        plugin.getLogger().info("Velocity bridge enabled on channel \"" + channel + "\".");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Sends a hub transfer request via {@code carrier}'s connection; {@code callback} runs on the main thread. */
    public void sendHubRequest(Player carrier, Consumer<HubTransferResult> callback) {
        UUID correlationId = UUID.randomUUID();
        int timeoutSeconds = plugin.getConfigManager().get("config.yml").get()
                .getInt("hub.proxy.request-timeout-seconds", ServerUtilConstants.DEFAULT_HUB_REQUEST_TIMEOUT_SECONDS);

        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingRequest removed = pending.remove(correlationId);
            if (removed != null) {
                removed.callback().accept(HubTransferResult.failure(correlationId, "hub.request-timeout"));
            }
        }, timeoutSeconds * 20L);

        pending.put(correlationId, new PendingRequest(callback, timeoutTask));
        carrier.sendPluginMessage(plugin, channel,
                ProtocolCodec.encodeHubTransferRequest(new HubTransferRequest(correlationId)));
    }

    @Override
    public void onPluginMessageReceived(String receivedChannel, Player player, byte[] message) {
        if (!receivedChannel.equals(channel)) {
            return;
        }
        MessageType type;
        try {
            type = ProtocolCodec.peekType(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to decode orelia:serverutil payload: " + e.getMessage());
            return;
        }
        switch (type) {
            case HUB_TRANSFER_RESULT -> handleHubTransferResult(message);
            case SERVER_SWITCH_NOTIFY -> handleServerSwitchNotify(message);
            case SERVER_SWITCH_LEAVE_NOTIFY -> handleServerSwitchLeaveNotify(message);
            default -> {
            }
        }
    }

    /**
     * Calls {@code onReady} with the matching notify the moment it's available - immediately if
     * SERVER_SWITCH_NOTIFY already arrived before this player's join was even processed, or as
     * soon as it arrives afterward. If nothing arrives within {@code timeoutTicks}, calls
     * {@code onReady} with {@code null} (plain join, not a network switch). Used by
     * {@code JoinMessageModule} instead of a fixed "wait then check" delay, which used to lose
     * the race whenever the notify was slower than the wait.
     */
    public void awaitArrival(UUID playerId, Consumer<ServerSwitchNotify> onReady, long timeoutTicks) {
        ServerSwitchNotify existing = pendingArrivals.remove(playerId);
        if (existing != null) {
            onReady.accept(existing);
            return;
        }
        arrivalWaiters.put(playerId, onReady);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Consumer<ServerSwitchNotify> waiter = arrivalWaiters.remove(playerId);
            if (waiter != null) {
                plugin.getLogger().info("No SERVER_SWITCH_NOTIFY arrived for " + playerId
                        + " within " + timeoutTicks + " ticks - falling back to the plain join message.");
                waiter.accept(null);
            }
        }, timeoutTicks);
    }

    /** Same as {@link #awaitArrival} but for SERVER_SWITCH_LEAVE_NOTIFY / the quitting player's departure. */
    public void awaitDeparture(UUID playerId, Consumer<ServerSwitchNotify> onReady, long timeoutTicks) {
        ServerSwitchNotify existing = pendingDepartures.remove(playerId);
        if (existing != null) {
            onReady.accept(existing);
            return;
        }
        departureWaiters.put(playerId, onReady);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Consumer<ServerSwitchNotify> waiter = departureWaiters.remove(playerId);
            if (waiter != null) {
                waiter.accept(null);
            }
        }, timeoutTicks);
    }

    private void handleHubTransferResult(byte[] message) {
        HubTransferResult result = ProtocolCodec.decodeHubTransferResult(message);
        PendingRequest request = pending.remove(result.correlationId());
        if (request == null) {
            return;
        }
        request.timeoutTask().cancel();
        Bukkit.getScheduler().runTask(plugin, () -> request.callback().accept(result));
    }

    private void handleServerSwitchNotify(byte[] message) {
        ServerSwitchNotify notify = ProtocolCodec.decodeServerSwitchNotify(message);
        plugin.getLogger().info("Received SERVER_SWITCH_NOTIFY for " + notify.playerId()
                + " (from=" + notify.fromServer() + ", to=" + notify.toServer() + ").");
        Bukkit.getScheduler().runTask(plugin, () -> {
            Consumer<ServerSwitchNotify> waiter = arrivalWaiters.remove(notify.playerId());
            if (waiter != null) {
                waiter.accept(notify);
            } else {
                pendingArrivals.put(notify.playerId(), notify);
            }
        });
    }

    private void handleServerSwitchLeaveNotify(byte[] message) {
        ServerSwitchNotify notify = ProtocolCodec.decodeServerSwitchLeaveNotify(message);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Consumer<ServerSwitchNotify> waiter = departureWaiters.remove(notify.playerId());
            if (waiter != null) {
                waiter.accept(notify);
            } else {
                pendingDepartures.put(notify.playerId(), notify);
            }
        });
    }
}
