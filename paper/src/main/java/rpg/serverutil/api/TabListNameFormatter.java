package rpg.serverutil.api;

import org.bukkit.entity.Player;

import java.util.Optional;

/** Formatters are tried highest-{@link #getPriority()}-first; the first non-empty result wins. */
public interface TabListNameFormatter {

    /** Stable id, used to {@link TabListApi#unregisterFormatter(String) unregister} later. */
    String getId();

    int getPriority();

    /** Empty if this formatter has nothing to say about {@code player} right now. */
    Optional<TabListEntry> format(Player player);
}
