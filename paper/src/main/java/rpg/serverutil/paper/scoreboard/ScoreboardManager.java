package rpg.serverutil.paper.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import rpg.serverutil.api.ScoreboardApi;
import rpg.serverutil.api.ScoreboardLineProvider;
import rpg.serverutil.paper.util.BoardUtil;
import rpg.serverutil.paper.util.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a sidebar scoreboard by merging every registered {@link ScoreboardLineProvider}'s
 * lines (highest priority first) and only re-drawing a player's board when the merged line
 * list actually changed, to avoid flicker on periodic re-render. Layers its objective/teams
 * onto the player's shared personal {@link Scoreboard} (see {@link BoardUtil}) rather than
 * replacing the whole board, so it doesn't clobber {@code TabListManager}'s teams.
 */
public final class ScoreboardManager implements ScoreboardApi {

    private static final String OBJECTIVE_NAME = "orelia_su_sb";
    private static final String TEAM_PREFIX = "su_sb_";
    private static final int MAX_LINES = ChatColor.values().length;

    private final String title;
    private final boolean hideNumbers;
    private final Map<String, ScoreboardLineProvider> providers = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastRenderedLines = new ConcurrentHashMap<>();

    public ScoreboardManager(String title, boolean hideNumbers) {
        this.title = title;
        this.hideNumbers = hideNumbers;
    }

    @Override
    public void registerProvider(ScoreboardLineProvider provider) {
        providers.put(provider.getId(), provider);
    }

    @Override
    public void unregisterProvider(String providerId) {
        providers.remove(providerId);
        lastRenderedLines.clear();
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            render(player);
        }
    }

    public void forget(Player player) {
        lastRenderedLines.remove(player.getUniqueId());
    }

    private void render(Player player) {
        List<String> lines = collectLines(player);
        if (lines.equals(lastRenderedLines.get(player.getUniqueId()))) {
            return;
        }
        lastRenderedLines.put(player.getUniqueId(), lines);

        Scoreboard board = BoardUtil.ensurePersonalBoard(player);
        Objective existing = board.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            existing.unregister();
        }
        if (lines.isEmpty()) {
            return;
        }

        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, ColorUtil.colorize(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        for (int i = 0; i < lines.size() && i < MAX_LINES; i++) {
            String entry = ChatColor.values()[i].toString();
            String teamName = TEAM_PREFIX + i;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }
            team.setPrefix(ColorUtil.colorize(lines.get(i)));
            var scoreEntry = objective.getScore(entry);
            scoreEntry.setScore(score--);
            if (hideNumbers) {
                scoreEntry.numberFormat(NumberFormat.blank());
            }
        }
    }

    private List<String> collectLines(Player player) {
        List<String> lines = new ArrayList<>();
        providers.values().stream()
                .sorted(Comparator.comparingInt(ScoreboardLineProvider::getPriority).reversed())
                .forEach(provider -> lines.addAll(provider.getLines(player)));
        return lines;
    }
}
