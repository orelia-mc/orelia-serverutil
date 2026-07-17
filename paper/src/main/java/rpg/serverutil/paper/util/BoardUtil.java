package rpg.serverutil.paper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Shared by {@code ScoreboardManager} (sidebar) and {@code TabListManager} (name prefix/
 * suffix) so both features can layer their own objective/teams onto the *same* per-player
 * {@link Scoreboard} instance instead of stomping on each other by each calling
 * {@code player.setScoreboard(...)} with a fresh board.
 */
public final class BoardUtil {

    private BoardUtil() {
    }

    /**
     * Returns the player's current scoreboard if it's already a personal one (assigned by
     * this plugin), otherwise creates and assigns a fresh personal board. Never returns the
     * server's main scoreboard, so registering teams here never affects unrelated players
     * who haven't been given a personal board yet.
     */
    public static Scoreboard ensurePersonalBoard(Player player) {
        Scoreboard current = player.getScoreboard();
        if (current != Bukkit.getScoreboardManager().getMainScoreboard()) {
            return current;
        }
        Scoreboard fresh = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(fresh);
        return fresh;
    }
}
