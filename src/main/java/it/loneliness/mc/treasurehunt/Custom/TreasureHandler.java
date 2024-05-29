package it.loneliness.mc.treasurehunt.Custom;

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import it.loneliness.mc.treasurehunt.Model.LogHandler;

public class TreasureHandler implements Listener {

    private final JavaPlugin plugin;
    @SuppressWarnings("unused")
    private final LogHandler logger;
    private final TreasureManager chestManager;

    public TreasureHandler(JavaPlugin plugin, LogHandler logger, TreasureManager chestManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.chestManager = chestManager;
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Block block = event.getInventory().getLocation().getBlock();
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Location chestLocation = chest.getLocation();
            if (chestManager.isTreasureLocation(chestLocation)) {
                Player player = (Player) event.getPlayer();
                triggerEffect(chestLocation);
                chestManager.removeTreasureLocation(chestLocation);
                block.setType(Material.AIR);
                dropChestItems(chest);
                player.sendMessage("You have discovered a hidden chest!");
            }
        }
    }

    private void triggerEffect(Location loc) {
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
