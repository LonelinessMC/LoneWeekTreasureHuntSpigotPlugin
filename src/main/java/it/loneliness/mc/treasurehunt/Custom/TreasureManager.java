package it.loneliness.mc.treasurehunt.Custom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private Announcement announce;

    public TreasureManager(Plugin plugin, LogHandler logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.area = new TreasureArea(plugin, logger);
        this.lock = new ReentrantLock();
        this.announce = Announcement.getInstance(plugin);
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

    public boolean validateAllPrizes(){
        try{
            List<Map<?, ?>> treasureSets = this.plugin.getConfig().getMapList("prizes-in-treasure");    
            for (Map<?, ?> selectedItemSet : treasureSets) {
                getPrize(selectedItemSet);
            }
        } catch (Exception e) {
            logger.severe("Some prizes specified in the config are not valid");
            logger.severe(e.getStackTrace().toString());
            return false;
        }

        return true;        
    }

    private List<ItemStack> getPrize(Map<?, ?> selectedItemSet){
        List<ItemStack> output = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) selectedItemSet.get("treasure");
        for (Map<?, ?> itemMap : items) {
            Material material = Material.getMaterial((String) itemMap.get("material"));
            int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (itemMap.containsKey("name")) {
                meta.setDisplayName(announce.applyFormat((String) itemMap.get("name")));
            }

            if (itemMap.containsKey("lore")) {
                @SuppressWarnings("unchecked")
                List<String> lore = ((List<String>) itemMap.get("lore")).stream().map(s -> announce.applyFormat(s)).toList();
                meta.setLore(lore);
            }

            if (itemMap.containsKey("enchantments")) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> enchantments = (Map<String, Integer>) itemMap.get("enchantments");
                for (Map.Entry<String, Integer> enchantment : enchantments.entrySet()) {
                    @SuppressWarnings("deprecation")
                    Enchantment ench = Enchantment.getByName(enchantment.getKey());
                    meta.addEnchant(ench, enchantment.getValue(), true);
                }
            }

            item.setItemMeta(meta);
            output.add(item);
        }
        return output;
    }

    private List<ItemStack> getRandomPrizes() {
        List<Map<?, ?>> treasureSets = this.plugin.getConfig().getMapList("prizes-in-treasure");
        Random random = new Random();

        Map<?, ?> selectedItemSet = treasureSets.get(random.nextInt(treasureSets.size()));
        
        return getPrize(selectedItemSet);

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

                Chest chest = (Chest) block.getState();
                List<ItemStack> prizes = getRandomPrizes();
                prizes.forEach(prize -> chest.getBlockInventory().addItem(prize));

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
