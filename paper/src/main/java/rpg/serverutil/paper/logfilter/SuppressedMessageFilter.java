package rpg.serverutil.paper.logfilter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Denies any {@link LogEvent} whose formatted message matches one of the configured patterns -
 * see {@link rpg.serverutil.paper.logfilter.LogFilterModule} for where this gets attached.
 * Only {@link #filter(LogEvent)} is overridden: a filter attached to a {@code LoggerConfig}
 * (as opposed to a {@code Logger} instance directly) is only ever consulted through that one
 * method, since by the time a {@code LoggerConfig} processes an event it's already been fully
 * constructed - the other {@code AbstractFilter} overloads exist for filters attached earlier,
 * directly on a {@code Logger}, which isn't how this one is used.
 */
final class SuppressedMessageFilter extends AbstractFilter {

    private final List<Pattern> patterns;

    SuppressedMessageFilter(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    @Override
    public Result filter(LogEvent event) {
        String message = event.getMessage() == null ? null : event.getMessage().getFormattedMessage();
        if (message == null) {
            return Result.NEUTRAL;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(message).find()) {
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }
}
