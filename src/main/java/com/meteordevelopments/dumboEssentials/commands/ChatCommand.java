package com.meteordevelopments.dumboEssentials.commands;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatCommand implements CommandExecutor {
    DumboEssentials plugin = DumboEssentials.getPlugin();
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player player = (Player) commandSender;
        if (args.length == 1 && args[0].equalsIgnoreCase("enable")) {
            if (player.hasPermission("group.admin")) {
                plugin.getConfiguration().set("chat-disabled", false);
                plugin.getConfiguration().save();
                plugin.getConfiguration().reload();
                player.sendMessage(ColorUtility.translate(plugin.getConfiguration().getString("chat-enable-command-message")));
            }
        }else if (args.length == 1 && args[0].equalsIgnoreCase("disable")) {
            if (player.hasPermission("group.admin")) {
                plugin.getConfiguration().set("chat-disabled", true);
                plugin.getConfiguration().save();
                plugin.getConfiguration().reload();
                player.sendMessage(ColorUtility.translate(plugin.getConfiguration().getString("chat-disable-command-message")));
            }
        }
        return true;
    }
}
