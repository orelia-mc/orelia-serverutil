package rpg.serverutil.paper.hub;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.bridge.VelocityBridgeModule;
import rpg.serverutil.paper.config.ConfigFile;

/**
 * {@code /hub}'s actual behavior, driven by {@code hub.mode} in config.yml. TELEPORT works
 * standalone (no other server required); PROXY needs {@code VelocityBridgeModule} wired up
 * in phase 6 - until then it reports itself as unavailable rather than failing silently.
 */
public final class HubService {

    private final OreliaServerUtilPlugin plugin;

    public HubService(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendToHub(Player player) {
        ConfigFile configFile = plugin.getConfigManager().get("config.yml");
        HubMode mode = parseMode(configFile.get().getString("hub.mode", "TELEPORT"));

        switch (mode) {
            case TELEPORT -> teleport(player, configFile);
            case PROXY -> proxy(player);
        }
    }

    private HubMode parseMode(String raw) {
        try {
            return HubMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HubMode.TELEPORT;
        }
    }

    private void teleport(Player player, ConfigFile configFile) {
        ConfigurationSection section = configFile.get().getConfigurationSection("hub.teleport");
        if (section == null) {
            plugin.getMessageManager().send(player, "hub.not-configured");
            return;
        }
        String worldName = section.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getMessageManager().send(player, "hub.world-not-found", "world", worldName);
            return;
        }
        double x = section.getDouble("x", 0.5);
        double y = section.getDouble("y", 100.0);
        double z = section.getDouble("z", 0.5);
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        player.teleportAsync(new Location(world, x, y, z, yaw, pitch));
        plugin.getMessageManager().send(player, "hub.teleported");
    }

    private void proxy(Player player) {
        plugin.getModuleManager().get(VelocityBridgeModule.class)
                .filter(VelocityBridgeModule::isEnabled)
                .ifPresentOrElse(
                        bridge -> bridge.sendHubRequest(player, result -> {
                            if (!result.success()) {
                                plugin.getMessageManager().send(player, "hub.proxy-failed");
                            }
                            // success: Velocity already transferred the player, nothing more to do here.
                        }),
                        () -> plugin.getMessageManager().send(player, "hub.proxy-not-available"));
    }
}
