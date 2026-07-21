package rpg.serverutil.paper.util;

import java.util.Locale;

/**
 * Formats a money amount into a compact {@code k}/{@code m}-suffixed string (e.g. {@code 1500}
 * -> {@code "1.5k"}, {@code 2000000} -> {@code "2m"}), used by the {@code {money}} placeholder.
 * Independent copy of orelia-core's {@code rpg.util.MoneyFormat} (this plugin only softdepends
 * on OreliaCore) - keep the two in sync when changing formatting behavior.
 */
public final class MoneyFormat {

    private MoneyFormat() {
    }

    public static String format(double amount) {
        double abs = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";
        if (abs >= 1_000_000) {
            return sign + trimTrailingZero(abs / 1_000_000) + "m";
        }
        if (abs >= 1_000) {
            return sign + trimTrailingZero(abs / 1_000) + "k";
        }
        return sign + trimTrailingZero(abs);
    }

    private static String trimTrailingZero(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
