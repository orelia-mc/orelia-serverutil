package rpg.serverutil.common.protocol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Round-trip encode/decode checks - the only automated verification available for the
 * velocity module's wire format, since the Velocity API is compileOnly and can't be
 * exercised outside a running proxy.
 */
class ProtocolCodecTest {

    @Test
    void hubTransferRequestRoundTrips() {
        UUID correlationId = UUID.randomUUID();
        HubTransferRequest original = new HubTransferRequest(correlationId);

        byte[] encoded = ProtocolCodec.encodeHubTransferRequest(original);

        assertEquals(MessageType.HUB_TRANSFER_REQUEST, ProtocolCodec.peekType(encoded));
        HubTransferRequest decoded = ProtocolCodec.decodeHubTransferRequest(encoded);
        assertEquals(original, decoded);
    }

    @Test
    void hubTransferResultRoundTripsSuccess() {
        HubTransferResult original = HubTransferResult.ok(UUID.randomUUID());

        byte[] encoded = ProtocolCodec.encodeHubTransferResult(original);
        HubTransferResult decoded = ProtocolCodec.decodeHubTransferResult(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void hubTransferResultRoundTripsFailure() {
        HubTransferResult original = HubTransferResult.failure(UUID.randomUUID(), "hub.server-not-found");

        byte[] encoded = ProtocolCodec.encodeHubTransferResult(original);
        HubTransferResult decoded = ProtocolCodec.decodeHubTransferResult(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void serverSwitchNotifyRoundTripsWithFromServer() {
        ServerSwitchNotify original = new ServerSwitchNotify(UUID.randomUUID(), "survival", "hub");

        byte[] encoded = ProtocolCodec.encodeServerSwitchNotify(original);
        ServerSwitchNotify decoded = ProtocolCodec.decodeServerSwitchNotify(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void serverSwitchNotifyRoundTripsWithNullFromServer() {
        ServerSwitchNotify original = new ServerSwitchNotify(UUID.randomUUID(), null, "hub");

        byte[] encoded = ProtocolCodec.encodeServerSwitchNotify(original);
        ServerSwitchNotify decoded = ProtocolCodec.decodeServerSwitchNotify(encoded);

        assertNull(decoded.fromServer());
        assertEquals(original, decoded);
    }

    @Test
    void serverSwitchLeaveNotifyRoundTrips() {
        ServerSwitchNotify original = new ServerSwitchNotify(UUID.randomUUID(), "survival", "hub");

        byte[] encoded = ProtocolCodec.encodeServerSwitchLeaveNotify(original);

        assertEquals(MessageType.SERVER_SWITCH_LEAVE_NOTIFY, ProtocolCodec.peekType(encoded));
        ServerSwitchNotify decoded = ProtocolCodec.decodeServerSwitchLeaveNotify(encoded);
        assertEquals(original, decoded);
    }

    @Test
    void decodingWithWrongTypeThrows() {
        byte[] encoded = ProtocolCodec.encodeHubTransferRequest(new HubTransferRequest(UUID.randomUUID()));

        assertThrows(IllegalArgumentException.class, () -> ProtocolCodec.decodeHubTransferResult(encoded));
    }
}
