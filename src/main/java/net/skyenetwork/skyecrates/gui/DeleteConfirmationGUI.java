package net.skyenetwork.skyecrates.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DeleteConfirmationGUI implements Listener {
    
    private final SkyeCratesPlugin plugin;
    private final MiniMessage miniMessage;
    private final Map<UUID, DeleteSession> deleteSessions;
    private final Random random;
    
    public DeleteConfirmationGUI(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.deleteSessions = new HashMap<>();
        this.random = new Random();
    }
    
    public void openDeleteConfirmation(Player player, Location crateLocation, String crateName) {
        Inventory gui = Bukkit.createInventory(null, 27, miniMessage.deserialize("<red>Confirm Crate Deletion"));
        
        // Fill with gray glass
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(miniMessage.deserialize("<gray> "));
            grayGlass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, grayGlass);
        }
        
        // Place redstone block randomly
        int redstoneSlot = random.nextInt(27);
        ItemStack redstoneBlock = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta redstoneMeta = redstoneBlock.getItemMeta();
        if (redstoneMeta != null) {
            redstoneMeta.displayName(miniMessage.deserialize("<red>Click to Delete Crate"));
            redstoneMeta.lore(java.util.Arrays.asList(
                miniMessage.deserialize("<yellow>Crate: " + crateName),
                miniMessage.deserialize("<gray>Click 3 times to confirm deletion"),
                miniMessage.deserialize("<red>Clicks remaining: 3")
            ));
            redstoneBlock.setItemMeta(redstoneMeta);
        }
        gui.setItem(redstoneSlot, redstoneBlock);
        
        // Create session
        DeleteSession session = new DeleteSession(crateLocation, crateName, redstoneSlot, 3);
        deleteSessions.put(player.getUniqueId(), session);
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!miniMessage.stripTags(title).equals("Confirm Crate Deletion")) return;
        
        event.setCancelled(true);
        
        DeleteSession session = deleteSessions.get(player.getUniqueId());
        if (session == null) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.REDSTONE_BLOCK) return;
        
        // Reduce clicks remaining
        session.clicksRemaining--;
        
        if (session.clicksRemaining <= 0) {
            // Delete the crate
            plugin.getCrateManager().removeCrate(session.location);
            session.location.getBlock().setType(Material.AIR);
            
            player.sendMessage(plugin.getConfigManager().getMessage("crate-removed"));
            player.closeInventory();
            deleteSessions.remove(player.getUniqueId());
            return;
        }
        
        // Move redstone block to new random position
        Inventory inv = event.getInventory();
        
        // Clear old position
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(miniMessage.deserialize("<gray> "));
            grayGlass.setItemMeta(glassMeta);
        }
        inv.setItem(session.redstoneSlot, grayGlass);
        
        // Place at new position
        int newSlot = random.nextInt(27);
        session.redstoneSlot = newSlot;
        
        ItemStack redstoneBlock = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta redstoneMeta = redstoneBlock.getItemMeta();
        if (redstoneMeta != null) {
            redstoneMeta.displayName(miniMessage.deserialize("<red>Click to Delete Crate"));
            redstoneMeta.lore(java.util.Arrays.asList(
                miniMessage.deserialize("<yellow>Crate: " + session.crateName),
                miniMessage.deserialize("<gray>Click 3 times to confirm deletion"),
                miniMessage.deserialize("<red>Clicks remaining: " + session.clicksRemaining)
            ));
            redstoneBlock.setItemMeta(redstoneMeta);
        }
        inv.setItem(newSlot, redstoneBlock);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (miniMessage.stripTags(title).equals("Confirm Crate Deletion")) {
            deleteSessions.remove(player.getUniqueId());
        }
    }
    
    private static class DeleteSession {
        Location location;
        String crateName;
        int redstoneSlot;
        int clicksRemaining;
        
        DeleteSession(Location location, String crateName, int redstoneSlot, int clicksRemaining) {
            this.location = location;
            this.crateName = crateName;
            this.redstoneSlot = redstoneSlot;
            this.clicksRemaining = clicksRemaining;
        }
    }
}
