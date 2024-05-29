package it.loneliness.mc.treasurehunt.Controller;

import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Custom.ChestManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final List<String> allowedPrefixes;
    private final String permissionPrefix;

    private final List<CommandEntry> commandList;
    private ChestManager chestManager;

    public CommandHandler(Plugin plugin, List<String> allowedPrefixes, ChestManager chestManager) {
        this.plugin = plugin;
        this.allowedPrefixes = allowedPrefixes;
        this.permissionPrefix = allowedPrefixes.get(0); // Set the primary permission prefix

        commandList = new ArrayList<CommandEntry>();
        commandList.add(new CommandEntry("disable", permissionPrefix,
                params -> setEnabledCommand(params.sender, params.cmd, params.label, params.args, false), true));
        commandList.add(new CommandEntry("enable", permissionPrefix,
                params -> setEnabledCommand(params.sender, params.cmd, params.label, params.args, true), true));
        commandList.add(new CommandEntry("status", permissionPrefix, this::getStatusCommand, true));
        commandList.add(new CommandEntry("incrementscore", permissionPrefix,
                params -> incrementScore(params.sender, params.cmd, params.label, params.args, 1), false));
        commandList.add(new CommandEntry("resetscore", permissionPrefix, this::resetScore, true));
        commandList.add(new CommandEntry("test", permissionPrefix, this::getTest, false));
        commandList.add(new CommandEntry("help", permissionPrefix, this::getHelp, true));
        commandList.add(new CommandEntry("list", permissionPrefix, this::getList, true));

        this.chestManager = chestManager;
    }

    private boolean getList(CommandParams params) {
        ComponentBuilder builder = new ComponentBuilder(" - location of treasure chests\n");

        for (Location loc : chestManager.getChestsLocations()) {
            String locString = Math.floor(loc.getX()) + " " + Math.floor(loc.getY()) + " " + Math.floor(loc.getZ());
            TextComponent locComponent = new TextComponent(locString + "\n");
            locComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + loc.getX() + " " + loc.getY() + " " + loc.getZ()));
            builder.append(locComponent);
        }
        Announcement.getInstance(plugin).sendPrivateMessage(params.sender, builder);
        return true;
    }

    private boolean getHelp(CommandParams params){
        String output = " - comandi possibili:\n";
        for (CommandEntry command : commandList) {
            if(command.isAllowed(params.sender)){
                output += "/"+command.permissionPrefix+" "+command.commandName+"\n";
            }
        }
        Announcement.getInstance(plugin).sendPrivateMessage(params.sender, output);
        return true;
    }

    private boolean getTest(CommandParams params){
        Player player = (Player) params.sender;
        chestManager.spawnTreasureInLocation(player.getLocation());
        return true;
    }

    private boolean setEnabledCommand(CommandSender sender, Command cmd, String label, String[] args, boolean enabled) {
        if (enabled) {
            Announcement.getInstance(plugin).sendPrivateMessage(sender, "Scheduler started");
            this.plugin.getTaskScheduler().start();
        } else {
            Announcement.getInstance(plugin).sendPrivateMessage(sender, "Scheduler stopped");
            this.plugin.getTaskScheduler().stop();
        }

        return true;
    }

    private boolean getStatusCommand(CommandParams params) {
        Announcement.getInstance(plugin).sendPrivateMessage(params.sender,
                "Scheduler running: " + this.plugin.getTaskScheduler().isRunning());
        return true;
    }

    private boolean incrementScore(CommandSender sender, Command cmd, String label, String[] args, int score) {
        ScoreboardController scoreboard = ScoreboardController.getInstance(this.plugin);
        scoreboard.incrementScore(sender.getName(), score);
        Announcement.getInstance(plugin).sendPrivateMessage(sender,
                "Punteggio attuale\n" + scoreboard.getSortedPlayersByScore());
        return true;
    }

    private boolean resetScore(CommandParams params) {
        ScoreboardController scoreboard = ScoreboardController.getInstance(this.plugin);
        scoreboard.resetAllPlayersScore();
        Announcement.getInstance(plugin).sendPrivateMessage(params.sender,
                "Punteggio attuale\n" + scoreboard.getSortedPlayersByScore());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }

        // Check if the command starts with any of the allowed prefixes
        boolean validPrefix = false;
        for (String prefix : allowedPrefixes) {
            if (cmd.getName().equalsIgnoreCase(prefix)) {
                validPrefix = true;
                break;
            }
        }

        if (!validPrefix) {
            return false;
        }

        String commandName = args[0].toLowerCase();
        CommandEntry commandEntry = commandList.stream().filter(command -> command.commandName.equals(commandName))
                .findFirst().orElse(null);

        if (commandEntry == null ||
                !commandEntry.isAllowed(sender)) {
            return false;
        }

        return commandEntry.commandFunction.apply(new CommandParams(sender, cmd, label, args));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // Check if the command starts with any of the allowed prefixes
        boolean validPrefix = false;
        for (String prefix : allowedPrefixes) {
            if (cmd.getName().equalsIgnoreCase(prefix)) {
                validPrefix = true;
                break;
            }
        }

        if (!validPrefix) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return commandList.stream()
                    .filter(command -> command.commandName.startsWith(partialCommand))
                    .filter(command -> command.isAllowed(sender))
                    .map(command -> command.commandName)
                    .toList();
        }

        return new ArrayList<>();
    }

    private static class CommandParams {
        CommandSender sender;
        Command cmd;
        String label;
        String[] args;

        CommandParams(CommandSender sender, Command cmd, String label, String[] args) {
            this.sender = sender;
            this.cmd = cmd;
            this.label = label;
            this.args = args;
        }
    }

    private static class CommandEntry {
        String commandName;
        String permissionPrefix;
        Function<CommandParams, Boolean> commandFunction;
        boolean consoleAllowed;

        CommandEntry(String commandName, String permissionPrefix, Function<CommandParams, Boolean> commandFunction,
                boolean consoleAllowed) {
            this.commandName = commandName;
            this.permissionPrefix = permissionPrefix;
            this.commandFunction = commandFunction;
            this.consoleAllowed = consoleAllowed;
        }

        boolean isAllowed(CommandSender sender) {
            if (!(sender instanceof Player) && this.consoleAllowed) {
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                String permission = permissionPrefix + "." + this.commandName;

                if (player.hasPermission(permission)) {
                    return true;
                }
            }

            return false;
        }
    }
}
