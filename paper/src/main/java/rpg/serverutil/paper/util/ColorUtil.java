package rpg.serverutil.paper.util;

import org.bukkit.ChatColor;

/**
 * Translates {@code &}-coded strings into {@link ChatColor}-formatted text. Independent copy
 * of orelia-core's {@code rpg.util.ColorUtil} (this plugin only softdepends on OreliaCore).
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
