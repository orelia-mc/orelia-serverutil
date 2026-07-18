package rpg.serverutil.api;

import org.bukkit.entity.Player;

import java.util.Optional;

/** Providers are tried highest-{@link #getPriority()}-first; the first non-empty result wins. */
public interface ChatPlaceholderProvider {

    /** Stable id, used to {@link ChatApi#unregisterProvider(String) unregister} later. */
    String getId();

    int getPriority();

    /** Empty if this provider has nothing to say about {@code sender} right now. */
    Optional<String> getPlaceholder(Player sender);
}
