package rpg.serverutil.paper.tablist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import rpg.serverutil.api.TabListApi;
import rpg.serverutil.api.TabListEntry;
import rpg.serverutil.api.TabListNameFormatter;
import rpg.serverutil.paper.util.BoardUtil;
import rpg.serverutil.paper.util.ColorUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Colors/decorates tab-list names via scoreboard Team prefix/suffix. A player's name color in
 * *another* player's tab list is driven by that other player's own current {@link Scoreboard}
 * (Minecraft's tab list follows each client's local scoreboard team membership), so this has
 * to write every online player's team into every online player's personal board each tick -
 * O(playerCount^2), acceptable for the player counts this plugin targets.
 */
public final class TabListManager implements TabListApi {

    private static final String TEAM_PREFIX = "su_tab_";

    private final Map<String, TabListNameFormatter> formatters = new ConcurrentHashMap<>();

    @Override
    public void registerFormatter(TabListNameFormatter formatter) {
        formatters.put(formatter.getId(), formatter);
    }

    @Override
    public void unregisterFormatter(String formatterId) {
        formatters.remove(formatterId);
    }

    public void tick() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        Map<UUID, TabListEntry> resolved = new HashMap<>();
        for (Player target : online) {
            resolve(target).ifPresent(entry -> resolved.put(target.getUniqueId(), entry));
        }

        for (Player viewer : online) {
            Scoreboard board = BoardUtil.ensurePersonalBoard(viewer);
            for (Player target : online) {
                applyTeam(board, target, resolved.get(target.getUniqueId()));
            }
        }
    }

    private Optional<TabListEntry> resolve(Player player) {
        return formatters.values().stream()
                .sorted(Comparator.comparingInt(TabListNameFormatter::getPriority).reversed())
                .map(formatter -> formatter.format(player))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private void applyTeam(Scoreboard board, Player target, TabListEntry entry) {
        // Keep the team name short - older Minecraft versions capped this at 16 characters.
        String teamName = TEAM_PREFIX + target.getUniqueId().toString().replace("-", "").substring(0, 8);
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(target.getName());
        }
        team.setPrefix(entry != null ? ColorUtil.colorize(entry.prefix()) : "");
        team.setSuffix(entry != null ? ColorUtil.colorize(entry.suffix()) : "");
    }
}
