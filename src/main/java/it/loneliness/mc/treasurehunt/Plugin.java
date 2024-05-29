package it.loneliness.mc.treasurehunt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import it.loneliness.mc.treasurehunt.Controller.CommandHandler;
import it.loneliness.mc.treasurehunt.Controller.TaskScheduler;
import it.loneliness.mc.treasurehunt.Custom.ChestHandler;
import it.loneliness.mc.treasurehunt.Custom.ChestManager;
import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class Plugin extends JavaPlugin{
    LogHandler logger;
    CommandHandler commandHandler;
    TaskScheduler taskScheduler;
    private ChestManager chestManager;
    private ChestHandler chestHandler;
    
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
        
        chestManager = new ChestManager(this, logger);
        chestManager.onEnable();

        chestHandler = new ChestHandler(this, logger, chestManager);
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(chestHandler, this);

        //Make sure this is alligned with the plugin.yml, the first in the list is used for the permissions
        List<String> prefixes = new ArrayList<>(Arrays.asList("treasurehunt", "th"));
        this.commandHandler = new CommandHandler(this, prefixes, chestManager);
        for(String prefix : prefixes){
            this.getCommand(prefix).setExecutor(commandHandler);
            this.getCommand(prefix).setTabCompleter(commandHandler);
        }


        //this.taskScheduler = new TaskScheduler(this, 60, actionsManager); //in seconds
        //taskScheduler.start();
    }

    @Override
    public void onDisable() {
        logger.info("Disabling the plugin");
        chestManager.onDisable();
        //taskScheduler.stop();
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

        return true;
    }

    public TaskScheduler getTaskScheduler(){
        return this.taskScheduler;
    }
}