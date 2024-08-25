package com.meteordevelopments.dumboEssentials;

import com.meteordevelopments.dumboEssentials.listeners.BlockListener;
import com.meteordevelopments.dumboEssentials.listeners.JoinListener;
import com.meteordevelopments.dumboEssentials.placeholders.Placeholders;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import lombok.Getter;
import me.elementalgaming.ElementalGems.GemAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class DumboEssentials extends JavaPlugin implements Listener, CommandExecutor {
    @Getter
    public static DumboEssentials plugin;
    private long regrowthTime;
    private Set<Block> wheatBlocks = new HashSet<>();
    private Set<String> enabledWorlds = new HashSet<>();
    private long nextRegrowthTime;
    private int pos1X, pos1Y, pos1Z;
    private int pos2X, pos2Y, pos2Z;
    private double gemChance;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("dumboessentials")).setExecutor(this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders().register();
        }

        startRegrowthTask();
    }
    private void loadConfig() {
        FileConfiguration config = getConfig();
        regrowthTime = config.getLong("regrowth-time", 900);
        enabledWorlds.addAll(config.getStringList("worlds"));

        pos1X = config.getInt("positions.pos1.x", 100);
        pos1Y = config.getInt("positions.pos1.y", 64);
        pos1Z = config.getInt("positions.pos1.z", -100);
        pos2X = config.getInt("positions.pos2.x", 200);
        pos2Y = config.getInt("positions.pos2.y", 80);
        pos2Z = config.getInt("positions.pos2.z", 100);

        gemChance = config.getDouble("gem-chance", 0.25) / 100.0; // Convert percentage to decimal

        nextRegrowthTime = System.currentTimeMillis() / 1000 + regrowthTime;
    }

    private void startRegrowthTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String worldName : enabledWorlds) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        // Regrow all wheat blocks in the defined block area
                        for (int x = Math.min(pos1X, pos2X); x <= Math.max(pos1X, pos2X); x++) {
                            for (int y = Math.min(pos1Y, pos2Y); y <= Math.max(pos1Y, pos2Y); y++) {
                                for (int z = Math.min(pos1Z, pos2Z); z <= Math.max(pos1Z, pos2Z); z++) {
                                    Block block = world.getBlockAt(x, y, z);
                                    if (block.getType() == Material.WHEAT) {
                                        regrowBlock(block);
                                    }
                                }
                            }
                        }
                    }
                }
                wheatBlocks.clear();
                nextRegrowthTime = System.currentTimeMillis() / 1000 + regrowthTime;
            }
        }.runTaskTimer(this, regrowthTime * 20L, regrowthTime * 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (block.getType() == Material.WHEAT && enabledWorlds.contains(block.getWorld().getName())) {
            if (isWithinConfiguredArea(block.getLocation())) {
                event.setDropItems(false); // Prevent drops
                block.setType(Material.WHEAT);
                wheatBlocks.add(block);
                BlockState state = block.getState();
                if (state.getBlockData() instanceof Ageable) {
                    Ageable ageable = (Ageable) state.getBlockData();
                    ageable.setAge(0); // Set to seed stage
                    state.setBlockData(ageable);
                    state.update(true, false);
                    // Random chance to add gems
                    Random random = new Random();
                    if (random.nextDouble() < gemChance) { // Use configurable chance
                        GemAPI.addGems(player.getUniqueId(), 1);
                        player.sendMessage(ColorUtility.translate(getConfig().getString("gem-drop-message")));
                    }
                }
            }
        }
    }

    private void regrowBlock(Block block) {
        BlockState state = block.getState();
        if (state.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) state.getBlockData();
            ageable.setAge(ageable.getMaximumAge());
            state.setBlockData(ageable);
            state.update(true, false);
        }
    }

    private boolean isWithinConfiguredArea(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= Math.min(pos1X, pos2X) && x <= Math.max(pos1X, pos2X)
                && y >= Math.min(pos1Y, pos2Y) && y <= Math.max(pos1Y, pos2Y)
                && z >= Math.min(pos1Z, pos2Z) && z <= Math.max(pos1Z, pos2Z);
    }

    public String getTimeUntilNextRegrowth() {
        long currentTime = System.currentTimeMillis() / 1000;
        long timeRemaining = nextRegrowthTime - currentTime;

        long minutes = timeRemaining / 60;
        long seconds = timeRemaining % 60;

        return minutes + "m " + seconds + "s";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("dumboessentials") && sender.hasPermission("dumboessentials.reload")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage("Configuration reloaded.");
                return true;
            } else {
                sender.sendMessage("Usage: /dumboessentials reload");
                return false;
            }
        }
        return false;
    }
}
