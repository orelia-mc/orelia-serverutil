package rpg.serverutil.paper.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A single named YAML config file backed by a file on disk, copied from the jar's bundled
 * default on first use. Independent copy of orelia-core's {@code rpg.core.config.ConfigFile}
 * - this plugin only softdepends on OreliaCore, so it can't import core's infrastructure
 * classes (they wouldn't be on the classpath when core isn't installed).
 */
public final class ConfigFile {

    private final Logger logger;
    private final File file;
    private final String resourcePath;
    private YamlConfiguration configuration;

    ConfigFile(Logger logger, File dataFolder, String fileName) {
        this.logger = logger;
        this.file = new File(dataFolder, fileName);
        this.resourcePath = fileName;
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config file: " + resourcePath, e);
        }
    }

    public YamlConfiguration get() {
        return configuration;
    }

    public File getFile() {
        return file;
    }
}
