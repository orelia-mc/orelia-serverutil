package rpg.serverutil.paper.message;

import org.bukkit.command.CommandSender;
import rpg.serverutil.paper.config.ConfigFile;
import rpg.serverutil.paper.util.ColorUtil;

/**
 * Key-based lookup over {@code messages.yml}. Independent copy of orelia-core's
 * {@code rpg.core.message.MessageManager} (this plugin only softdepends on OreliaCore, so it
 * can't share core's infrastructure classes - see also {@code rpg.serverutil.paper.config}).
 */
public final class MessageManager {

    private final ConfigFile messagesFile;

    public MessageManager(ConfigFile messagesFile) {
        this.messagesFile = messagesFile;
    }

    public String getPrefix() {
        return ColorUtil.colorize(messagesFile.get().getString("prefix", ""));
    }

    public String raw(String key) {
        String value = messagesFile.get().getString(key);
        return value != null ? value : "??" + key + "??";
    }

    public String format(String key, Object... placeholders) {
        String template = raw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            template = template.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return ColorUtil.colorize(template);
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(getPrefix() + format(key, placeholders));
    }

    public void sendRaw(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(format(key, placeholders));
    }
}
