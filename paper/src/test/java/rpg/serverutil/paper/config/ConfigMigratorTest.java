package rpg.serverutil.paper.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {

    private static final Logger LOGGER = Logger.getLogger(ConfigMigratorTest.class.getName());

    @Test
    void appendsMissingTopLevelSectionFromBundledDefault(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 1

                # Existing section.
                velocity:
                  enabled: false
                """;
        String bundledText = """
                config-version: 2

                # Existing section.
                velocity:
                  enabled: false

                # A brand new section added in version 2.
                new-feature:
                  enabled: true
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(result.contains("# A brand new section added in version 2."));
        assertTrue(result.contains("new-feature:"));
        assertTrue(result.contains("enabled: true"));
        assertTrue(result.startsWith(existingText));
    }

    @Test
    void doesNothingWhenExistingVersionIsAlreadyCurrent(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 2

                velocity:
                  enabled: false
                """;
        String bundledText = """
                config-version: 2

                velocity:
                  enabled: false

                new-feature:
                  enabled: true
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(result.contains("new-feature"));
    }
}
