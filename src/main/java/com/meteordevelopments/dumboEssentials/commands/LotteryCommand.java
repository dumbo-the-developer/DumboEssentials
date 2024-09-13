package com.meteordevelopments.dumboEssentials.commands;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class LotteryCommand implements CommandExecutor {

    DumboEssentials plugin = DumboEssentials.getPlugin();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lottery")) {
            if (sender instanceof Player player) {
                if (player.hasPermission("dumboessentials.lottery")) {
                    startLottery();
                }
                return true;
            } else {
                sender.sendMessage("This command can only be run by a player.");
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void startLottery() {
        new BukkitRunnable() {
            int countdown = 10; // Countdown from 10 seconds

            @Override
            public void run() {
                if (countdown > 0) {
                    String countdownMessage = plugin.getConfiguration().getString("lottery.countdown", "&eLottery starts in %seconds% seconds!")
                            .replace("%seconds%", String.valueOf(countdown));
                    Bukkit.broadcastMessage(ColorUtility.translate(countdownMessage));
                    countdown--;
                } else {
                    List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
                    if (players.isEmpty()) {
                        String noPlayersMessage = plugin.getConfiguration().getString("lottery.no_players", "&cNo players are online to participate in the lottery.");
                        Bukkit.broadcastMessage(ColorUtility.translate(noPlayersMessage));
                    } else {
                        Player winner = players.get(new Random().nextInt(players.size()));
                        String winnerMessage = plugin.getConfiguration().getString("lottery.winner", "&6Congratulations %winner%! You have won the lottery!")
                                .replace("%winner%", winner.getName());
                        Bukkit.broadcastMessage(ColorUtility.translate(winnerMessage));
                    }
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // Schedule the task every 20 ticks (1 second)
    }
}
