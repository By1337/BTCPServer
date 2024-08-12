package org.by1337.btcp.server.addon;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class AddonLogger implements Logger {
    private final Logger logger;
    private final String prefix;

    public AddonLogger(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }

    private String appendPrefix(String msg) {
        return prefix + " " + msg;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(appendPrefix(msg));
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(appendPrefix(format), arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(appendPrefix(format), arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(appendPrefix(format), arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(appendPrefix(msg), t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, appendPrefix(msg));
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, appendPrefix(format), arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, appendPrefix(format), arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker, appendPrefix(format), argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, appendPrefix(msg), t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(appendPrefix(msg));
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(appendPrefix(format), arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(appendPrefix(format), arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(appendPrefix(format), arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(appendPrefix(msg), t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, appendPrefix(msg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, appendPrefix(format), arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, appendPrefix(format), arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, appendPrefix(format), arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, appendPrefix(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(appendPrefix(msg));
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(appendPrefix(format), arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(appendPrefix(format), arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(appendPrefix(format), arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(appendPrefix(msg), t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, appendPrefix(msg));
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, appendPrefix(format), arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, appendPrefix(format), arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, appendPrefix(format), arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, appendPrefix(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(appendPrefix(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(appendPrefix(format), arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(appendPrefix(format), arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(appendPrefix(format), arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(appendPrefix(msg), t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, appendPrefix(msg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, appendPrefix(format), arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, appendPrefix(format), arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, appendPrefix(format), arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, appendPrefix(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(appendPrefix(msg));
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(appendPrefix(format), arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(appendPrefix(format), arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(appendPrefix(format), arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(appendPrefix(msg), t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, appendPrefix(msg));
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, appendPrefix(format), arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, appendPrefix(format), arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, appendPrefix(format), arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, appendPrefix(msg), t);
    }
}
