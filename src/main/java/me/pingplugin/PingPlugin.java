package me.pingplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingPlugin extends JavaPlugin implements TabExecutor {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("ping") != null) {
            getCommand("ping").setExecutor(this);
            getCommand("ping").setTabCompleter(this);
        }
        if (getCommand("pingreload") != null) {
            getCommand("pingreload").setExecutor(this);
        }
    }

    /** Reads a message from config, fills in placeholders and parses it. */
    private Component format(String key, String player, Integer ping) {
        String raw = getConfig().getString("messages." + key, "<red>Missing message: " + key);
        String accent = getConfig().getString("accent-color", "#00a2ff");

        raw = raw.replace("{accent}", accent);
        if (player != null) raw = raw.replace("{player}", player);
        if (ping != null) raw = raw.replace("{ping}", String.valueOf(ping));

        try {
            return mm.deserialize(raw);
        } catch (Exception e) {
            getLogger().warning("Invalid format for messages." + key + ": " + e.getMessage());
            return Component.text(raw);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /pingreload
        if (command.getName().equalsIgnoreCase("pingreload")) {
            if (!sender.hasPermission("pingplugin.reload")) {
                sender.sendMessage(format("no-permission", null, null));
                return true;
            }
            reloadConfig();
            sender.sendMessage(format("reloaded", null, null));
            return true;
        }

        // /ping -> your own ping
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(format("console-usage", null, null));
                return true;
            }
            sender.sendMessage(format("self", player.getName(), player.getPing()));
            return true;
        }

        // /ping <player> -> someone else's ping
        if (!sender.hasPermission("pingplugin.ping.others")) {
            sender.sendMessage(format("no-permission", null, null));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            // Offline: check whether they've ever joined (cache lookup only, no web request)
            OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[0]);
            if (offline != null && offline.hasPlayedBefore()) {
                String name = offline.getName() != null ? offline.getName() : args[0];
                sender.sendMessage(format("player-offline", name, null));
            } else {
                sender.sendMessage(format("user-not-found", args[0], null));
            }
            return true;
        }

        sender.sendMessage(format("other", target.getName(), target.getPing()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ping")
                && args.length == 1
                && sender.hasPermission("pingplugin.ping.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
