package com.crewstations.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

/**
 * Thin logging wrapper so all mod logs share a consistent prefix and a single
 * logger category. Use {@code CSLog.info(...)} etc. instead of calling
 * {@code Global.getLogger(...)} directly.
 */
public final class CSLog {

    private CSLog() {}

    private static final String PREFIX = "[CrewStations] ";

    private static Logger log() {
        return Global.getLogger(CSLog.class);
    }

    public static void info(String msg) {
        log().info(PREFIX + msg);
    }

    public static void warn(String msg) {
        log().warn(PREFIX + msg);
    }

    public static void error(String msg, Throwable t) {
        log().error(PREFIX + msg, t);
    }
}
