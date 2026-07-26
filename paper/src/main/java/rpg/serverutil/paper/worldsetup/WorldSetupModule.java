package rpg.serverutil.paper.worldsetup;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.config.ConfigFile;
import rpg.serverutil.paper.module.ServerUtilModule;

import java.util.List;
import java.util.Optional;

/**
 * Backs {@code /suadmin worldsetup <world> [profile]}: runs a named batch of console commands
 * (config.yml {@code world-setup.profiles.<name>.commands}) against a world in one go, instead
 * of typing them by hand one at a time. Each command has {@code {world}} substituted for the
 * given world name first, so the same profile works for any world without editing config.yml.
 *
 * <p>Deliberately not limited to vanilla {@code /gamerule} (an earlier version only applied a
 * config-driven map of gamerule name -> value via {@link org.bukkit.GameRule#getByName}) - a
 * profile's commands are run exactly as typed by whatever plugin owns them, so it can invoke
 * another plugin's own setup command too (e.g. Multiverse-Core's {@code mv create}) as part of
 * the same one-command batch, including ones that create the world in the first place. A
 * vanilla gamerule change needs an explicit {@code execute in minecraft:{world} run gamerule
 * ...} wrapper to target a specific world when run from console - see the bundled default
 * profile for the pattern.
 */
public final class WorldSetupModule implements ServerUtilModule {

    private OreliaServerUtilPlugin plugin;

    @Override
    public String getName() {
        return "world-setup";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onDisable() {
    }

    /**
     * Runs {@code profileName}'s commands against {@code worldName} (substituted into every
     * {@code {world}} token) from the console. Empty if the profile isn't defined in
     * config.yml; otherwise the number of commands dispatched (0 if the profile has none).
     * {@code worldName} doesn't need to already exist as a loaded {@link org.bukkit.World} -
     * a profile is free to be the thing that creates it.
     */
    public Optional<Integer> applyProfile(String worldName, String profileName) {
        ConfigFile configFile = plugin.getConfigManager().get("config.yml");
        ConfigurationSection profileSection = configFile.get()
                .getConfigurationSection("world-setup.profiles." + profileName);
        if (profileSection == null) {
            return Optional.empty();
        }
        List<String> commands = profileSection.getStringList("commands");
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{world}", worldName));
        }
        return Optional.of(commands.size());
    }

    /** Names of every profile defined under {@code world-setup.profiles}, for tab-completion. */
    public List<String> getProfileNames() {
        ConfigFile configFile = plugin.getConfigManager().get("config.yml");
        ConfigurationSection profiles = configFile.get().getConfigurationSection("world-setup.profiles");
        return profiles == null ? List.of() : List.copyOf(profiles.getKeys(false));
    }
}
