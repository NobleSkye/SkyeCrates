package net.skyenetwork.skyecrates.listeners

import net.kyori.adventure.text.minimessage.MiniMessage
import net.skyenetwork.skyecrates.SkyeCratesPlugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class CrateListener(private val plugin: SkyeCratesPlugin) : Listener {
    
    private val miniMessage = MiniMessage.miniMessage()
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item
        val clickedBlock = event.clickedBlock
        
        // Handle crate item usage
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.type == Material.CHEST) {
                val meta = item.itemMeta
                if (meta != null) {
                    val nbtKey = NamespacedKey(plugin, "crate_type")
                    val crateType = meta.persistentDataContainer.get(nbtKey, PersistentDataType.STRING)
                    
                    if (crateType != null) {
                        val crate = plugin.crateManager.getCrate(crateType)
                        if (crate != null) {
                            // Generate loot directly for the player
                            val loot = crate.lootTable.populateLoot(java.util.Random(), null)
                            
                            // Give items to player
                            loot.forEach { lootItem ->
                                val remaining = player.inventory.addItem(lootItem)
                                // Drop items that don't fit
                                remaining.values.forEach { dropItem ->
                                    player.world.dropItemNaturally(player.location, dropItem)
                                }
                            }
                            
                            // Remove one crate item from inventory
                            item.amount = item.amount - 1
                            
                            // Send message
                            player.sendMessage(plugin.configManager.getMessage("crate-opened", "crate" to crate.name))
                            
                            // Cancel event to prevent placing the chest
                            event.isCancelled = true
                        }
                    }
                }
            }
        }
        
        // Handle placed crate interaction
        if (event.action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.type == Material.CHEST) {
                val crateName = plugin.crateManager.getCrateAt(clickedBlock.location)
                if (crateName != null) {
                    if (player.hasPermission("skyecrates.use")) {
                        plugin.crateManager.openCrate(clickedBlock.location, player)
                    } else {
                        player.sendMessage(plugin.configManager.getMessage("no-permission"))
                    }
                    event.isCancelled = true
                }
            }
        }
    }
}
