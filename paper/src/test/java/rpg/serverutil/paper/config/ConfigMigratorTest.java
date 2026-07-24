package rpg.serverutil.paper.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void appendsMissingKeyInsideExistingTopLevelSection(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 1

                velocity:
                  enabled: false
                """;
        String bundledText = """
                config-version: 2

                velocity:
                  enabled: false
                  # Plugin messaging channel, must match the Velocity-side config.
                  channel: "orelia:serverutil"
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(result.contains("enabled: false"));
        assertTrue(result.contains("  # Plugin messaging channel, must match the Velocity-side config."));
        assertTrue(result.contains("  channel: \"orelia:serverutil\""));
        int velocityIndex = result.indexOf("velocity:");
        int channelIndex = result.indexOf("channel: \"orelia:serverutil\"");
        assertTrue(channelIndex > velocityIndex);
    }

    @Test
    void appendsMissingKeyTwoLevelsDeepInsideExistingTree(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 1

                scoreboard:
                  core-lines:
                    enabled: true
                """;
        String bundledText = """
                config-version: 2

                scoreboard:
                  core-lines:
                    enabled: true
                    priority: 10
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(result.contains("    priority: 10"));
        assertTrue(result.contains("scoreboard:\n  core-lines:\n    enabled: true"));
    }

    @Test
    void appendsWholeNewNestedSectionAtOnceWhenParentDoesNotExist(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 1

                scoreboard:
                  core-lines:
                    enabled: true
                """;
        String bundledText = """
                config-version: 2

                scoreboard:
                  core-lines:
                    enabled: true
                  hide-numbers:
                    enabled: false
                    priority: 5
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(result.contains("hide-numbers:"));
        assertTrue(result.contains("enabled: false"));
        assertTrue(result.contains("priority: 5"));
    }

    @Test
    void migratingTwiceDoesNotDuplicateAppendedKeys(@TempDir Path tempDir) throws IOException {
        String existingText = """
                config-version: 1

                velocity:
                  enabled: false
                """;
        String bundledText = """
                config-version: 2

                velocity:
                  enabled: false
                  channel: "orelia:serverutil"
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);
        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        int firstIndex = result.indexOf("channel: \"orelia:serverutil\"");
        int lastIndex = result.lastIndexOf("channel: \"orelia:serverutil\"");
        assertEquals(firstIndex, lastIndex);
    }

    @Test
    void backfillsMissingConfigVersionAtTopOfFile(@TempDir Path tempDir) throws IOException {
        String existingText = """
                velocity:
                  enabled: false
                """;
        String bundledText = """
                config-version: 1

                velocity:
                  enabled: false
                """;

        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, existingText, StandardCharsets.UTF_8);

        ConfigMigrator.migrate(LOGGER, file.toFile(), bundledText);

        String result = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(result.startsWith("config-version: 1"));
        assertTrue(result.contains("enabled: false"));
    }
}
