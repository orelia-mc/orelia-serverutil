package rpg.serverutil.paper.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorUtilTest {

    @Test
    void colorizeTranslatesLegacyCodes() {
        assertEquals("§aHello", ColorUtil.colorize("&aHello"));
    }

    @Test
    void colorizeExpandsHexIntoLegacySectionSequence() {
        assertEquals("§x§f§f§0§0§0§0test", ColorUtil.colorize("&#FF0000test"));
    }

    @Test
    void colorizeExpandsMappedCustomCode() {
        assertEquals("§x§e§7§4§c§3§ctest", ColorUtil.colorize("&%ctest"));
    }

    @Test
    void colorizeLeavesUnmappedCustomCodeUntouched() {
        assertEquals("&%ztest", ColorUtil.colorize("&%ztest"));
    }

    @Test
    void colorizeOfNullReturnsEmptyString() {
        assertEquals("", ColorUtil.colorize(null));
    }
}
