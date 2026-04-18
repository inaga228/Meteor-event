package ru.inaga228.meteorevent.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class MessageUtil {

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(color(message));
    }
}
