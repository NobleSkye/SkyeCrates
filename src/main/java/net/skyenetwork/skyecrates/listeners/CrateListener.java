package net.skyenetwork.skyecrates.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import net.skyenetwork.skyecrates.crates.CrateManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

public class CrateListener implements Listener {
    
    private final SkyeCratesPlugin plugin;
    private final MiniMessage miniMessage;
    
    public CrateListener(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block clickedBlock = event.getClickedBlock();
        
        // Handle crate placer item usage
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.PAPER) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                NamespacedKey nbtKey = new NamespacedKey(plugin, "crate_type");
                String crateType = meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
                
                if (crateType != null && clickedBlock != null) {
                    // Place the crate on top of the clicked block
                    Location placeLocation = clickedBlock.getLocation().add(0, 1, 0);
                    if (placeLocation.getBlock().getType() == Material.AIR) {
                        plugin.getCrateManager().placeCrate(placeLocation, crateType);
                        
                        // Remove one placer item from inventory
                        item.setAmount(item.getAmount() - 1);
                        
                        // Send message
                        CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateType);
                        if (crate != null) {
                            player.sendMessage(plugin.getConfigManager().getMessage("crate-placed", "crate", crate.getName()));
                        }
                        
                        // Cancel event to prevent other interactions
                        event.setCancelled(true);
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("crate-place-blocked"));
                        event.setCancelled(true);
                    }
                }
            }
        }
        
        // Handle placed crate interaction (barriers)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.getType() == Material.BARRIER) {
                String crateName = plugin.getCrateManager().getCrateAt(clickedBlock.getLocation());
                if (crateName != null) {
                    if (player.hasPermission("skyecrates.use")) {
                        plugin.getCrateManager().openCrate(clickedBlock.getLocation(), player);
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    }
                    event.setCancelled(true);
                }
            }
        }
    }
}
