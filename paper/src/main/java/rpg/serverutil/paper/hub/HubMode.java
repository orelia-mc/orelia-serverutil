package rpg.serverutil.paper.hub;

/** {@code hub.mode} in config.yml - how {@code /hub} sends a player to the hub. */
public enum HubMode {
    /** Teleport within this server to {@code hub.teleport.*} coordinates. */
    TELEPORT,
    /** Request a Velocity transfer via {@code VelocityBridgeModule} (phase 6). */
    PROXY
}
