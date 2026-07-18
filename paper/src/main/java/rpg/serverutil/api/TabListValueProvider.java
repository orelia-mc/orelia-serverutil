package rpg.serverutil.api;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Providers are tried highest-{@link #getPriority()}-first; the first non-empty result wins.
 * The resolved value is written into the same per-viewer scoreboard as {@code prefix}/
 * {@code suffix}, so different viewers can see different values for the same target (see
 * {@code TabListManager}).
 */
public interface TabListValueProvider {

    /** Stable id, used to {@link TabListApi#unregisterValueProvider(String) unregister} later. */
    String getId();

    int getPriority();

    /** Empty if this provider has nothing to say about {@code target} right now. */
    Optional<String> getValue(Player target);
}
