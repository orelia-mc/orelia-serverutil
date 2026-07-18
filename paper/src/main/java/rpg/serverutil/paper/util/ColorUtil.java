package rpg.serverutil.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    /** Same {@code &}-code translation as {@link #colorize(String)}, as an Adventure {@link Component}. */
    public static Component component(String input) {
        return LegacyComponentSerializer.legacySection().deserialize(colorize(input));
    }
}
