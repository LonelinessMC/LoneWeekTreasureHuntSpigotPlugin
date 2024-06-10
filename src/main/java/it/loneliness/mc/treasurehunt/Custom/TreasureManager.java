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
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Controller.Announcement;
import it.loneliness.mc.treasurehunt.Controller.ScoreboardController;
import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class TreasureManager {
    private static String STORAGE_FILE = "treasureLocations.yml";
    private final List<Location> treasureLocations = new CopyOnWriteArrayList<>();
    private Plugin plugin;
    private YamlConfiguration treasureLocationsConfig;
    private File treasureLocationsFile;
    private final LogHandler logger;
    private TreasureArea area;
    private Lock lock;
    private Announcement announce;
    private int pointsToOpener;
    private int pointsToClose;
    private int pointsCloseRadius;
    private FileConfiguration config;
    private ScoreboardController scoreboardController;

    public TreasureManager(Plugin plugin, LogHandler logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.area = new TreasureArea(plugin, logger);
        this.lock = new ReentrantLock();
        this.announce = Announcement.getInstance(plugin);

        config = this.plugin.getConfig();
        pointsToOpener = config.getInt("points-to-opener");
        pointsToClose = config.getInt("points-to-close");
        pointsCloseRadius = config.getInt("close-range");

        scoreboardController = ScoreboardController.getInstance(plugin);
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
            logger.severe(e);
            return false;
        }

        return true;        
    }

    private List<ItemStack> getPrize(Map<?, ?> selectedItemSet){
        List<ItemStack> output = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) selectedItemSet.get("treasure");
        for (Map<?, ?> itemMap : items) {
            logger.debug((String) itemMap.get("material"));
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
                    treasureLocations.add(location);
                    Announcement.getInstance(plugin).announce("un nuovo tesoro è stato individuato, vai dal cartografo");
                }
            }
        }.runTask(this.plugin);
    }

    // This should not be a task because on disable we cannot set tasks
    public void despawnAllTreasures() {
        for (Location location : treasureLocations) {
            Chunk chunk = location.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load();
            }

            Block block = location.getBlock();
            block.setType(Material.AIR);
            treasureLocations.remove(location);
        }
        treasureLocations.clear();
    }

    public void onEnable() {
        treasureLocationsFile = new File(this.plugin.getDataFolder(), STORAGE_FILE);
        if (!treasureLocationsFile.exists()) {
            this.plugin.saveResource(STORAGE_FILE, false);
        }
        this.treasureLocationsConfig = YamlConfiguration.loadConfiguration(treasureLocationsFile);

        if (treasureLocationsConfig.isSet("chests")) {
            treasureLocationsConfig.getList("chests").forEach(location -> {
                if (location instanceof Location) {
                    spawnTreasureInLocation((Location) location);
                }
            });
        }
    }

    public void onDisable() {
        treasureLocationsConfig.set("chests", treasureLocations);
        try {
            treasureLocationsConfig.save(treasureLocationsFile);
            despawnAllTreasures();
        } catch (IOException e) {
            logger.severe(e.getStackTrace().toString());
        }
    }

    public boolean isTreasureLocation(Location chestLocation) {
        for (Location loc : treasureLocations) {
            if (loc.getBlock().equals(chestLocation.getBlock())) {
                return true;
            }
        }
        return false;
    }

    public void removeTreasureLocation(Location chestLocation) {
        for (Location loc : treasureLocations) {
            if (loc.getBlock().equals(chestLocation.getBlock())) {
                treasureLocations.remove(loc);
            }
        }
    }

    public List<Location> getChestsLocations() {
        return this.treasureLocations;
    }

    public void periodicRunner() {
        if (this.getChestsLocations().size() == 0) {
            this.spawnTreasureRandomly();
        }
    }

    public void handleTreasureFound(Location openTreasureLocation, Player winner) {
        this.removeTreasureLocation(openTreasureLocation);
        this.triggerWinEffect(openTreasureLocation);

        if(winner != null){
            this.handleTrasureWinner(winner);
        }

        for(Player player : openTreasureLocation
            .getWorld()
            .getNearbyEntities(openTreasureLocation, pointsCloseRadius, pointsCloseRadius, pointsCloseRadius)
            .stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .sorted((player1, player2) -> Double.compare(
                player1.getLocation().distance(openTreasureLocation),
                player2.getLocation().distance(openTreasureLocation)))
            .collect(Collectors.toList())
        ){
            if(winner == null){
                this.handleTrasureWinner(player);
                winner = player;
            } else if(!player.equals(winner)) {
                this.handleTreasureClose(player);
            }
        }
    }

    private void handleTrasureWinner(Player player) {
        scoreboardController.incrementScore(player.getName(), pointsToOpener);
        announce.announce(this.config.getString("win-broadcast-message").replace("{player}", player.getName()).replace("{points}", pointsToOpener+""));
    }

    private void handleTreasureClose(Player player) {
        scoreboardController.incrementScore(player.getName(), pointsToClose);
        announce.sendPrivateMessage(player, this.config.getString("close-win-private-message").replace("{player}", player.getName()).replace("{points}", pointsToClose+""));
    }

    private void triggerWinEffect(Location loc) {
        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Firework firework = loc.getWorld().spawn(loc, Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();
                    FireworkEffect effect = FireworkEffect.builder()
                            .withColor(org.bukkit.Color.RED, org.bukkit.Color.GREEN, org.bukkit.Color.BLUE)
                            .with(FireworkEffect.Type.BALL)
                            .withFlicker()
                            .build();
                    meta.addEffect(effect);
                    meta.setPower(1);
                    firework.setFireworkMeta(meta);
                }
            }.runTaskLater(plugin, i * 10L); // Delay each firework a little for a better effect
        }
    }
}
