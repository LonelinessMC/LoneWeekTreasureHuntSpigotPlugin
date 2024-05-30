package it.loneliness.mc.treasurehunt.Controller;

import it.loneliness.mc.treasurehunt.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import it.loneliness.mc.treasurehunt.Model.LogHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class Announcement {
    private static Announcement instance;
    @SuppressWarnings("unused")
    private final Plugin plugin;
    private final LogHandler logger;
    private final String prefix;

    public static Announcement getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new Announcement(plugin);
        }
        return instance;
    }

    private Announcement(Plugin plugin) {
        this.plugin = plugin;
        this.logger = LogHandler.getInstance(null);
        this.prefix = plugin.getConfig().getString("chat-prefix");
    }

    public void sendPrivateMessage(List<Player> players, String message) {
        for (Player player : players)
            sendPrivateMessage(player, message);
    }

    private void sendPrivateMessage(List<Player> players, ComponentBuilder message) {
        players.forEach(p -> sendPrivateMessage(p, message));
    }

    public void sendPrivateMessage(CommandSender cs, String message) {
        if (cs instanceof Player) {
            sendPrivateMessage(((Player) cs), message);
        } else {
            sendConsoleMessage(message);
        }
    }

    public void sendPrivateMessage(CommandSender cs, ComponentBuilder message) {
        if (cs instanceof Player) {
            sendPrivateMessage(((Player) cs), message);
        } else {
            sendConsoleMessage(message);
        }
    }

    public void sendPrivateMessage(Player p, String message) {
        p.sendMessage(applyFormat(this.prefix + message));
    }

    public void sendPrivateMessage(Player p, ComponentBuilder message) {
        // Create a new ComponentBuilder to prepend the prefix
        ComponentBuilder prefixedMessage = new ComponentBuilder()
                .append(this.prefix) // Append the prefix
                .append(message.create()); // Append the original message

        // Send the combined message to the player
        p.spigot().sendMessage(applyFormat(prefixedMessage).create());
    }

    public void sendConsoleMessage(String message) {
        logger.log(applyFormatConsole(message));
    }

    public void sendConsoleMessage(ComponentBuilder message) {
        logger.log(applyFormatConsole(message));
    }

    public void announce(String message) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        sendPrivateMessage(players, message);

        sendConsoleMessage(message);
    }

    public void announce(ComponentBuilder message) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        sendPrivateMessage(players, message);

        sendConsoleMessage(message);
    }

    public String applyFormat(String message) {
        message = message.replace(">>", "»").replace("<<", "«");

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]){6}");
        Matcher matcherHex = hexPattern.matcher(message);
        while (matcherHex.find()) {
            ChatColor hexColor = ChatColor.of(matcherHex.group().substring(1));
            String before = message.substring(0, matcherHex.start());
            String after = message.substring(matcherHex.end());
            message = before + hexColor + after;
            matcherHex = hexPattern.matcher(message);
        }

        message = ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    private ComponentBuilder applyFormat(ComponentBuilder prefixedMessage) {
        ComponentBuilder formatted = new ComponentBuilder();

        for ( BaseComponent x : prefixedMessage.create()) {
            if(x instanceof TextComponent){
                formatted.append(TextComponent.fromLegacy(applyFormat(((TextComponent)x).getText())));
            } else {
                formatted.append(x);
            } 
        }
        return formatted;
    }

    public static String applyFormatConsole(String message) {
        message = message.replace(">>", "»").replace("<<", "«");

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]){6}");
        Matcher matcherHex = hexPattern.matcher(message);
        while (matcherHex.find()) {
            String before = message.substring(0, matcherHex.start());
            String after = message.substring(matcherHex.end());
            message = before + after;
            matcherHex = hexPattern.matcher(message);
        }
        return message;
    }

    private String applyFormatConsole(ComponentBuilder prefixedMessage) {
        StringBuilder formatted = new StringBuilder();

        for (BaseComponent x : prefixedMessage.create()) {
            if (x instanceof TextComponent) {
                formatted.append(applyFormatConsole(((TextComponent) x).getText()));
            } else {
                formatted.append(x.toPlainText());
            }
        }

        return formatted.toString();
    }

}
