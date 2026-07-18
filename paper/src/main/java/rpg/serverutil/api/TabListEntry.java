package rpg.serverutil.api;

import org.bukkit.ChatColor;

/**
 * A resolved tab-list name decoration. {@code prefix}/{@code suffix} may be an empty string.
 * {@code color} is nullable (null = don't override the name color) - since it drives a
 * scoreboard {@code Team}'s color, it also colors the player's above-head nametag (same Team
 * backs both surfaces on modern Minecraft, see {@code TabListManager}'s class Javadoc).
 */
public record TabListEntry(String prefix, String suffix, ChatColor color) {
}
