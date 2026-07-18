package rpg.serverutil.paper.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import rpg.serverutil.api.ChatApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;
import rpg.serverutil.paper.placeholder.PlaceholderService;
import rpg.serverutil.paper.util.ColorUtil;

import java.util.List;

/**
 * Applies a configurable format (color codes + a placeholder slot next to the sender's name)
 * to every chat message via Paper's {@link AsyncChatEvent}, and optionally attaches a hover
 * tooltip (join time, level, job, ...) to the sender's name - see {@code chat.tooltip} in
 * config.yml. The placeholder is resolved once per message (not per-viewer) through
 * {@link ChatApi}, matching {@code BelownameApi}'s convention - the same text/tooltip is
 * shown to everyone reading the message.
 */
public final class ChatModule implements ServerUtilModule, Listener {

    private OreliaServerUtilPlugin plugin;
    private ChatManager manager;

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getConfigManager().get("config.yml").get().getBoolean("chat.enabled", true)) {
            return;
        }
        this.manager = new ChatManager();
        plugin.getServer().getServicesManager().register(ChatApi.class, manager, plugin, ServicePriority.Normal);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        YamlConfiguration config = plugin.getConfigManager().get("config.yml").get();
        String format = config.getString("chat.format", "{placeholder}{sender}&7: &f{message}");
        String placeholderText = manager.resolve(event.getPlayer()).orElse("");
        Component placeholder = ColorUtil.component(placeholderText);
        Component tooltip = buildTooltip(config, event.getPlayer());

        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> {
            Component sender = tooltip != null ? sourceDisplayName.hoverEvent(HoverEvent.showText(tooltip)) : sourceDisplayName;
            return render(format, sender, placeholder, message);
        }));
    }

    /** Builds the (message-send-time) hover tooltip Component shown when hovering over the sender's name, or {@code null} if disabled. */
    private Component buildTooltip(YamlConfiguration config, Player player) {
        if (!config.getBoolean("chat.tooltip.enabled", true)) {
            return null;
        }
        List<String> lines = config.getStringList("chat.tooltip.lines");
        if (lines.isEmpty()) {
            return null;
        }
        PlaceholderService placeholders = plugin.getPlaceholderService();
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(ColorUtil.component(placeholders.resolve(lines.get(i), player)));
        }
        return result;
    }

    private Component render(String template, Component sender, Component placeholder, Component message) {
        Component result = Component.empty();
        int i = 0;
        while (i < template.length()) {
            int nextSender = template.indexOf("{sender}", i);
            int nextPlaceholder = template.indexOf("{placeholder}", i);
            int nextMessage = template.indexOf("{message}", i);
            int next = closest(nextSender, nextPlaceholder, nextMessage);
            if (next == -1) {
                result = result.append(ColorUtil.component(template.substring(i)));
                break;
            }
            if (next > i) {
                result = result.append(ColorUtil.component(template.substring(i, next)));
            }
            if (next == nextSender) {
                result = result.append(sender);
                i = next + "{sender}".length();
            } else if (next == nextPlaceholder) {
                result = result.append(placeholder);
                i = next + "{placeholder}".length();
            } else {
                result = result.append(message);
                i = next + "{message}".length();
            }
        }
        return result;
    }

    private int closest(int... positions) {
        int closest = -1;
        for (int position : positions) {
            if (position != -1 && (closest == -1 || position < closest)) {
                closest = position;
            }
        }
        return closest;
    }
}
