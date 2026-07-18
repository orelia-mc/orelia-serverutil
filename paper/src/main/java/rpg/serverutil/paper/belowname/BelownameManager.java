package rpg.serverutil.paper.belowname;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import rpg.serverutil.api.BelownameApi;
import rpg.serverutil.api.BelownameValueProvider;
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
 * Renders the text shown below every player's nametag via a {@link DisplaySlot#BELOW_NAME}
 * {@link Objective}, same {@link BoardUtil} personal-board layering as {@code ScoreboardManager}/
 * {@code TabListManager}. The title is shared (Minecraft's belowname title is not per-viewer),
 * only the per-target value differs.
 */
public final class BelownameManager implements BelownameApi {

    private static final String OBJECTIVE_NAME = "orelia_su_bn";

    private final String title;
    private final Map<String, BelownameValueProvider> providers = new ConcurrentHashMap<>();

    public BelownameManager(String title) {
        this.title = title;
    }

    @Override
    public void registerProvider(BelownameValueProvider provider) {
        providers.put(provider.getId(), provider);
    }

    @Override
    public void unregisterProvider(String providerId) {
        providers.remove(providerId);
    }

    public void tick() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        Map<UUID, String> resolved = new HashMap<>();
        for (Player target : online) {
            resolve(target).ifPresent(value -> resolved.put(target.getUniqueId(), value));
        }

        for (Player viewer : online) {
            Scoreboard board = BoardUtil.ensurePersonalBoard(viewer);
            Objective objective = board.getObjective(OBJECTIVE_NAME);
            if (objective == null) {
                objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, ColorUtil.colorize(title));
                objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
            for (Player target : online) {
                var score = objective.getScore(target.getName());
                score.setScore(0);
                String value = resolved.get(target.getUniqueId());
                score.numberFormat(value != null ? NumberFormat.fixed(ColorUtil.component(value)) : NumberFormat.blank());
            }
        }
    }

    private Optional<String> resolve(Player target) {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(BelownameValueProvider::getPriority).reversed())
                .map(provider -> provider.getValue(target))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
