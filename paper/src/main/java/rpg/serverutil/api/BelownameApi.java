package rpg.serverutil.api;

/**
 * Cross-plugin surface published via Bukkit's {@code ServicesManager} so other plugins can
 * feed the text shown below a player's nametag (health, faction, ...) without reaching into
 * {@code rpg.serverutil.paper.belowname} internals.
 */
public interface BelownameApi {

    void registerProvider(BelownameValueProvider provider);

    void unregisterProvider(String providerId);
}
