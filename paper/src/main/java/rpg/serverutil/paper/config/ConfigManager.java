package rpg.serverutil.paper.config;

import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central registry of this plugin's YAML config files. Independent copy of orelia-core's
 * {@code rpg.core.config.ConfigManager} (see {@link ConfigFile} for why).
 */
public final class ConfigManager {

    private final Plugin plugin;
    private final Map<String, ConfigFile> files = new LinkedHashMap<>();

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public ConfigFile register(String fileName) {
        return files.computeIfAbsent(fileName, name -> {
            if (plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            }
            return new ConfigFile(plugin.getLogger(), plugin.getDataFolder(), name);
        });
    }

    public ConfigFile get(String fileName) {
        ConfigFile file = files.get(fileName);
        if (file == null) {
            throw new IllegalStateException("Config file not registered: " + fileName);
        }
        return file;
    }

    public void reloadAll() {
        files.values().forEach(ConfigFile::reload);
    }

    public Set<String> getRegisteredFileNames() {
        return Set.copyOf(files.keySet());
    }
}
