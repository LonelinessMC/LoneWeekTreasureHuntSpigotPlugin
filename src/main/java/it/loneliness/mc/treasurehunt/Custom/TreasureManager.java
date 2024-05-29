package it.loneliness.mc.treasurehunt.Custom;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Controller.Announcement;
import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class TreasureManager {
    private static String STORAGE_FILE = "chestLocations.yml";
    private final List<Location> chestLocations = new CopyOnWriteArrayList<>();
    private Plugin plugin;
    private YamlConfiguration chestLocationsConfig;
    private File chestLocationsFile;
    private final LogHandler logger;
    private TreasureArea area;
    private Lock lock;

    public TreasureManager(Plugin plugin, LogHandler logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.area = new TreasureArea(plugin, logger);
        this.lock = new ReentrantLock();
    }

    public void spawnTreasureRandomly() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(lock.tryLock()){
                    try{
                        Location loc = area.getRandomLocation(area.getRandomWorld());
                        spawnTreasureInLocation(loc);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.info("spawnTreasureRandomly is already being executed");
                }
            }
        }.runTaskAsynchronously(this.plugin);
    }

    public void spawnTreasureInLocation(Location location) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Chunk chunk = location.getChunk();
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                Block block = location.getBlock();
                block.setType(Material.CHEST);
                BlockState state = block.getState();

                if (state instanceof Chest) {
                    // Chest chest = (Chest) state;
                    // populateChest(chest.getInventory());
                    chestLocations.add(location);
                    Announcement.getInstance(plugin).announce("un nuovo tesoro è stato individuato, una /th find");
                }
            }
        }.runTask(this.plugin);
    }

    // This should not be a task because on disable we cannot set tasks
    public void despawnAllTreasures() {
        for (Location location : chestLocations) {
            Chunk chunk = location.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load();
            }

            Block block = location.getBlock();
            block.setType(Material.AIR);
            chestLocations.remove(location);
        }
        chestLocations.clear();
    }

    public void onEnable() {
        chestLocationsFile = new File(this.plugin.getDataFolder(), STORAGE_FILE);
        if (!chestLocationsFile.exists()) {
            this.plugin.saveResource(STORAGE_FILE, false);
        }
        this.chestLocationsConfig = YamlConfiguration.loadConfiguration(chestLocationsFile);

        if (chestLocationsConfig.isSet("chests")) {
            chestLocationsConfig.getList("chests").forEach(location -> {
                if (location instanceof Location) {
                    spawnTreasureInLocation((Location) location);
                }
            });
        }
    }

    public void onDisable() {
        chestLocationsConfig.set("chests", chestLocations);
        try {
            chestLocationsConfig.save(chestLocationsFile);
            despawnAllTreasures();
        } catch (IOException e) {
            logger.severe(e.getStackTrace().toString());
        }
    }

    public boolean isTreasureLocation(Location chestLocation) {
        for (Location loc : chestLocations) {
            if (loc.getBlock().equals(chestLocation.getBlock())) {
                return true;
            }
        }
        return false;
    }

    public void removeTreasureLocation(Location chestLocation) {
        for (Location loc : chestLocations) {
            if (loc.getBlock().equals(chestLocation.getBlock())) {
                chestLocations.remove(loc);
            }
        }
    }

    public List<Location> getChestsLocations() {
        return this.chestLocations;
    }

    public void periodicRunner() {
        if (this.getChestsLocations().size() == 0) {
            this.spawnTreasureRandomly();
        }
    }
}
