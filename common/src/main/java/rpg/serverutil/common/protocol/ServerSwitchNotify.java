package rpg.serverutil.common.protocol;

import java.util.UUID;

/**
 * Velocity -&gt; Paper: best-effort notification that a player just switched backend servers,
 * so the destination Paper server can show a title/announce. {@code fromServer} is
 * {@code null} on a player's very first connection (no prior server).
 */
public record ServerSwitchNotify(UUID playerId, String fromServer, String toServer) {
}
