package com.meteordevelopments.dumboEssentials.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.meteordevelopments.dumboEssentials.DumboEssentials;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    DumboEssentials plugin = DumboEssentials.getPlugin();
    FileConfiguration config = plugin.getConfig();

    String nether = config.getString("nether");
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(nether)) return;
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
        if (sp == null) return;
        Island island = sp.getIsland();
        if (island == null) return;
        if (island.isNetherEnabled()) island.setNetherEnabled(false);
    }
}
