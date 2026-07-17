package rpg.serverutil.common.protocol;

import java.util.UUID;

/** Velocity -&gt; Paper: outcome of a {@link HubTransferRequest}. */
public record HubTransferResult(UUID correlationId, boolean success, String reasonKey) {

    public static HubTransferResult ok(UUID correlationId) {
        return new HubTransferResult(correlationId, true, "");
    }

    public static HubTransferResult failure(UUID correlationId, String reasonKey) {
        return new HubTransferResult(correlationId, false, reasonKey);
    }
}
