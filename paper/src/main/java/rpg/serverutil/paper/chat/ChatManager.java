package rpg.serverutil.paper.chat;

import org.bukkit.entity.Player;
import rpg.serverutil.api.ChatApi;
import rpg.serverutil.api.ChatPlaceholderProvider;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatManager implements ChatApi {

    private final Map<String, ChatPlaceholderProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void registerProvider(ChatPlaceholderProvider provider) {
        providers.put(provider.getId(), provider);
    }

    @Override
    public void unregisterProvider(String providerId) {
        providers.remove(providerId);
    }

    public Optional<String> resolve(Player sender) {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(ChatPlaceholderProvider::getPriority).reversed())
                .map(provider -> provider.getPlaceholder(sender))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
