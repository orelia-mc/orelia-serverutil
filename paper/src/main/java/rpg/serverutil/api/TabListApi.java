package rpg.serverutil.api;

/**
 * Cross-plugin surface published via Bukkit's {@code ServicesManager} so other plugins can
 * customize tab-list name prefix/suffix/color (job/guild tags, ...) and the value shown on
 * the right side of a tab-list entry, without reaching into {@code rpg.serverutil.paper.tablist}
 * internals. These are two independent mechanisms under one API (name decoration via
 * scoreboard Team, value via a PLAYER_LIST-slot Objective) - see {@code TabListManager}.
 */
public interface TabListApi {

    void registerFormatter(TabListNameFormatter formatter);

    void unregisterFormatter(String formatterId);

    void registerValueProvider(TabListValueProvider provider);

    void unregisterValueProvider(String providerId);
}
