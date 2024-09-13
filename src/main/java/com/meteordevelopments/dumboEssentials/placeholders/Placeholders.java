package com.meteordevelopments.dumboEssentials.placeholders;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.meteordevelopments.dumboEssentials.DumboEssentials;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Placeholders extends PlaceholderExpansion {

    DumboEssentials plugin = DumboEssentials.getPlugin();

    @Override
    @NotNull
    public String getIdentifier() {
        return "nnb";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "DUMBO";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0-SNAPSHOT";
    }

    @Override
    public String onRequest(OfflinePlayer player, String placeholder) {
        if (placeholder.equalsIgnoreCase("nether")) {
            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
            if (sp != null) {
                Island island = sp.getIsland();
                if (island != null) {
                    if (island.isNetherEnabled()) {
                        return Objects.requireNonNull(plugin.getConfiguration().getString("nether-enabled"));
                    }else return Objects.requireNonNull(plugin.getConfiguration().getString("nether-disabled"));
                }
            }
        }
        if (placeholder.equals("time_remaining")) {
            return plugin.getTimeUntilNextRegrowth();
        }
        return null;
    }
}
