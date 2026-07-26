package rpg.serverutil.paper.logfilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import rpg.serverutil.paper.OreliaServerUtilPlugin;
import rpg.serverutil.paper.module.ServerUtilModule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Suppresses noisy vanilla/server console log lines matching config.yml's
 * {@code log-filter.suppressed-patterns} (Java regex, case-insensitive, matched anywhere in
 * the formatted message) - e.g. the "Named entity X died" line vanilla logs for every
 * custom-named entity's death (every Orelia-tagged monster carries a custom name for its
 * HP-bar nametag, so every single kill would otherwise spam this) or "moved wrongly"/"moved
 * too quickly" anti-cheat warnings that custom mob AI/teleporting can trigger.
 *
 * <p>There's no Bukkit-level API for this - these lines come from Minecraft's own internal
 * server code logging directly through Log4j2, not through a plugin {@code Logger}. Attaches a
 * {@link SuppressedMessageFilter} to the root {@link LoggerConfig} instead, the same mechanism
 * a {@code log4j2.xml} filter config would use, just applied at runtime from config.yml so it
 * can be changed with {@code /suadmin reload} instead of needing a server restart.
 */
public final class LogFilterModule implements ServerUtilModule {

    private OreliaServerUtilPlugin plugin;
    private SuppressedMessageFilter activeFilter;

    @Override
    public String getName() {
        return "log-filter";
    }

    @Override
    public void onEnable(OreliaServerUtilPlugin plugin) {
        this.plugin = plugin;
        applyFilter();
    }

    @Override
    public void onDisable() {
        removeFilter();
    }

    @Override
    public void onReload() {
        removeFilter();
        applyFilter();
    }

    private void applyFilter() {
        var config = plugin.getConfigManager().get("config.yml").get();
        List<String> rawPatterns = config.getStringList("log-filter.suppressed-patterns");
        List<Pattern> patterns = new ArrayList<>();
        for (String raw : rawPatterns) {
            try {
                patterns.add(Pattern.compile(raw, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                plugin.getLogger().warning("Invalid log-filter.suppressed-patterns regex, skipping: " + raw);
            }
        }
        if (patterns.isEmpty()) {
            return;
        }
        activeFilter = new SuppressedMessageFilter(patterns);
        rootLoggerConfig().addFilter(activeFilter);
        loggerContext().updateLoggers();
    }

    /** Removes exactly the filter instance this module added - never touches a filter some other plugin/config might have attached. */
    private void removeFilter() {
        if (activeFilter == null) {
            return;
        }
        rootLoggerConfig().removeFilter(activeFilter);
        loggerContext().updateLoggers();
        activeFilter = null;
    }

    private LoggerConfig rootLoggerConfig() {
        return loggerContext().getConfiguration().getRootLogger();
    }

    private LoggerContext loggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }
}
