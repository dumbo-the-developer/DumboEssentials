package com.meteordevelopments.dumboEssentials.logger;


import com.meteordevelopments.dumboEssentials.DumboEssentials;

import java.util.logging.Level;

public class Logger {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("Minecraft");
    private static final String PREFIX = DumboEssentials.getPlugin().getPrefix();

    private static void log(Level level, String content) {
        LOGGER.log(level, PREFIX + " " + content);
    }

    public static void info(String content) {
        log(Level.INFO, content);
    }

    public static void warn(String content) {
        log(Level.WARNING, content);
    }

    public static void severe(String content) {
        log(Level.SEVERE, content);
    }

}