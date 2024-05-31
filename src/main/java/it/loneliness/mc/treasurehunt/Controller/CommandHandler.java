package it.loneliness.mc.treasurehunt.Controller;

import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Custom.TreasureManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final List<String> allowedPrefixes;
    private final String permissionPrefix;

    private final List<CommandEntry> commandList;
    private TreasureManager treasureManager;
    private final Announcement announcement;

    public CommandHandler(Plugin plugin, List<String> allowedPrefixes, TreasureManager chestManager) {
        this.plugin = plugin;
        this.allowedPrefixes = allowedPrefixes;
        this.permissionPrefix = allowedPrefixes.get(0); // Set the primary permission prefix

        commandList = new ArrayList<CommandEntry>();
        commandList.add(new CommandEntry("help", permissionPrefix, this::getHelp, true));
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
        commandList.add(new CommandEntry("find", permissionPrefix, this::findClosestTreasure, false));
        commandList.add(new CommandEntry("despawnall", permissionPrefix, this::despawnAllTreasures, false));
        commandList.add(new CommandEntry("runperiodic", permissionPrefix, this::runPeriodicTask, true));

        this.treasureManager = chestManager;
        this.announcement = Announcement.getInstance(plugin);
    }

    private boolean runPeriodicTask(CommandParams params){
        treasureManager.periodicRunner();
        announcement.sendPrivateMessage(params.sender, "Running periodic task forcfully");
        return true;
    }

    private boolean despawnAllTreasures(CommandParams params){
        treasureManager.despawnAllTreasures();
        announcement.sendPrivateMessage(params.sender, "despawned all treasures");
        return true;
    }

    private boolean findClosestTreasure(CommandParams params){
        Player player = (Player) params.sender;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType().equals(Material.MAP)) {
            List<Location> locations = treasureManager.getChestsLocations();
            if(locations.size() > 0){
                Location closestLocation = locations.stream().filter(location -> location.getWorld().equals(player.getLocation().getWorld())).min(Comparator.comparingDouble(location -> player.getLocation().distance(location))).orElse(null);
                if(closestLocation != null){
                    MapView mapView = Bukkit.createMap(closestLocation.getWorld());
                    mapView.setCenterX(closestLocation.getBlockX());
                    mapView.setCenterZ(closestLocation.getBlockZ());
                    mapView.setTrackingPosition(true);
                    mapView.addRenderer(new MapRenderer() {
                        private boolean cursorAdded = false;

                        @Override
                        public void render(MapView map, MapCanvas canvas, Player player) {
                            if(!cursorAdded){
                                MapCursorCollection mapCursorCollection = canvas.getCursors();
                                mapCursorCollection.addCursor(new MapCursor((byte) 0, (byte) 0, (byte) 0, MapCursor.Type.RED_X, true));
                                canvas.setCursors(mapCursorCollection);
                                cursorAdded = true;
                            }   
                        }
                    });

                    //Create the map item
                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                    MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                    mapMeta.setMapView(mapView);
                    mapItem.setItemMeta(mapMeta);

                    //It the player only has one map just replace it in the hand, otherwise reduce the number of maps and add in inventory the map
                    if(heldItem.getAmount() > 1){
                        HashMap<Integer, ItemStack> leftItems = player.getInventory().addItem(mapItem);
                        if(leftItems.isEmpty()){ // in this case player had space to be give the map
                            heldItem.setAmount(heldItem.getAmount()-1);
                            announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("success-generate-map"));
                        } else {
                            announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("error-not-enough-space"));
                        }
                    }else{
                        player.getInventory().setItemInMainHand(mapItem);
                        announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("success-generate-map"));
                    }                    
                } else {
                    announcement.sendPrivateMessage(params.sender, this.plugin.getConfig().getString("error-no-treasure-world"));
                }
            } else {
                announcement.sendPrivateMessage(params.sender, this.plugin.getConfig().getString("error-no-treasure"));
            }       
        } else {
            announcement.sendPrivateMessage(params.sender, this.plugin.getConfig().getString("error-no-map-in-hand"));
        }
        return true;
    }

    private boolean getList(CommandParams params) {
        ComponentBuilder builder = new ComponentBuilder(" - location of treasure chests\n");

        for (Location loc : treasureManager.getChestsLocations()) {
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
        treasureManager.spawnTreasureInLocation(player.getLocation());
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
