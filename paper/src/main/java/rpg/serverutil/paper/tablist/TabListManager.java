package rpg.serverutil.paper.tablist;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import rpg.serverutil.api.TabListApi;
import rpg.serverutil.api.TabListEntry;
import rpg.serverutil.api.TabListNameFormatter;
import rpg.serverutil.api.TabListValueProvider;
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
 * Colors/decorates tab-list names via scoreboard Team prefix/suffix/color, and writes the
 * right-side value via a {@link DisplaySlot#PLAYER_LIST} Objective. A player's name color in
 * *another* player's tab list is driven by that other player's own current {@link Scoreboard}
 * (Minecraft's tab list follows each client's local scoreboard team membership), so this has
 * to write every online player's team into every online player's personal board each tick -
 * O(playerCount^2), acceptable for the player counts this plugin targets.
 *
 * <p>The same Team that drives the tab-list name prefix/suffix/color also colors that player's
 * above-head nametag - this is intentional (see the plan's design notes): Minecraft only allows
 * one Team per scoreboard entry, so nametag color and tab-list name color are the same value on
 * this plugin, not independently configurable.
 */
public final class TabListManager implements TabListApi {

    private static final String TEAM_PREFIX = "su_tab_";
    private static final String VALUE_OBJECTIVE_NAME = "orelia_su_tlv";

    private final Map<String, TabListNameFormatter> formatters = new ConcurrentHashMap<>();
    private final Map<String, TabListValueProvider> valueProviders = new ConcurrentHashMap<>();

    @Override
    public void registerFormatter(TabListNameFormatter formatter) {
        formatters.put(formatter.getId(), formatter);
    }

    @Override
    public void unregisterFormatter(String formatterId) {
        formatters.remove(formatterId);
    }

    @Override
    public void registerValueProvider(TabListValueProvider provider) {
        valueProviders.put(provider.getId(), provider);
    }

    @Override
    public void unregisterValueProvider(String providerId) {
        valueProviders.remove(providerId);
    }

    public void tick() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        Map<UUID, TabListEntry> resolvedEntries = new HashMap<>();
        Map<UUID, String> resolvedValues = new HashMap<>();
        for (Player target : online) {
            resolveEntry(target).ifPresent(entry -> resolvedEntries.put(target.getUniqueId(), entry));
            resolveValue(target).ifPresent(value -> resolvedValues.put(target.getUniqueId(), value));
        }

        for (Player viewer : online) {
            Scoreboard board = BoardUtil.ensurePersonalBoard(viewer);
            Objective valueObjective = ensureValueObjective(board);
            for (Player target : online) {
                applyTeam(board, target, resolvedEntries.get(target.getUniqueId()));
                applyValue(valueObjective, target, resolvedValues.get(target.getUniqueId()));
            }
        }
    }

    private Optional<TabListEntry> resolveEntry(Player player) {
        return formatters.values().stream()
                .sorted(Comparator.comparingInt(TabListNameFormatter::getPriority).reversed())
                .map(formatter -> formatter.format(player))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<String> resolveValue(Player player) {
        return valueProviders.values().stream()
                .sorted(Comparator.comparingInt(TabListValueProvider::getPriority).reversed())
                .map(provider -> provider.getValue(player))
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
        team.setColor(entry != null && entry.color() != null ? entry.color() : ChatColor.RESET);
    }

    private Objective ensureValueObjective(Scoreboard board) {
        Objective objective = board.getObjective(VALUE_OBJECTIVE_NAME);
        if (objective == null) {
            objective = board.registerNewObjective(VALUE_OBJECTIVE_NAME, Criteria.DUMMY, "");
            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        return objective;
    }

    private void applyValue(Objective objective, Player target, String value) {
        var score = objective.getScore(target.getName());
        score.setScore(0);
        score.numberFormat(value != null ? NumberFormat.fixed(ColorUtil.component(value)) : NumberFormat.blank());
    }
}
