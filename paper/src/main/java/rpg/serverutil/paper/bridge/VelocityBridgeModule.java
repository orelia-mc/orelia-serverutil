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
import rpg.serverutil.paper.util.ColorUtil;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Paper -&gt; Velocity command bridge for {@code hub.mode: PROXY}, plus best-effort display of
 * incoming {@code SERVER_SWITCH_NOTIFY} titles. Only registers its plugin messaging channels
 * when {@code velocity.enabled} is true in config.yml - the plugin messaging channel requires
 * an online {@link Player} to send through, so console-triggered hub requests aren't
 * supported (same limitation as MultiAccount's {@code PaperCommandBridge}).
 */
public final class VelocityBridgeModule implements ServerUtilModule, PluginMessageListener {

    private record PendingRequest(Consumer<HubTransferResult> callback, BukkitTask timeoutTask) {
    }

    private OreliaServerUtilPlugin plugin;
    private boolean enabled;
    private String channel;
    private final Map<UUID, PendingRequest> pending = new ConcurrentHashMap<>();
    // Populated by incoming SERVER_SWITCH_(LEAVE_)NOTIFY, consumed by JoinMessageModule's
    // delayed join/quit broadcast so it can tell a network switch apart from a fresh login/a
    // true disconnect (see the plan's Phase 6 design notes).
    private final Map<UUID, ServerSwitchNotify> pendingArrivals = new ConcurrentHashMap<>();
    private final Map<UUID, ServerSwitchNotify> pendingDepartures = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "velocity-bridge";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfigManager().get("config.yml").get();
        this.enabled = config.getBoolean("velocity.enabled", false);
        this.channel = config.getString("velocity.channel", ServerUtilConstants.CHANNEL);
        if (!enabled) {
            return;
        }
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public void onDisable() {
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

    /** Consumed once by {@code JoinMessageModule}'s delayed join broadcast; empty if none arrived in time. */
    public Optional<ServerSwitchNotify> consumeArrival(UUID playerId) {
        return Optional.ofNullable(pendingArrivals.remove(playerId));
    }

    /** Consumed once by {@code JoinMessageModule}'s delayed quit broadcast; empty if none arrived in time. */
    public Optional<ServerSwitchNotify> consumeDeparture(UUID playerId) {
        return Optional.ofNullable(pendingDepartures.remove(playerId));
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
        pendingArrivals.put(notify.playerId(), notify);
        Bukkit.getScheduler().runTask(plugin, () -> showSwitchTitle(notify));
    }

    private void handleServerSwitchLeaveNotify(byte[] message) {
        ServerSwitchNotify notify = ProtocolCodec.decodeServerSwitchLeaveNotify(message);
        pendingDepartures.put(notify.playerId(), notify);
    }

    private void showSwitchTitle(ServerSwitchNotify notify) {
        var config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("server-switch-notify.enabled", true)) {
            return;
        }
        Player target = Bukkit.getPlayer(notify.playerId());
        if (target == null) {
            return;
        }
        String title = ColorUtil.colorize(config.getString("server-switch-notify.title", ""));
        String subtitle = ColorUtil.colorize(config.getString("server-switch-notify.subtitle", "")
                .replace("{server}", notify.toServer()));
        target.sendTitle(title, subtitle, 10, 40, 10);
    }
}
