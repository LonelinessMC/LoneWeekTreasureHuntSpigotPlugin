package it.loneliness.mc.treasurehunt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import it.loneliness.mc.treasurehunt.Controller.CommandHandler;
import it.loneliness.mc.treasurehunt.Controller.TaskScheduler;
import it.loneliness.mc.treasurehunt.Custom.TreasureHandler;
import it.loneliness.mc.treasurehunt.Custom.TreasureManager;
import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class Plugin extends JavaPlugin{
    LogHandler logger;
    CommandHandler commandHandler;
    TaskScheduler taskScheduler;
    private TreasureManager manager;
    private TreasureHandler treasureHandler;
    
    @Override
    public void onEnable() {
        logger = LogHandler.getInstance(getLogger());
        logger.info("Enabling the plugin");

        if (!checkAndLoadConfig()) {
            logger.severe("Configuration is invalid. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if(getConfig().getBoolean("debug")){
            logger.setDebug(true);
        }
        
        manager = new TreasureManager(this, logger);
        manager.onEnable();

        treasureHandler = new TreasureHandler(this, logger, manager);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(treasureHandler, this);

        //Make sure this is alligned with the plugin.yml, the first in the list is used for the permissions
        List<String> prefixes = new ArrayList<>(Arrays.asList("treasurehunt", "th"));
        this.commandHandler = new CommandHandler(this, prefixes, manager);
        for(String prefix : prefixes){
            this.getCommand(prefix).setExecutor(commandHandler);
            this.getCommand(prefix).setTabCompleter(commandHandler);
        }

        if (!lateValidateConfig()) {
            logger.severe("Configuration is invalid. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.taskScheduler = new TaskScheduler(this, 60, manager); //in seconds
        taskScheduler.start();
    }

    @Override
    public void onDisable() {
        logger.info("Disabling the plugin");
        manager.onDisable();
        taskScheduler.stop();
    }

    /**
     * Checks and loads the configuration file.
     * @return true if the configuration is valid, false otherwise.
     */
    private boolean checkAndLoadConfig() {
        // Save the default config if it does not exist
        saveDefaultConfig();

        String scoreboardId = getConfig().getString("scoreboard-id");
        if (scoreboardId.isBlank() || scoreboardId.isEmpty()) {
            logger.severe("No scoreboard id specified in the config!");
            return false;
        }

        int a = getConfig().getInt("min-distance-from-center");
        if (a < 0) {
            logger.severe("min-distance-from-center invalid!");
            return false;
        }
        int b = getConfig().getInt("max-distance-from-center");
        if (b < 0) {
            logger.severe("max-distance-from-center invalid!");
            return false;
        }


        int c = getConfig().getInt("points-to-opener");
        if (c < 0) {
            logger.severe("points-to-opener invalid!");
            return false;
        }
        int d = getConfig().getInt("points-to-close");
        if (d < 0) {
            logger.severe("points-to-close invalid!");
            return false;
        }
        int e = getConfig().getInt("close-range");
        if (e < 0) {
            logger.severe("close-range invalid!");
            return false;
        }

        return true;
    }

    private boolean lateValidateConfig(){
        if(!manager.validateAllPrizes()){
            logger.severe("prizes in prizes-in-treasure are invalid!");
            return false;
        }

        return true;
    }

    public TaskScheduler getTaskScheduler(){
        return this.taskScheduler;
    }
}