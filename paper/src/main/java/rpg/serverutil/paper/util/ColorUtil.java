package rpg.serverutil.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates {@code &}-coded strings into {@link ChatColor}-formatted text. Independent copy
 * of orelia-core's {@code rpg.util.ColorUtil} (this plugin only softdepends on OreliaCore) -
 * keep the two in sync when changing color-code behavior.
 *
 * <p>{@code &#RRGGBB} hex codes are supported, expanded into the legacy {@code
 * §x§R§R§G§G§B§B} encoding Bukkit/Adventure use for hex under the hood, so both
 * {@link #colorize} and {@link #component} handle hex transparently.
 *
 * <p>{@code &%<char>} custom color codes are also supported - see {@link #CUSTOM_COLORS} to
 * add/remove one.
 */
public final class ColorUtil {

    private static final char SECTION = '§';
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern CUSTOM_PATTERN = Pattern.compile("&%(.)");

    /**
     * Custom color codes ({@code &%<char>}), each mapped to a 6-digit hex RGB value. Add or
     * remove entries here freely - an unmapped {@code &%<char>} is left untouched as literal
     * text. Keep in sync with orelia-core's {@code rpg.util.ColorUtil.CUSTOM_COLORS}.
     */
    private static final Map<Character, String> CUSTOM_COLORS = Map.of(
            'c', "E74C3C" // example: a custom "red" distinct from vanilla &c (RED)
            // add more here, e.g. 'x', "RRGGBB"
    );

    private static final LegacyComponentSerializer HEX_AWARE_SERIALIZER = LegacyComponentSerializer.builder()
            .character(SECTION)
            .hexColors()
            .build();

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        String expanded = expandCustomCodes(expandHex(input));
        return ChatColor.translateAlternateColorCodes('&', expanded);
    }

    /** Same {@code &}-code translation as {@link #colorize(String)} (legacy, hex, custom), as an Adventure {@link Component}. */
    public static Component component(String input) {
        return HEX_AWARE_SERIALIZER.deserialize(colorize(input));
    }

    private static String expandHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(hexToLegacySection(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String expandCustomCodes(String input) {
        Matcher matcher = CUSTOM_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String hex = CUSTOM_COLORS.get(matcher.group(1).charAt(0));
            String replacement = hex != null ? hexToLegacySection(hex) : matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Expands a 6-digit hex value into {@code §x§R§R§G§G§B§B} (each digit as its own {@code §}-prefixed char). */
    private static String hexToLegacySection(String hex) {
        StringBuilder expanded = new StringBuilder().append(SECTION).append('x');
        for (char hexDigit : hex.toLowerCase().toCharArray()) {
            expanded.append(SECTION).append(hexDigit);
        }
        return expanded.toString();
    }
}
