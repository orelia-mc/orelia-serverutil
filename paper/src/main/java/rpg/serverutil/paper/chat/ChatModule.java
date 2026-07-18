package rpg.serverutil.paper.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import rpg.serverutil.api.ChatApi;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;
import rpg.serverutil.paper.util.ColorUtil;

/**
 * Applies a configurable format (color codes + a placeholder slot next to the sender's name) to
 * every chat message via Paper's {@link AsyncChatEvent}. The placeholder is resolved once per
 * message (not per-viewer) through {@link ChatApi}, matching {@code BelownameApi}'s convention -
 * the same text is shown to everyone reading the message.
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
        String format = plugin.getConfigManager().get("config.yml").get()
                .getString("chat.format", "{placeholder}{sender}&7: &f{message}");
        String placeholderText = manager.resolve(event.getPlayer()).orElse("");
        Component placeholder = ColorUtil.component(placeholderText);

        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                render(format, sourceDisplayName, placeholder, message)));
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
