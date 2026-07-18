package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.serverutil.api.ChatPlaceholderProvider;
import rpg.serverutil.paper.placeholder.PlaceholderService;

import java.util.Optional;

/** Shows a config-driven, placeholder-resolved value next to the sender's name in chat (level + job by default). */
final class CoreChatPlaceholderProvider implements ChatPlaceholderProvider {

    private final PlaceholderService placeholders;
    private final String format;

    CoreChatPlaceholderProvider(PlaceholderService placeholders, String format) {
        this.placeholders = placeholders;
        this.format = format;
    }

    @Override
    public String getId() {
        return "orelia-core-chat";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Optional<String> getPlaceholder(Player sender) {
        return Optional.of(placeholders.resolve(format, sender));
    }
}
