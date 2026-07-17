package rpg.serverutil.common.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

/**
 * Encodes/decodes payloads sent over the {@code orelia:serverutil} plugin messaging channel.
 * Uses the Guava {@code ByteArrayDataOutput}/{@code ByteArrayDataInput} both Velocity and
 * Paper bundle, same convention as MultiAccount's {@code ProtocolCodec} - this module stays
 * free of Bukkit/Velocity API imports so it can be shared by both platform modules.
 */
public final class ProtocolCodec {

    private ProtocolCodec() {
    }

    public static byte[] encodeHubTransferRequest(HubTransferRequest request) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.HUB_TRANSFER_REQUEST.name());
        writeUuid(out, request.correlationId());
        return out.toByteArray();
    }

    public static byte[] encodeHubTransferResult(HubTransferResult result) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.HUB_TRANSFER_RESULT.name());
        writeUuid(out, result.correlationId());
        out.writeBoolean(result.success());
        out.writeUTF(result.reasonKey());
        return out.toByteArray();
    }

    public static byte[] encodeServerSwitchNotify(ServerSwitchNotify notify) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.SERVER_SWITCH_NOTIFY.name());
        writeUuid(out, notify.playerId());
        out.writeBoolean(notify.fromServer() != null);
        if (notify.fromServer() != null) {
            out.writeUTF(notify.fromServer());
        }
        out.writeUTF(notify.toServer());
        return out.toByteArray();
    }

    public static MessageType peekType(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        return MessageType.valueOf(in.readUTF());
    }

    public static HubTransferRequest decodeHubTransferRequest(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        requireType(in, MessageType.HUB_TRANSFER_REQUEST);
        return new HubTransferRequest(readUuid(in));
    }

    public static HubTransferResult decodeHubTransferResult(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        requireType(in, MessageType.HUB_TRANSFER_RESULT);
        UUID correlationId = readUuid(in);
        boolean success = in.readBoolean();
        String reasonKey = in.readUTF();
        return new HubTransferResult(correlationId, success, reasonKey);
    }

    public static ServerSwitchNotify decodeServerSwitchNotify(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        requireType(in, MessageType.SERVER_SWITCH_NOTIFY);
        UUID playerId = readUuid(in);
        boolean hasFromServer = in.readBoolean();
        String fromServer = hasFromServer ? in.readUTF() : null;
        String toServer = in.readUTF();
        return new ServerSwitchNotify(playerId, fromServer, toServer);
    }

    private static void requireType(ByteArrayDataInput in, MessageType expected) {
        MessageType actual = MessageType.valueOf(in.readUTF());
        if (actual != expected) {
            throw new IllegalArgumentException("Expected " + expected + " payload, got " + actual);
        }
    }

    private static void writeUuid(ByteArrayDataOutput out, UUID uuid) {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(ByteArrayDataInput in) {
        long most = in.readLong();
        long least = in.readLong();
        return new UUID(most, least);
    }
}
