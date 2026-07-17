package rpg.serverutil.api;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * One block of sidebar lines. Providers are merged highest-{@link #getPriority()}-first;
 * within a single provider, lines keep the order returned by {@link #getLines(Player)}.
 */
public interface ScoreboardLineProvider {

    /** Stable id, used to {@link ScoreboardApi#unregisterProvider(String) unregister} later. */
    String getId();

    /** Higher priority renders first (nearer the top of the sidebar). */
    int getPriority();

    /** Lines to show this player right now. An empty list contributes nothing this render. */
    List<String> getLines(Player player);
}
