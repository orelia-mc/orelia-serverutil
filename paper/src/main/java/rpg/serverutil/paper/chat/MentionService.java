package rpg.serverutil.paper.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.util.ColorUtil;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects {@code @name} mentions in a chat message (see {@code mention.*} in config.yml),
 * replaces each recognized one with a colored highlight, and pings whoever got mentioned with
 * a sound. {@code @all} (config-renameable) mentions every online player, gated by
 * {@code mention.all.permission} (op-only by default) so it can't be spammed by anyone typing
 * the word "all". Used only by {@link ChatModule}'s public-chat pipeline - party/guild/admin
 * chat (orelia-extra) has its own separate broadcast path this doesn't reach.
 */
final class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final OreliaServerUtilPlugin plugin;

    MentionService(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Replaces every recognized {@code @name}/{@code @all} token in {@code plainMessage} with
     * a colored highlight (unrecognized {@code @word}s are left as literal text) and schedules
     * a notification sound for whoever was mentioned. {@code colorizeLiteral} controls whether
     * the non-mention text is run through {@link ColorUtil} too (mirrors whether the sender is
     * currently permitted to use color codes in their own message).
     */
    Component highlight(String plainMessage, Player sender, boolean colorizeLiteral) {
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        if (!config.getBoolean("mention.enabled", true)) {
            return literal(plainMessage, colorizeLiteral);
        }

        String format = config.getString("mention.format", "&%6[&%e@{name}&%6]&r");
        boolean allEnabled = config.getBoolean("mention.all.enabled", true);
        String allKeyword = config.getString("mention.all.keyword", "all");
        String allPermission = config.getString("mention.all.permission", "orelia.serverutil.chat.mention.all");

        Set<Player> mentioned = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(plainMessage);
        Component result = Component.empty();
        int lastEnd = 0;
        while (matcher.find()) {
            String token = matcher.group(1);
            boolean isAll = allEnabled && token.equalsIgnoreCase(allKeyword) && sender.hasPermission(allPermission);
            Player target = isAll ? null : findOnlinePlayer(token);
            if (!isAll && target == null) {
                continue; // not a recognized mention - leave the literal "@token" text untouched
            }

            if (matcher.start() > lastEnd) {
                result = result.append(literal(plainMessage.substring(lastEnd, matcher.start()), colorizeLiteral));
            }
            String displayName = isAll ? allKeyword : target.getName();
            result = result.append(ColorUtil.component(format.replace("{name}", displayName)));
            lastEnd = matcher.end();

            if (isAll) {
                mentioned.addAll(Bukkit.getOnlinePlayers());
            } else {
                mentioned.add(target);
            }
        }
        if (lastEnd < plainMessage.length()) {
            result = result.append(literal(plainMessage.substring(lastEnd), colorizeLiteral));
        }

        mentioned.remove(sender);
        playSound(config, mentioned);
        return result;
    }

    private Component literal(String text, boolean colorize) {
        return colorize ? ColorUtil.component(text) : Component.text(text);
    }

    private Player findOnlinePlayer(String name) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private void playSound(YamlConfiguration config, Set<Player> mentioned) {
        if (mentioned.isEmpty() || !config.getBoolean("mention.sound.enabled", true)) {
            return;
        }
        String soundName = config.getString("mention.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("mention.sound.name (\"" + soundName + "\") isn't a valid org.bukkit.Sound "
                    + "constant - skipping the mention sound.");
            return;
        }
        float volume = (float) config.getDouble("mention.sound.volume", 1.0);
        float pitch = (float) config.getDouble("mention.sound.pitch", 1.0);
        // AsyncChatEvent may fire off the main thread - Player#playSound must run sync.
        Bukkit.getScheduler().runTask(plugin, () ->
                mentioned.forEach(player -> player.playSound(player.getLocation(), sound, volume, pitch)));
    }
}
