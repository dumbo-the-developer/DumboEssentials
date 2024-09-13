package com.meteordevelopments.dumboEssentials.islands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FlyChecker {

    DumboEssentials plugin = DumboEssentials.getPlugin();

    public void startChecking() {
        // Run a repeating task every second to check permissions
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkTemporaryPermission(player, "superior.island.fly");
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // Run every second
    }

    private void checkTemporaryPermission(Player player, String permission) {
        SuperiorPlayer splayer = SuperiorSkyblockAPI.getPlayer(player);
        if (splayer != null && splayer.isInsideIsland()) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());

            if (user == null) {
                return;
            }

            // Find the permission node
            Optional<Node> node = user.getNodes().stream()
                    .filter(n -> n.getKey().equalsIgnoreCase(permission) && n.hasExpiry())
                    .findFirst();

            if (node.isPresent() && node.get().hasExpiry()) {
                // Calculate remaining time
                long remainingTime = Objects.requireNonNull(node.get().getExpiry()).getEpochSecond() - System.currentTimeMillis() / 1000;

                if (remainingTime > 0) {
                    // Get action bar message from config
                    String actionBarMessage = ColorUtility.translate(plugin.getConfiguration().getString("fly.message"));
                    actionBarMessage = actionBarMessage.replace("%time%", formatDuration(remainingTime));

                    // Display remaining time in action bar
                    sendActionBar(player, actionBarMessage);
                } else {
                    splayer.setIslandFly(false);
                }
            }else {
                // The player does not have the temporary permission or it has no expiry
                player.setAllowFlight(false);
                player.setFlying(false);
                splayer.setIslandFly(false);
            }
        }
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(message); // Requires Paper or a similar library
    }

    private String formatDuration(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        String d = ColorUtility.translate(plugin.getConfiguration().getString("fly.days")),
                h = ColorUtility.translate(plugin.getConfiguration().getString("fly.hours")),
                m = ColorUtility.translate(plugin.getConfiguration().getString("fly.minutes")),
                s = ColorUtility.translate(plugin.getConfiguration().getString("fly.seconds"));

        return days + d + hours + h + minutes + m + seconds + s;
    }
}