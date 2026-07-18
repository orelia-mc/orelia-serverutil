package rpg.serverutil.paper.integration;

import org.bukkit.entity.Player;
import rpg.api.JobApi;
import rpg.api.StatusApi;
import rpg.serverutil.api.ChatPlaceholderProvider;

import java.util.Optional;

/** Shows the player's OreliaCore level and job next to their name in chat. */
final class CoreChatPlaceholderProvider implements ChatPlaceholderProvider {

    private final JobApi jobApi;
    private final StatusApi statusApi;
    private final String format;

    CoreChatPlaceholderProvider(JobApi jobApi, StatusApi statusApi, String format) {
        this.jobApi = jobApi;
        this.statusApi = statusApi;
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
        int level = statusApi != null ? statusApi.getLevel(sender.getUniqueId()).orElse(1) : 1;
        String job = jobApi != null ? jobApi.getCurrentJobDisplayName(sender.getUniqueId()).orElse("") : "";
        return Optional.of(format.replace("{level}", String.valueOf(level)).replace("{job}", job));
    }
}
