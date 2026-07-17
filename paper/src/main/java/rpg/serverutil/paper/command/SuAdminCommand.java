package rpg.serverutil.paper.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.spawn.SpawnModule;
import rpg.serverutil.paper.worldsetup.WorldSetupModule;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /suadmin <reload|setspawn|worldsetup <world> [profile]|...>} - the root admin
 * dispatcher for orelia-serverutil. Unlike orelia-core/world/extra/debug, this doesn't
 * register into a shared {@code AdminCommandRegistry} (OreliaCore is only a soft dependency)
 * - it's its own top-level Bukkit command (see {@code plugin.yml}).
 */
public final class SuAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "setspawn", "worldsetup");

    private final OreliaServerUtilPlugin plugin;

    public SuAdminCommand(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().send(sender, "usage.root");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                plugin.getMessageManager().send(sender, "admin.reloaded");
            }
            case "setspawn" -> setSpawn(sender);
            case "worldsetup" -> worldSetup(sender, args);
            default -> plugin.getMessageManager().send(sender, "usage.root");
        }
        return true;
    }

    private void setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "command.player-only");
            return;
        }
        plugin.getModuleManager().get(SpawnModule.class).ifPresent(spawnModule -> {
            spawnModule.setSpawn(player);
            plugin.getMessageManager().send(sender, "spawn.set", "world", player.getWorld().getName());
        });
    }

    private void worldSetup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "usage.worldsetup");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            plugin.getMessageManager().send(sender, "worldsetup.world-not-found", "world", args[1]);
            return;
        }
        String profile = args.length >= 3 ? args[2] : "default";
        plugin.getModuleManager().get(WorldSetupModule.class).ifPresent(worldSetupModule -> {
            var invalidKeys = worldSetupModule.applyProfile(world, profile);
            if (invalidKeys.isEmpty()) {
                plugin.getMessageManager().send(sender, "worldsetup.profile-not-found", "profile", profile);
                return;
            }
            if (invalidKeys.get().isEmpty()) {
                plugin.getMessageManager().send(sender, "worldsetup.applied", "world", world.getName(), "profile", profile);
            } else {
                plugin.getMessageManager().send(sender, "worldsetup.applied-with-warnings",
                        "world", world.getName(), "profile", profile, "keys", String.join(", ", invalidKeys.get()));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return matching(SUBCOMMANDS, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("worldsetup")) {
            List<String> worldNames = Bukkit.getWorlds().stream().map(World::getName).toList();
            return matching(worldNames, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("worldsetup")) {
            List<String> profiles = plugin.getModuleManager().get(WorldSetupModule.class)
                    .map(WorldSetupModule::getProfileNames)
                    .orElse(List.of());
            return matching(profiles, args[2]);
        }
        return List.of();
    }

    private List<String> matching(Iterable<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
