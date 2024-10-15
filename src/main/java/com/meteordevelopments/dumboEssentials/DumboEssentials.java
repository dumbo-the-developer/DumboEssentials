package com.meteordevelopments.dumboEssentials;

import com.meteordevelopments.dumboEssentials.commands.BuyRankCommand;
import com.meteordevelopments.dumboEssentials.commands.ChatCommand;
import com.meteordevelopments.dumboEssentials.commands.LotteryCommand;
import com.meteordevelopments.dumboEssentials.configs.Config;
import com.meteordevelopments.dumboEssentials.listeners.BlockListener;
import com.meteordevelopments.dumboEssentials.listeners.JoinListener;
import com.meteordevelopments.dumboEssentials.listeners.PistonListener;
import com.meteordevelopments.dumboEssentials.listeners.PlayerListener;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class DumboEssentials extends JavaPlugin implements Listener, CommandExecutor {
    @Getter
    public static DumboEssentials plugin;
    @Getter
    private Config configuration;
    @Getter
    private Config mineConfig;
    @Getter
    private Config rankConfig;
    @Getter
    private Map<Location, Material> oreMap = new HashMap<>();
    private long regrowthTime;
    private final Set<String> enabledWorlds = new HashSet<>();
    private long nextRegrowthTime;
    private int pos1X, pos1Y, pos1Z;
    private int pos2X, pos2Y, pos2Z;
    private double gemChance;

    @Override
    public void onEnable() {
        plugin = this;
        configuration = new Config("config.yml");
        mineConfig = new Config("mine.yml");
        rankConfig = new Config("ranks.yml");
        loadConfig();
        loadOresFromConfig();
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new PistonListener(), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        Objects.requireNonNull(getCommand("dumboessentials")).setExecutor(this);
        Objects.requireNonNull(getCommand("chat")).setExecutor(new ChatCommand());
        Objects.requireNonNull(getCommand("lottery")).setExecutor(new LotteryCommand());
        Objects.requireNonNull(getCommand("buyrank")).setExecutor(new BuyRankCommand());
        Objects.requireNonNull(getCommand("createshop")).setExecutor(new BuyRankCommand());
        Objects.requireNonNull(getCommand("giverankpaper")).setExecutor(new BuyRankCommand());
        Objects.requireNonNull(getCommand("buyrank")).setTabCompleter(new BuyRankCommand());
        Objects.requireNonNull(getCommand("giverankpaper")).setTabCompleter(new BuyRankCommand());

        startRegrowthTask();
        restoreBlocks();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders().register();
        }
    }

    private void loadConfig() {
        regrowthTime = configuration.getLong("regrowth-time", 900);
        enabledWorlds.addAll(configuration.getStringList("worlds"));

        pos1X = configuration.getInt("positions.pos1.x", 100);
        pos1Y = configuration.getInt("positions.pos1.y", 64);
        pos1Z = configuration.getInt("positions.pos1.z", -100);
        pos2X = configuration.getInt("positions.pos2.x", 200);
        pos2Y = configuration.getInt("positions.pos2.y", 80);
        pos2Z = configuration.getInt("positions.pos2.z", 100);

        gemChance = configuration.getDouble("gem-chance", 0.25) / 100.0; // Convert percentage to decimal

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
                for (Player player : Bukkit.getOnlinePlayers()) {
                    List<String> announcements = getConfiguration().getStringList("regen-announcement");
                    for (String announcement : announcements) {
                        announcement = ColorUtility.translate(announcement);
                        player.sendMessage(announcement);
                    }
                }
                nextRegrowthTime = System.currentTimeMillis() / 1000 + regrowthTime;
            }
        }.runTaskTimer(this, regrowthTime * 20L, regrowthTime * 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if the block is wheat and in the enabled world
        if (block.getType() == Material.WHEAT && enabledWorlds.contains(block.getWorld().getName())) {

            // Check if the block is within the configured area
            if (isWithinConfiguredArea(block.getLocation())) {

                // Get the block state and block data
                BlockState state = block.getState();

                // Check if the block data is an instance of Ageable (Wheat is ageable)
                if (state.getBlockData() instanceof Ageable ageable) {

                    // Check if the wheat is fully grown (maximum age)
                    if (ageable.getAge() == ageable.getMaximumAge()) {

                        // Prevent default drops and reset wheat to age 0
                        event.setDropItems(false);
                        ageable.setAge(0);
                        state.setBlockData(ageable);
                        state.update(true, false);

                        // Random chance to add gems
                        Random random = new Random();
                        if (random.nextDouble() < gemChance) { // Use configurable chance
                            GemAPI.addGems(player.getUniqueId(), 1);
                            player.sendMessage(ColorUtility.translate(getConfiguration().getString("gem-drop-message")));
                        }
                    }
                }
            }
        }
    }


    private void regrowBlock(Block block) {
        BlockState state = block.getState();
        if (state.getBlockData() instanceof Ageable ageable) {
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

        return minutes + plugin.getConfiguration().getString("minutes") + " " + seconds + plugin.getConfiguration().getString("seconds");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("dumboessentials") && sender.hasPermission("dumboessentials.reload")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                getMineConfig().reload();
                getConfiguration().reload();
                getRankConfig().reload();
                loadConfig();
                new Placeholders().unregister();
                new Placeholders().register();
                sender.sendMessage("Configuration and placeholders reloaded.");
                return true;
            } else {
                sender.sendMessage("Usage: /dumboessentials reload");
                return false;
            }
        }
        return false;
    }

    public String getPrefix() {
        return ColorUtility.translate(configuration.getString("prefix"));
    }

    public void revertBlock(Location location) {
        String blockKey = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        Material originalMaterial = Material.valueOf(plugin.getMineConfig().getString("reverting." + blockKey + ".material"));

        // Revert the block to its original material
        location.getBlock().setType(originalMaterial);

        // Remove the block entry from the config after reverting
        plugin.getMineConfig().set("reverting." + blockKey, null);
        plugin.getMineConfig().save();
    }

    public void loadOresFromConfig() {
        if (!mineConfig.isConfigurationSection("ores")) {
            Bukkit.getLogger().warning("No ores section found in the configuration!");
            return; // Exit if the section is missing
        }

        // Iterate through each ore type in the config
        for (String oreType : mineConfig.getConfigurationSection("ores").getKeys(false)) {
            if (!mineConfig.isSet("ores." + oreType + ".material") || !mineConfig.isSet("ores." + oreType + ".locations")) {
                Bukkit.getLogger().warning("Missing material or locations for " + oreType + " in config!");
                continue; // Skip this entry if material or locations are missing
            }

            Material material;
            try {
                material = Material.valueOf(mineConfig.getString("ores." + oreType + ".material"));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().severe("Invalid material for " + oreType + " in config!");
                continue; // Skip invalid material
            }

            List<String> locationStrings = mineConfig.getStringList("ores." + oreType + ".locations");
            if (locationStrings.isEmpty()) {
                Bukkit.getLogger().warning("No locations specified for " + oreType + " in config!");
                continue; // Skip if no locations are specified
            }

            for (String locString : locationStrings) {
                Location location = parseLocation(locString);
                if (location != null) {
                    oreMap.put(location, material);
                }
            }
        }
    }

    public void restoreBlocks() {
        if (plugin.getMineConfig().isConfigurationSection("reverting")) {
            long currentTime = System.currentTimeMillis();

            for (String blockKey : plugin.getMineConfig().getConfigurationSection("reverting").getKeys(false)) {
                long revertTime = plugin.getMineConfig().getLong("reverting." + blockKey + ".time");

                // Declare variables outside of the loop
                World world = null;
                int x = 0, y = 0, z = 0;

                if (revertTime <= currentTime) {
                    // Extract the block's location details from the config key
                    String[] parts = blockKey.split(",");
                    world = Bukkit.getWorld(parts[0]);
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                    z = Integer.parseInt(parts[3]);
                    Location location = new Location(world, x, y, z);

                    // Revert the block immediately
                    revertBlock(location);
                } else {
                    // Extract the block's location details from the config key
                    String[] parts = blockKey.split(",");
                    world = Bukkit.getWorld(parts[0]);
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                    z = Integer.parseInt(parts[3]);

                    // Schedule a task to revert the block at the correct time
                    World finalWorld = world;
                    int finalX = x, finalY = y, finalZ = z;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            revertBlock(new Location(finalWorld, finalX, finalY, finalZ));
                        }
                    }.runTaskLater(this, (revertTime - currentTime) / 50); // Convert milliseconds to ticks
                }
            }
        }
    }

    private Location parseLocation(String locationString) {
        try {
            String[] parts = locationString.replace("Location{", "").replace("}", "").split(",");
            String worldName = parts[0].split("=")[2];
            double x = Double.parseDouble(parts[1].split("=")[1]);
            double y = Double.parseDouble(parts[2].split("=")[1]);
            double z = Double.parseDouble(parts[3].split("=")[1]);
            return new Location(Bukkit.getWorld(worldName), x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void reloadOres() {
        mineConfig.reload(); // Reload the config file
        oreMap.clear(); // Clear the existing map

        loadOresFromConfig();
    }

    @Override
    public void onDisable() {
        new Placeholders().unregister();
    }
}
