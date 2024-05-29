package it.loneliness.mc.treasurehunt.Custom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import it.loneliness.mc.treasurehunt.Plugin;


public class TreasureArea {
    private final int maxDistanceFromCenter;
    private final int minDistanceFromCenter;

    public TreasureArea(Plugin plugin) {
        this.maxDistanceFromCenter = plugin.getConfig().getInt("max-distance-from-center");
        this.minDistanceFromCenter = plugin.getConfig().getInt("min-distance-from-center");
    }
    
    public World getRandomWorld() {
        return Bukkit.getWorld("world");
    }

    public Location getRandomLocation(World world){
        Location center = world.getWorldBorder().getCenter();

        // Generate a random angle in radians
        double angle = Math.random() * 2 * Math.PI;

        // Generate a random distance within the specified range
        double distance = minDistanceFromCenter + (Math.random() * (maxDistanceFromCenter - minDistanceFromCenter));

        // Calculate the coordinates of the random position
        double xOffset = distance * Math.cos(angle);
        double zOffset = distance * Math.sin(angle);

        // Create a new Location with the offsets
        Location randomPosition = center.clone().add(xOffset, 0, zOffset);

        return randomPosition;
    }
}
