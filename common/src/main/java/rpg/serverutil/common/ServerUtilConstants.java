package rpg.serverutil.common;

/** Shared defaults between the paper/velocity modules. Keep both sides referencing this. */
public final class ServerUtilConstants {

    public static final String CHANNEL = "orelia:serverutil";
    public static final int DEFAULT_HUB_REQUEST_TIMEOUT_SECONDS = 5;

    private ServerUtilConstants() {
    }
}
