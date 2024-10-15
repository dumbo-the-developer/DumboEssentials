package com.meteordevelopments.dumboEssentials.commands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.meteordevelopments.dumboEssentials.DumboEssentials;
import com.meteordevelopments.dumboEssentials.utils.ColorUtility;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.meteordevelopments.dumboEssentials.utils.ColorUtility.replacePlaceholders;

@SuppressWarnings({"deprecation", "all"})
public class BuyRankCommand implements CommandExecutor, TabCompleter {
    private final Economy economy;
    DumboEssentials plugin = DumboEssentials.getPlugin();

    public BuyRankCommand() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        assert rsp != null;
        this.economy = rsp.getProvider();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("buyrank") && sender instanceof Player) {
            return handleBuyRank((Player) sender, args);
        } else if (command.getName().equalsIgnoreCase("createshop") && sender instanceof Player) {
            return handleCreateShop((Player) sender, args);
        } else if (command.getName().equalsIgnoreCase("giverankpaper") && sender instanceof Player) {
            return handleGiveRankPaper((Player) sender, args);
        }
        return false;
    }

    private boolean handleBuyRank(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.usage.buyrank")));
            return true;
        }

        String rankKey = args[0].toLowerCase();
        if (!plugin.getRankConfig().contains("ranks." + rankKey)) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.rank_not_found")));
            return true;
        }

        // Check if the player has already purchased the requested rank
        List<String> purchasedRanks = plugin.getRankConfig().getStringList("players." + player.getUniqueId() + ".purchased_ranks");
        if (purchasedRanks.contains(rankKey)) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.rank_already_owned")));
            return true;
        }

        // Check for previous rank requirement
        String previousRankKey = plugin.getRankConfig().getString("ranks." + rankKey + ".previous_rank");
        if (previousRankKey != null && !hasRank(player, previousRankKey)) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.previous_rank_required")));
            return true;
        }

        int cost = plugin.getRankConfig().getInt("ranks." + rankKey + ".cost");
        int requiredPapers = plugin.getRankConfig().getInt("ranks." + rankKey + ".required_papers");
        BigInteger minLevel = new BigInteger(plugin.getRankConfig().getString("ranks." + rankKey + ".min_island_level", "0"));
        SuperiorPlayer splayer = SuperiorSkyblockAPI.getPlayer(player);
        BigInteger playerLevel = splayer.getIsland().getIslandLevel().toBigInteger();

        if (playerLevel.compareTo(minLevel) < 0) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.insufficient_level")));
            return true;
        }

        String paperName = ColorUtility.translate(plugin.getRankConfig().getString("rank_paper.display_name"));
        List<String> paperLore = plugin.getRankConfig().getStringList("rank_paper.lore").stream()
                .map(ColorUtility::translate)
                .collect(Collectors.toList());

        if (!hasEnoughCurrency(player, cost) || !hasEnoughRankPapers(player, paperName, paperLore, requiredPapers)) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.insufficient_resources")));
            return true;
        }

        deductCurrency(player, cost);
        deductRankPapers(player, paperName, paperLore, requiredPapers);
        grantRank(player, rankKey);

        player.sendMessage(replacePlaceholders(plugin.getRankConfig().getString("messages.success.rank_purchased"),
                "{rank}", rankKey));
        return true;
    }

    private boolean hasRank(Player player, String rankKey) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getNodes().stream().anyMatch(node -> node.getKey().equals("group." + rankKey));
        }
        return false;
    }

    private boolean handleGiveRankPaper(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.usage.giverankpaper")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.player_not_found")));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.invalid_number")));
            return true;
        }

        String paperName = ColorUtility.translate(plugin.getRankConfig().getString("rank_paper.display_name"));
        List<String> paperLore = plugin.getRankConfig().getStringList("rank_paper.lore").stream()
                .map(ColorUtility::translate)
                .collect(Collectors.toList());

        ItemStack rankPaper = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = rankPaper.getItemMeta();
        meta.setDisplayName(paperName);
        meta.setLore(paperLore);
        rankPaper.setItemMeta(meta);

        target.getInventory().addItem(rankPaper);
        player.sendMessage(replacePlaceholders(plugin.getRankConfig().getString("messages.success.rankpaper_given"),
                "{amount}", String.valueOf(amount),
                "{player}", target.getName(),
                "{rank_paper_name}", paperName));
        return true;
    }

    private boolean handleCreateShop(Player player, String[] args) {
        if (args.length < 5) { // Updated to check for 5 arguments
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.usage.createshop")));
            return true;
        }

        String rankKey = args[0].toLowerCase();
        int cost;
        int requiredPapers;
        BigInteger minLevel;
        String previousRankKey = args[4].toLowerCase(); // New argument for previous rank

        try {
            cost = Integer.parseInt(args[1]);
            requiredPapers = Integer.parseInt(args[2]);
            minLevel = new BigInteger(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.error.invalid_number")));
            return true;
        }

        // Save the new rank shop configuration, including previous rank
        plugin.getRankConfig().set("ranks." + rankKey + ".name", rankKey);
        plugin.getRankConfig().set("ranks." + rankKey + ".cost", cost);
        plugin.getRankConfig().set("ranks." + rankKey + ".required_papers", requiredPapers);
        plugin.getRankConfig().set("ranks." + rankKey + ".min_island_level", minLevel.toString());
        plugin.getRankConfig().set("ranks." + rankKey + ".previous_rank", previousRankKey); // Set previous rank
        plugin.getRankConfig().save();

        player.sendMessage(ColorUtility.translate(plugin.getRankConfig().getString("messages.success.shop_created")));
        return true;
    }

    private boolean hasEnoughCurrency(Player player, int cost) {
        return economy.getBalance(player) >= cost;
    }

    private boolean hasEnoughRankPapers(Player player, String paperName, List<String> paperLore, int requiredPapers) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(paperName) && meta.hasLore() && Objects.equals(meta.getLore(), paperLore)) {
                    count += item.getAmount();
                    if (count >= requiredPapers) {
                        return true;
                    }
                }
            }
        }
        return count >= requiredPapers;
    }

    private void deductCurrency(Player player, int cost) {
        economy.withdrawPlayer(player, cost);
    }

    private void deductRankPapers(Player player, String paperName, List<String> paperLore, int requiredPapers) {
        int remaining = requiredPapers;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(paperName) && meta.hasLore() && Objects.equals(meta.getLore(), paperLore)) {
                    if (item.getAmount() >= remaining) {
                        item.setAmount(item.getAmount() - remaining);
                        break;
                    } else {
                        remaining -= item.getAmount();
                        item.setAmount(0);
                    }
                }
            }
        }
        player.updateInventory(); // Ensure inventory updates
    }

    private void grantRank(Player player, String rankKey) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            Node rankNode = Node.builder("group." + rankKey).build();
            user.data().add(rankNode);
            luckPerms.getUserManager().saveUser(user);

            // Update the player's purchased ranks
            List<String> purchasedRanks = plugin.getRankConfig().getStringList("players." + player.getUniqueId() + ".purchased_ranks");
            if (!purchasedRanks.contains(rankKey)) {
                purchasedRanks.add(rankKey);
                plugin.getRankConfig().set("players." + player.getUniqueId() + ".purchased_ranks", purchasedRanks);
                plugin.getRankConfig().save();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("buyrank")) {
            return plugin.getRankConfig().getConfigurationSection("ranks").getKeys(false).stream()
                    .filter(rank -> rank.startsWith(args[0]))
                    .collect(Collectors.toList());
        } else if (command.getName().equalsIgnoreCase("giverankpaper")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
