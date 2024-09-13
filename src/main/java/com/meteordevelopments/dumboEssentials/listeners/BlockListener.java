package com.meteordevelopments.dumboEssentials.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

public class BlockListener implements Listener {

    DumboEssentials plugin = DumboEssentials.getPlugin();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        SuperiorPlayer splayer = SuperiorSkyblockAPI.getPlayer(player);
        List<String> restrictedBlocks = plugin.getConfiguration().getStringList("nether-restricted-blocks");
        Material placedBlocks = event.getBlock().getType();

        if (restrictedBlocks.contains(placedBlocks.toString().toUpperCase())) {
            if (splayer != null) {
                Island island = splayer.getIsland();
                if (splayer.isInsideIsland() && island != null) {
                    if (!island.isNetherEnabled()) {
                        event.setCancelled(true);
                        player.sendMessage(ColorUtility.translate(plugin.getConfiguration().getString("not-unlocked-message")));
                    }
                }
            }
        }
    }
}
