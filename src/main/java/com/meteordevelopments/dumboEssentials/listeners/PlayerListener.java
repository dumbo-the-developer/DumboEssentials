package com.meteordevelopments.dumboEssentials.listeners;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PlayerListener implements Listener {

    DumboEssentials plugin = DumboEssentials.getPlugin();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (item != null && block != null && player.hasPermission("dumboessentials.admin") && item.getType() == Material.NAME_TAG) {
            Location loc = block.getLocation();
            Material material = loc.getBlock().getType();

            String materialKey = "ores." + material.toString().toLowerCase();
            String locationKey = materialKey + ".locations";

            List<String> locations = plugin.getMineConfig().getStringList(locationKey);
            if (!locations.contains(loc.toString())) {
                locations.add(loc.toString());
                plugin.getMineConfig().set(locationKey, locations);
            }

            plugin.getMineConfig().set(materialKey + ".material", material.toString());
            plugin.getMineConfig().save();

            // Immediately reload the config and update the oreMap
            plugin.reloadOres();

            player.sendMessage(ColorUtility.translate("&a&lSuccessfully Saved block and updated configuration."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Location blockLocation = block.getLocation();

        // Check if the broken block is one of the ores in the config
        if (plugin.getOreMap().containsKey(blockLocation)) {
            Material originalMaterial = plugin.getOreMap().get(blockLocation);

            // Cancel the default block drop
            event.setDropItems(false);

            // Add the block item to the player's inventory
            ItemStack itemStack = new ItemStack(originalMaterial);
            player.getInventory().addItem(itemStack);

            // Replace the block with Bedrock
            block.setType(Material.BEDROCK);

            // Store the reversion time in the config
            long revertTime = System.currentTimeMillis() + (20 * 1000); // 20 seconds from now
            String blockKey = blockLocation.getWorld().getName() + "," + blockLocation.getBlockX() + "," + blockLocation.getBlockY() + "," + blockLocation.getBlockZ();
            plugin.getMineConfig().set("reverting." + blockKey + ".material", originalMaterial.toString());
            plugin.getMineConfig().set("reverting." + blockKey + ".time", revertTime);
            plugin.getMineConfig().save();

            // Schedule a task to revert back to the original ore after 20 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.revertBlock(blockLocation);
                }
            }.runTaskLater(plugin, 20 * 20); // 20 seconds delay
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfiguration().getBoolean("chat-disabled")) {
            if (!player.hasPermission("dumboessentials.admin")) event.setCancelled(true);
            player.sendMessage(ColorUtility.translate(plugin.getConfiguration().getString("chat-disabled-message")));
        }
    }

}
