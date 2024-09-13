package com.meteordevelopments.dumboEssentials.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtility {

    public static String translate(String message){
        if (message == null) return "";
        if (Pattern.compile("&#[0-9A-f]{6}").matcher(message).find()) {
            Matcher matcher = Pattern.compile("&(#[0-9A-f]{6})").matcher(message);
            while (matcher.find()) {
                message = message.replaceFirst(
                        matcher.group(),
                        ChatColor.of(matcher.group(1)).toString()
                );
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String replacePlaceholders(String message, String... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtility.translate(message);
    }

}
