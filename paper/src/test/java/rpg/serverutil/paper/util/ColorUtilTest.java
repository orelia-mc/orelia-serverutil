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
        assertEquals("§x§d§6§6§f§6§ftest", ColorUtil.colorize("&%ctest"));
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
