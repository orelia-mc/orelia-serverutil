package rpg.serverutil.paper.worldsetup;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.config.ConfigFile;
import rpg.serverutil.paper.module.ServerUtilModule;

import java.util.List;
import java.util.Optional;

/**
 * Backs {@code /suadmin worldsetup <world> [profile]}: applies a named GameRule profile
 * (config.yml {@code world-setup.profiles.<name>.gamerules}) to a world in one command,
 * instead of running a dozen vanilla {@code /gamerule} commands by hand.
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
     * Applies {@code profileName}'s gamerules to {@code world}. Empty if the profile isn't
     * defined in config.yml; otherwise the list of gamerule keys that couldn't be applied
     * (unknown name or type mismatch - empty list means everything applied cleanly).
     */
    public Optional<List<String>> applyProfile(World world, String profileName) {
        ConfigFile configFile = plugin.getConfigManager().get("config.yml");
        ConfigurationSection profileSection = configFile.get()
                .getConfigurationSection("world-setup.profiles." + profileName);
        if (profileSection == null) {
            return Optional.empty();
        }
        return Optional.of(GameRuleRegistry.apply(world, profileSection.getConfigurationSection("gamerules")));
    }

    /** Names of every profile defined under {@code world-setup.profiles}, for tab-completion. */
    public List<String> getProfileNames() {
        ConfigFile configFile = plugin.getConfigManager().get("config.yml");
        ConfigurationSection profiles = configFile.get().getConfigurationSection("world-setup.profiles");
        return profiles == null ? List.of() : List.copyOf(profiles.getKeys(false));
    }
}
