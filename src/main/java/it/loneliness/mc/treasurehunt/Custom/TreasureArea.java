package it.loneliness.mc.treasurehunt.Custom;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Model.LogHandler;


public class TreasureArea {
    private final int maxDistanceFromCenter;
    private final int minDistanceFromCenter;
    private LogHandler logger;
    private Random random;

    public TreasureArea(Plugin plugin, LogHandler logger) {
        this.logger = logger;
        this.maxDistanceFromCenter = plugin.getConfig().getInt("max-distance-from-center");
        this.minDistanceFromCenter = plugin.getConfig().getInt("min-distance-from-center");
        this.random = new Random();
    }
    
    public World getRandomWorld() {
        return Bukkit.getWorld("world");
    }

    public Location getRandomLocation(World world){
        int attempts = 0;
        while(true){
            Location center = world.getWorldBorder().getCenter();

            // Generate a random angle in radians
            double angle = Math.random() * 2 * Math.PI;
    
            // Generate a random distance within the specified range
            double distance = minDistanceFromCenter + (Math.random() * (maxDistanceFromCenter - minDistanceFromCenter));
    
            // Calculate the coordinates of the random position
            double xOffset = distance * Math.cos(angle);
            double zOffset = distance * Math.sin(angle);
            int yIndex = random.nextInt(40 - (-50) + 1) + (-50);
    
            // Create a new Location with the offsets
            Location randomPosition = center.clone().add(xOffset, yIndex, zOffset);

            if(locationIsInACave(randomPosition)){
                logger.info("Required "+attempts+" attempts in getRandomLocation");
                return randomPosition;
            }

            attempts++;
            if(attempts % 100 == 0){
                logger.info("Currently at attempt "+attempts+" in getRandomLocation");
            }

            if(attempts % 2 == 0){
                try {
                    // sleep attempts to prevent server from lagging while searching for a valid location
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Handle the exception if the sleep is interrupted
                    logger.severe("Exception while getRandomLocation" + e.getMessage());
                }
            }
        }
    }

    public boolean locationIsInACave(Location loc){
        Block b = loc.getBlock();
        return b.getType().isAir() && b.getRelative(0, 1, 0).getType().isAir() && b.getRelative(0, -1, 0).getType().isSolid();
    }
}
