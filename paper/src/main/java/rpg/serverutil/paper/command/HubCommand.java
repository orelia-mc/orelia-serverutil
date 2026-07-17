package rpg.serverutil.paper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.hub.HubService;

/** {@code /hub} - sends the sender to the hub; see {@link HubService} for the mode logic. */
public final class HubCommand implements CommandExecutor {

    private final OreliaServerUtilPlugin plugin;
    private final HubService hubService;

    public HubCommand(OreliaServerUtilPlugin plugin, HubService hubService) {
        this.plugin = plugin;
        this.hubService = hubService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "command.player-only");
            return true;
        }
        hubService.sendToHub(player);
        return true;
    }
}
