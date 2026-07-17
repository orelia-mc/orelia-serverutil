package rpg.serverutil.common.protocol;

import java.util.UUID;

/**
 * Paper -&gt; Velocity: asks Velocity to transfer the requesting player to the hub server.
 * Deliberately does not carry a destination server name - Velocity's own config decides
 * where "hub" points to, so a compromised/misconfigured Paper backend can't redirect
 * players anywhere else.
 */
public record HubTransferRequest(UUID correlationId) {
}
