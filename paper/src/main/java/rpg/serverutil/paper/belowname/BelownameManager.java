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
import rpg.serverutil.paper.placeholder.PlaceholderService;
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
 * {@code TabListManager}. There is no separate "enabled" switch - the feature shows itself
 * automatically the moment any {@link BelownameValueProvider} resolves a value for at least one
 * online player, and hides itself (unregisters the objective) again once none do, mirroring
 * {@code ScoreboardManager}'s auto-hide-when-empty behavior. This means a plugin (e.g.
 * {@code CoreIntegrationModule}) registering a provider is sufficient on its own - no
 * config.yml flag needs to be flipped first.
 *
 * <p>The title is resolved per-viewer through {@link PlaceholderService} (each viewer has their
 * own personal {@link Scoreboard}, so this can differ per viewer, e.g. {@code {ping}}) and
 * re-applied every tick so it stays live - see {@link #setTitleTemplate(String)} for
 * {@code /suadmin reload} support.
 */
public final class BelownameManager implements BelownameApi {

    private static final String OBJECTIVE_NAME = "orelia_su_bn";

    private final PlaceholderService placeholders;
    private final Map<String, BelownameValueProvider> providers = new ConcurrentHashMap<>();
    private volatile String titleTemplate;

    public BelownameManager(PlaceholderService placeholders, String titleTemplate) {
        this.placeholders = placeholders;
        this.titleTemplate = titleTemplate;
    }

    /** Re-read by {@code /suadmin reload} - takes effect on the next tick, no restart needed. */
    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
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
            if (resolved.isEmpty()) {
                // No provider is producing anything for anyone right now - stay hidden rather
                // than showing an empty title to every player.
                if (objective != null) {
                    objective.unregister();
                }
                continue;
            }
            if (objective == null) {
                objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                        ColorUtil.component(placeholders.resolve(titleTemplate, viewer)));
                objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            } else {
                objective.displayName(ColorUtil.component(placeholders.resolve(titleTemplate, viewer)));
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
