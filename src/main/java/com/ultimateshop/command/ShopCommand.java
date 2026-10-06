package com.ultimateshop.command;

import com.ultimateshop.UltimateShop;
import com.ultimateshop.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final UltimateShop plugin;

    public ShopCommand(UltimateShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ShopManager sm = plugin.getShopManager();

        // /shop [categoria]
        if (command.getName().equalsIgnoreCase("shop")) {
            if (!(sender instanceof Player player)) {
                sm.msg(sender, "player-only", null);
                return true;
            }
            if (args.length == 0) {
                sm.openMain(player);
                return true;
            }
            if (!sm.hasCategory(args[0])) {
                sm.msg(player, "unknown-category", Map.of("category", args[0]));
                return true;
            }
            sm.openCategory(player, args[0], 1);
            return true;
        }

        // /ultimateshop | /ushop | /us  (solo OP)
        if (!sender.hasPermission("ultimateshop.admin")) {
            sm.msg(sender, "no-permission", null);
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sm.sendList(sender, "help");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAll();
                sm.msg(sender, "reloaded", null);
            }
            case "open" -> {
                if (args.length < 2) {
                    sm.sendList(sender, "help");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sm.msg(sender, "player-not-found", null);
                    return true;
                }
                if (args.length >= 3) {
                    if (!sm.hasCategory(args[2])) {
                        sm.msg(sender, "unknown-category", Map.of("category", args[2]));
                        return true;
                    }
                    sm.openCategory(target, args[2], 1);
                } else {
                    sm.openMain(target);
                }
            }
            default -> sm.sendList(sender, "help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ShopManager sm = plugin.getShopManager();
        List<String> out = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("shop")) {
            if (args.length == 1) {
                out.addAll(sm.getCategoryIds());
            }
        } else if (sender.hasPermission("ultimateshop.admin")) {
            if (args.length == 1) {
                out.addAll(List.of("help", "reload", "open"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
                out.addAll(sm.getCategoryIds());
            }
        }

        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }
                    }
