package it.loneliness.mc.treasurehunt.Custom;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import it.loneliness.mc.treasurehunt.Plugin;
import it.loneliness.mc.treasurehunt.Controller.Announcement;
import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class TreasureHandler implements Listener {

    private final Plugin plugin;
    @SuppressWarnings("unused")
    private final LogHandler logger;
    private final TreasureManager treasureManager;
    private Announcement announcement;
    private String npcSubstring;

    public TreasureHandler(Plugin plugin, LogHandler logger, TreasureManager treasureManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.treasureManager = treasureManager;
        this.announcement = Announcement.getInstance(plugin);
        this.npcSubstring = this.plugin.getConfig().getString("npc-name", null);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && npcSubstring != null) {
            for (Entity entity : player.getNearbyEntities(7, 7, 7)) {
                if (entity.getType().equals(EntityType.ARMOR_STAND) && entity.getName().contains(npcSubstring)) {
                    event.setCancelled(true);

                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem.getType().equals(Material.MAP)) {
                        List<Location> locations = treasureManager.getChestsLocations();
                        if (locations.size() > 0) {
                            Location closestLocation = locations.stream()
                                    .filter(location -> location.getWorld().equals(player.getLocation().getWorld()))
                                    .min(Comparator.comparingDouble(location -> player.getLocation().distance(location)))
                                    .orElse(null);
                            if (closestLocation != null) {
                                MapView mapView = Bukkit.createMap(closestLocation.getWorld());
                                mapView.setCenterX(closestLocation.getBlockX());
                                mapView.setCenterZ(closestLocation.getBlockZ());
                                mapView.setTrackingPosition(true);
                                mapView.addRenderer(new MapRenderer() {
                                    private boolean cursorAdded = false;
    
                                    @Override
                                    public void render(MapView map, MapCanvas canvas, Player player) {
                                        if (!cursorAdded) {
                                            MapCursorCollection mapCursorCollection = canvas.getCursors();
                                            mapCursorCollection.addCursor(
                                                    new MapCursor((byte) 0, (byte) 0, (byte) 0, MapCursor.Type.RED_X,
                                                            true));
                                            canvas.setCursors(mapCursorCollection);
                                            cursorAdded = true;
                                        }
                                    }
                                });
    
                                // Create the map item
                                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                                MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                                mapMeta.setMapView(mapView);
                                mapItem.setItemMeta(mapMeta);
    
                                // It the player only has one map just replace it in the hand, otherwise reduce
                                // the number of maps and add in inventory the map
                                if (heldItem.getAmount() > 1) {
                                    HashMap<Integer, ItemStack> leftItems = player.getInventory().addItem(mapItem);
                                    if (leftItems.isEmpty()) { // in this case player had space to be give the map
                                        heldItem.setAmount(heldItem.getAmount() - 1);
                                        announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("success-generate-map"));
                                    } else {
                                        announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("error-not-enough-space"));
                                    }
                                } else {
                                    player.getInventory().setItemInMainHand(mapItem);
                                    announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("success-generate-map"));
                                }
                            } else {
                                announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("error-no-treasure-world"));
                            }
                        } else {
                            announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("error-no-treasure"));
                        }
                    } else {
                        announcement.sendPrivateMessage(player, this.plugin.getConfig().getString("error-no-map-in-hand"));
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTreasureOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Block block = event.getInventory().getLocation().getBlock();
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Location chestLocation = chest.getLocation();
            if (treasureManager.isTreasureLocation(chestLocation)) {

                Player player = (Player) event.getPlayer();
                treasureManager.handleTreasureFound(chestLocation, player);

                block.setType(Material.AIR);
                dropChestItems(chest);
            }
        }
    }

    @EventHandler
    public void onTreasureBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Location chestLocation = chest.getLocation();
            if (treasureManager.isTreasureLocation(chestLocation)) {

                Player player = event.getPlayer();
                treasureManager.handleTreasureFound(chestLocation, player);
            }
        }
    }

    @EventHandler
    public void onTreasureExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                Location chestLocation = chest.getLocation();
                if (treasureManager.isTreasureLocation(chestLocation)) {
                    treasureManager.handleTreasureFound(chestLocation, null);
                }
            }
        }
    }

    @EventHandler
    public void onTreasureExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Location chestLocation = chest.getLocation();
            if (treasureManager.isTreasureLocation(chestLocation)) {
                treasureManager.handleTreasureFound(chestLocation, null);
            }
        }
    }

    @EventHandler
    public void onTreasureBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Location chestLocation = chest.getLocation();
            if (treasureManager.isTreasureLocation(chestLocation)) {
                treasureManager.handleTreasureFound(chestLocation, null);
            }
        }
    }
    
    private void dropChestItems(Chest chest) {
        Location loc = chest.getLocation();
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        chest.getInventory().clear();
    }
}
