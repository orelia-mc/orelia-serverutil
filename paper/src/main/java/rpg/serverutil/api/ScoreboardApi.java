package rpg.serverutil.api;

/**
 * Cross-plugin surface published via Bukkit's {@code ServicesManager} so other plugins
 * (orelia-core, orelia-extra, ...) can plug lines into the sidebar scoreboard without
 * reaching into {@code rpg.serverutil.paper.scoreboard} internals.
 */
public interface ScoreboardApi {

    void registerProvider(ScoreboardLineProvider provider);

    void unregisterProvider(String providerId);
}
