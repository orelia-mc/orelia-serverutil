package rpg.serverutil.api;

/**
 * Cross-plugin surface published via Bukkit's {@code ServicesManager} so other plugins can
 * customize tab-list name prefix/suffix (job/guild tags, ...) without reaching into
 * {@code rpg.serverutil.paper.tablist} internals.
 */
public interface TabListApi {

    void registerFormatter(TabListNameFormatter formatter);

    void unregisterFormatter(String formatterId);
}
