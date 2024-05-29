package it.loneliness.mc.treasurehunt.Controller;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TaskScheduler {
    private final JavaPlugin plugin;
    private final long periodSeconds;
    private int taskId = -1;

    public TaskScheduler(JavaPlugin plugin, long periodSeconds) {
        this.plugin = plugin;
        this.periodSeconds = periodSeconds;
    }

    public void start() {
        if (taskId == -1) {
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::periodicRunner, 0, periodSeconds*20);
        }
    }

    public void periodicRunner(){
        new BukkitRunnable() {
            @Override
            public void run() {
                //
            }
        }.runTaskAsynchronously(plugin);
        
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public boolean isRunning() {
        return taskId != -1;
    }
}
