package rpg.serverutil.velocity.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import rpg.serverutil.common.ServerUtilConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * velocity/config.yml load result. Hand-rolled SnakeYAML loading (copy-bundled-resource-if-
 * absent, then {@code Yaml().load()} into a raw map with typed getters), same convention as
 * MultiAccount's {@code VelocityConfig} - Velocity has no equivalent of Bukkit's
 * {@code FileConfiguration}, so every Velocity-side plugin rolls its own.
 */
public final class VelocityConfig {

    private final String channel;
    private final String hubServerName;
    private final String hubFallbackMessage;
    private final int hubRequestTimeoutSeconds;

    private VelocityConfig(String channel, String hubServerName, String hubFallbackMessage,
                            int hubRequestTimeoutSeconds) {
        this.channel = channel;
        this.hubServerName = hubServerName;
        this.hubFallbackMessage = hubFallbackMessage;
        this.hubRequestTimeoutSeconds = hubRequestTimeoutSeconds;
    }

    @SuppressWarnings("unchecked")
    public static VelocityConfig load(Path dataDirectory, Logger logger) {
        Path configPath = dataDirectory.resolve("config.yml");
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(configPath)) {
                try (InputStream in = VelocityConfig.class.getResourceAsStream("/config.yml")) {
                    if (in == null) {
                        throw new IllegalStateException("Bundled config.yml resource not found");
                    }
                    Files.copy(in, configPath);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare config.yml", e);
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(configPath)) {
            root = (Map<String, Object>) new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.yml", e);
        }
        if (root == null) {
            root = Map.of();
        }

        String channel = asString(root.get("channel"), ServerUtilConstants.CHANNEL);

        Map<String, Object> hub = root.get("hub") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        String hubServerName = asString(hub.get("server-name"), "hub");
        String hubFallbackMessage = asString(hub.get("fallback-message"), "Hub server not found.");
        int hubRequestTimeoutSeconds = asInt(hub.get("request-timeout-seconds"),
                ServerUtilConstants.DEFAULT_HUB_REQUEST_TIMEOUT_SECONDS);

        return new VelocityConfig(channel, hubServerName, hubFallbackMessage, hubRequestTimeoutSeconds);
    }

    private static int asInt(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static String asString(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    public String channel() {
        return channel;
    }

    public String hubServerName() {
        return hubServerName;
    }

    public String hubFallbackMessage() {
        return hubFallbackMessage;
    }

    public int hubRequestTimeoutSeconds() {
        return hubRequestTimeoutSeconds;
    }
}
