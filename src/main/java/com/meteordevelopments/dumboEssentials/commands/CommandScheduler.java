package com.meteordevelopments.dumboEssentials.commands;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.Duration;

public class CommandScheduler {

    DumboEssentials plugin = DumboEssentials.getPlugin();
    private final ScheduledExecutorService scheduler;
    private final long firstCommandDelayHours;
    private final long secondCommandDelayMinutes;

    public CommandScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Load delays from config
        firstCommandDelayHours = plugin.getConfiguration().getLong("firstCommandDelayHours", 3); // Default to 3 hours
        secondCommandDelayMinutes = plugin.getConfiguration().getLong("secondCommandDelayMinutes", 20); // Default to 20 minutes
    }

    public void start() {
        // Get the last time the command was executed from the config
        String lastRunTimeStr = plugin.getConfiguration().getString("lastRunTime");
        LocalDateTime lastRunTime = (lastRunTimeStr != null) ? LocalDateTime.parse(lastRunTimeStr) : null;

        // Calculate the elapsed time since the last run
        if (lastRunTime != null) {
            Duration timeSinceLastRun = Duration.between(lastRunTime, LocalDateTime.now());
            long elapsedHours = timeSinceLastRun.toHours();

            // Execute the first command for each elapsed interval
            while (elapsedHours >= firstCommandDelayHours) {
                runFirstCommand();
                elapsedHours -= firstCommandDelayHours;
            }
        }

        // Schedule the first command execution every 'firstCommandDelayHours' hours
        scheduler.scheduleAtFixedRate(this::runFirstCommand, calculateInitialDelay(lastRunTime), firstCommandDelayHours, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdownNow(); // Ensure scheduled tasks are stopped when the plugin is disabled
    }

    private void runFirstCommand() {
        // Use a BukkitRunnable to execute the command on the main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                // Execute your first command here
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "boss spawn Spawn 2000 130 2020 nemere");

                // Update the last run time in the configuration
                LocalDateTime now = LocalDateTime.now();
                plugin.getConfiguration().set("lastRunTime", now.toString());
                plugin.getConfiguration().save();

                // Schedule the second command to run 'secondCommandDelayMinutes' minutes later
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        runSecondCommand();
                    }
                }.runTaskLater(plugin, secondCommandDelayMinutes * 60 * 20); // minutes * seconds * 20 ticks
            }
        }.runTask(plugin); // Run this on the main thread
    }

    private void runSecondCommand() {
        // Use a BukkitRunnable to execute the command on the main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                // Execute your second command here
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "boss butcher Spawn");
            }
        }.runTask(plugin); // Run this on the main thread
    }

    private long calculateInitialDelay(LocalDateTime lastRunTime) {
        if (lastRunTime == null) {
            return 0; // Run immediately if no previous run time is found
        }

        // Calculate the delay until the next 'firstCommandDelayHours' interval
        LocalDateTime now = LocalDateTime.now();
        Duration timeSinceLastRun = Duration.between(lastRunTime, now);
        long timeSinceLastRunHours = timeSinceLastRun.toHours();

        if (timeSinceLastRunHours >= firstCommandDelayHours) {
            return 0; // Run immediately if it's been more than 'firstCommandDelayHours'
        }

        return (firstCommandDelayHours - timeSinceLastRunHours) * 60 * 60; // Return delay in seconds
    }
}
