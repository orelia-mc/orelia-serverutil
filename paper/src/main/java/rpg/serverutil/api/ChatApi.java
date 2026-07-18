package rpg.serverutil.api;

/**
 * Cross-plugin surface published via Bukkit's {@code ServicesManager} so other plugins can
 * feed a chat placeholder (level, title, rank, ...) shown next to the sender's name, without
 * reaching into {@code rpg.serverutil.paper.chat} internals.
 */
public interface ChatApi {

    void registerProvider(ChatPlaceholderProvider provider);

    void unregisterProvider(String providerId);
}
