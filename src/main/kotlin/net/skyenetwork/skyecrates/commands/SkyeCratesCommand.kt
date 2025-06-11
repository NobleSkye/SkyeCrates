package net.skyenetwork.skyecrates.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.skyenetwork.skyecrates.SkyeCratesPlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SkyeCratesCommand(private val plugin: SkyeCratesPlugin) : CommandExecutor, TabCompleter {
    
    private val miniMessage = MiniMessage.miniMessage()
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("skyecrates.admin")) {
            sender.sendMessage(plugin.configManager.getMessage("no-permission"))
            return true
        }
        
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "list" -> handleList(sender)
            "give" -> handleGive(sender, args)
            "place" -> handlePlace(sender, args)
            "remove" -> handleRemove(sender)
            else -> sendHelp(sender)
        }
        
        return true
    }
    
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(miniMessage.deserialize("<gold>=== SkyeCrates Commands ==="))
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates reload <gray>- Reload plugin configuration"))
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates list <gray>- List all available crates"))
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates give <player> <crate> [amount] <gray>- Give a crate item to a player"))
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates place <crate> <gray>- Place a crate at your target location"))
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates remove <gray>- Remove a crate you're looking at"))
    }
    
    private fun handleReload(sender: CommandSender) {
        plugin.reload()
        sender.sendMessage(plugin.configManager.getMessage("config-reloaded"))
    }
    
    private fun handleList(sender: CommandSender) {
        val crates = plugin.crateManager.getAllCrates()
        if (crates.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize("<red>No crates found!"))
            return
        }
        
        sender.sendMessage(miniMessage.deserialize("<gold>=== Available Crates ==="))
        crates.forEach { (key, crate) ->
            sender.sendMessage(miniMessage.deserialize("<yellow>$key <gray>- <white>${crate.name}"))
        }
    }
    
    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /skyecrates give <player> <crate> [amount]"))
            return
        }
        
        val targetName = args[1]
        val crateName = args[2]
        val amount = if (args.size >= 4) args[3].toIntOrNull() ?: 1 else 1
        
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(plugin.configManager.getMessage("player-not-found"))
            return
        }
        
        val crate = plugin.crateManager.getCrate(crateName)
        if (crate == null) {
            sender.sendMessage(plugin.configManager.getMessage("crate-not-found"))
            return
        }
        
        // Create crate item (chest with custom display name)
        val crateItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST, amount)
        val meta = crateItem.itemMeta
        meta?.displayName(miniMessage.deserialize("<gold>${crate.name}"))
        meta?.lore(listOf(miniMessage.deserialize("<gray>Right-click to open this crate!")))
        
        // Add NBT data to identify the crate type
        val nbtKey = org.bukkit.NamespacedKey(plugin, "crate_type")
        meta?.persistentDataContainer?.set(nbtKey, org.bukkit.persistence.PersistentDataType.STRING, crateName)
        
        crateItem.itemMeta = meta
        
        // Give item to player
        target.inventory.addItem(crateItem)
        
        sender.sendMessage(plugin.configManager.getMessage("crate-given", 
            "amount" to amount.toString(),
            "crate" to crate.name,
            "player" to target.name
        ))
    }
    
    private fun handlePlace(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by players!"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /skyecrates place <crate>"))
            return
        }
        
        val crateName = args[1]
        val crate = plugin.crateManager.getCrate(crateName)
        if (crate == null) {
            sender.sendMessage(plugin.configManager.getMessage("crate-not-found"))
            return
        }
        
        val targetBlock = sender.getTargetBlockExact(10)
        if (targetBlock == null) {
            sender.sendMessage(plugin.configManager.getMessage("no-target-block"))
            return
        }
        
        plugin.crateManager.placeCrate(targetBlock.location, crateName)
        sender.sendMessage(plugin.configManager.getMessage("crate-placed", "crate" to crate.name))
    }
    
    private fun handleRemove(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by players!"))
            return
        }
        
        val targetBlock = sender.getTargetBlockExact(10)
        if (targetBlock == null) {
            sender.sendMessage(plugin.configManager.getMessage("no-crate-to-remove"))
            return
        }
        
        if (plugin.crateManager.removeCrate(targetBlock.location)) {
            targetBlock.type = org.bukkit.Material.AIR
            sender.sendMessage(plugin.configManager.getMessage("crate-removed"))
        } else {
            sender.sendMessage(plugin.configManager.getMessage("no-crate-to-remove"))
        }
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("skyecrates.admin")) {
            return emptyList()
        }
        
        return when (args.size) {
            1 -> listOf("reload", "list", "give", "place", "remove").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "give" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
                "place" -> plugin.crateManager.getAllCrates().keys.filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "give" -> plugin.crateManager.getAllCrates().keys.filter { it.startsWith(args[2], true) }
                else -> emptyList()
            }
            4 -> when (args[0].lowercase()) {
                "give" -> listOf("1", "5", "10")
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
