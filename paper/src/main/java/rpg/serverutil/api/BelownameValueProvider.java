package rpg.serverutil.api;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Providers are tried highest-{@link #getPriority()}-first; the first non-empty result wins.
 * The resolved value is the same for every viewer (belowname title/value is not per-viewer
 * data in vanilla Minecraft - see {@code BelownameManager}), so implementations should only
 * depend on {@code target}'s own state, not on who's looking.
 */
public interface BelownameValueProvider {

    /** Stable id, used to {@link BelownameApi#unregisterProvider(String) unregister} later. */
    String getId();

    int getPriority();

    /** Empty if this provider has nothing to say about {@code target} right now. */
    Optional<String> getValue(Player target);
}
