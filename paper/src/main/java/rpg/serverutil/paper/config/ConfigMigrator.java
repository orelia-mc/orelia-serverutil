package rpg.serverutil.paper.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds newly-introduced top-level config keys to an existing user config file without
 * touching anything else in it. Independent copy of orelia-core's
 * {@code rpg.core.config.ConfigMigrator} - see that class's Javadoc for the full design
 * rationale (never loads-then-resaves via {@link YamlConfiguration}, since that strips every
 * comment; missing keys are appended as raw text, comments included; obsolete keys are only
 * logged as a warning, never auto-removed).
 */
final class ConfigMigrator {

    private ConfigMigrator() {
    }

    static void migrate(Logger logger, File file, String bundledText) {
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new StringReader(bundledText));
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);

        int bundledVersion = bundled.getInt("config-version", 0);
        int existingVersion = existing.getInt("config-version", 0);
        if (existingVersion >= bundledVersion) {
            return;
        }

        var bundledKeys = bundled.getKeys(false);
        var existingKeys = existing.getKeys(false);
        Map<String, String> blocks = extractTopLevelBlocks(bundledText);

        StringBuilder appended = new StringBuilder();
        for (String key : bundledKeys) {
            if (!existingKeys.contains(key) && blocks.containsKey(key)) {
                appended.append("\n").append(blocks.get(key));
            }
        }

        for (String key : existingKeys) {
            if (!bundledKeys.contains(key)) {
                logger.warning("Config key '" + key + "' in " + file.getName() + " is no longer used by this version - you can remove it.");
            }
        }

        if (appended.length() == 0) {
            return;
        }
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, true)) {
            writer.write(appended.toString());
            logger.info("Added new default config keys to " + file.getName() + " - see the file for details.");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to append new config keys to " + file.getName(), e);
        }
    }

    private static Map<String, String> extractTopLevelBlocks(String text) {
        Map<String, String> blocks = new LinkedHashMap<>();
        for (String chunk : text.split("\n\\s*\n")) {
            String keyLine = null;
            for (String line : chunk.split("\n")) {
                if (line.matches("^[A-Za-z0-9_.-]+:.*")) {
                    keyLine = line;
                    break;
                }
            }
            if (keyLine != null) {
                String key = keyLine.substring(0, keyLine.indexOf(':'));
                blocks.putIfAbsent(key, chunk.strip() + "\n");
            }
        }
        return blocks;
    }
}
