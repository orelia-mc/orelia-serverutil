package rpg.serverutil.paper.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rpg.api.EconomyApi;
import rpg.api.JobApi;
import rpg.api.StatusApi;
import rpg.extra.api.GuildApi;
import rpg.extra.api.PartyApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.util.MoneyFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Single place every provider/module resolves {@code {token}} placeholders through, instead of
 * each hand-rolling its own {@code String#replace} chain. Two tiers:
 *
 * <ul>
 *   <li>Built-in server tokens - always available, no OreliaCore needed:
 *       {@code {online}}/{@code {max_online}}/{@code {tps}}/{@code {ping}}/{@code {world}}/
 *       {@code {player}}/{@code {name}}/{@code {server}}/{@code {date}}/{@code {time}}.</li>
 *   <li>OreliaCore tokens - resolved via its published {@code rpg.api} interfaces
 *       (soft dependency, silently left as literal text if OreliaCore isn't installed):
 *       {@code {level}}/{@code {job}}/{@code {money}}/{@code {health}}/{@code {max_health}}.
 *       {@code {health}}/{@code {max_health}} come from {@link StatusApi#getCurrentHp} /
 *       {@code getFinalStats}'s {@code "HP"} entry - deliberately NOT vanilla
 *       {@link Player#getHealth()}, since Orelia tracks its own RPG health separately from the
 *       vanilla health bar. {@code {money}} is formatted via {@link MoneyFormat}.</li>
 *   <li>OreliaExtra tokens - resolved via its published {@code rpg.extra.api} interfaces
 *       (soft dependency, silently left as literal text if OreliaExtra isn't installed):
 *       {@code {guild}}/{@code {guild_tag}} (empty string if not in a guild) and
 *       {@code {party}} (a colored marker if in a party, empty string otherwise).</li>
 * </ul>
 *
 * <p>If PlaceholderAPI is installed, any remaining {@code %...%} placeholders are resolved
 * through it last (see {@link PlaceholderApiHook}), so admins can mix in ranks/other plugins'
 * placeholders freely.
 */
public final class PlaceholderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OreliaServerUtilPlugin plugin;
    private final boolean placeholderApiPresent;

    public PlaceholderService(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        this.placeholderApiPresent = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String resolve(String template, Player player) {
        String result = resolveBuiltIn(template, player);
        result = resolveCoreTokens(result, player);
        result = resolveExtraTokens(result, player);
        if (placeholderApiPresent) {
            result = PlaceholderApiHook.resolve(player, result);
        }
        return result;
    }

    private String resolveBuiltIn(String template, Player player) {
        String serverName = plugin.getConfigManager().get("config.yml").get().getString("server.name", "");
        return template
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max_online}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{tps}", String.format("%.1f", Math.min(20.0, Bukkit.getTPS()[0])))
                .replace("{ping}", String.valueOf(player.getPing()))
                .replace("{world}", player.getWorld().getName())
                .replace("{player}", player.getName())
                .replace("{name}", player.getName())
                .replace("{server}", serverName)
                .replace("{date}", LocalDateTime.now().format(DATE_FORMAT))
                .replace("{time}", LocalDateTime.now().format(TIME_FORMAT));
    }

    private String resolveCoreTokens(String template, Player player) {
        String result = template;
        if (result.contains("{level}") || result.contains("{health}") || result.contains("{max_health}")) {
            StatusApi statusApi = plugin.getServer().getServicesManager().load(StatusApi.class);
            if (statusApi != null) {
                double currentHp = statusApi.getCurrentHp(player.getUniqueId()).orElse(0.0);
                double maxHp = statusApi.getFinalStats(player.getUniqueId()).getOrDefault("HP", 0.0);
                result = result.replace("{level}", String.valueOf(statusApi.getLevel(player.getUniqueId()).orElse(1)))
                        .replace("{health}", String.valueOf((long) currentHp))
                        .replace("{max_health}", String.valueOf((long) maxHp));
            }
        }
        if (result.contains("{job}")) {
            JobApi jobApi = plugin.getServer().getServicesManager().load(JobApi.class);
            if (jobApi != null) {
                result = result.replace("{job}", jobApi.getCurrentJobDisplayName(player.getUniqueId()).orElse(""));
            }
        }
        if (result.contains("{money}")) {
            EconomyApi economyApi = plugin.getServer().getServicesManager().load(EconomyApi.class);
            if (economyApi != null) {
                result = result.replace("{money}", MoneyFormat.format(economyApi.getBalance(player.getUniqueId())));
            }
        }
        return result;
    }

    private String resolveExtraTokens(String template, Player player) {
        String result = template;
        if (result.contains("{guild}") || result.contains("{guild_tag}")) {
            GuildApi guildApi = plugin.getServer().getServicesManager().load(GuildApi.class);
            if (guildApi != null) {
                result = result.replace("{guild}", guildApi.getGuildName(player.getUniqueId()).orElse(""))
                        .replace("{guild_tag}", guildApi.getGuildTag(player.getUniqueId()).orElse(""));
            }
        }
        if (result.contains("{party}")) {
            PartyApi partyApi = plugin.getServer().getServicesManager().load(PartyApi.class);
            if (partyApi != null) {
                result = result.replace("{party}", partyApi.isInParty(player.getUniqueId()) ? "&%9◆" : "");
            }
        }
        return result;
    }
}
