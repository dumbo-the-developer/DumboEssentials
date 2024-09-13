package com.meteordevelopments.dumboEssentials.listeners;

import com.meteordevelopments.dumboEssentials.DumboEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import java.util.Objects;

public class PistonListener implements Listener {

    DumboEssentials plugin = DumboEssentials.getPlugin();

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!Objects.requireNonNull(plugin.getConfiguration().getList("island-worlds")).contains(event.getSource().getWorld().getName())) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> block(event), 20L);
    }

    private boolean isPiston(Location b, Block block) {
        if (b.getBlock().getType() == Material.PISTON) {
            BlockFace blockFace = ((Directional) b.getBlock().getState().getBlockData()).getFacing();
            BlockFace required = b.getBlock().getFace(block);
            if (blockFace == required) {
                block.breakNaturally();
            }

            return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!Objects.requireNonNull(plugin.getConfiguration().getList("island-worlds")).contains(event.getBlock().getWorld().getName())) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (event.getBlock().getType() != Material.SUGAR_CANE && event.getBlock().getType() != Material.MELON && event.getBlock().getType() != Material.PUMPKIN && event.getBlock().getType() != Material.KELP_PLANT) return;
            block(event);
        }, 10L);
    }

    private void block(BlockEvent event) {
        Location o = event.getBlock().getLocation();
        int r = 1;
        for(int x = (r * -1); x <= r; x++) {
            for(int y = (r * -1); y <= r; y++) {
                for(int z = (r * -1); z <= r; z++) {
                    Location b = new Location(o.getWorld(), o.getBlockX() + x, o.getBlockY() + y, o.getBlockZ() + z);

                    if(b.distance(o) > r)
                        continue;

                    if(b.distance((o)) < 0)
                        continue;

                    if (isPiston(b, event.getBlock())) break;
                }
            }
        }
    }
}
