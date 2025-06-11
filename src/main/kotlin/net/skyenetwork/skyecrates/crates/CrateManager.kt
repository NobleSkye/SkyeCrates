package net.skyenetwork.skyecrates.crates

import com.google.gson.JsonParser
import net.skyenetwork.skyecrates.SkyeCratesPlugin
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTable
import java.io.File
import java.io.StringReader

data class CrateConfig(
    val name: String,
    val particles: ParticleConfig,
    val lootTable: LootTable
)

data class ParticleConfig(
    val type: Particle,
    val deltaX: Double,
    val deltaY: Double,
    val deltaZ: Double,
    val count: Int,
    val speed: Double
)

class CrateManager(private val plugin: SkyeCratesPlugin) {
    
    private val crates = mutableMapOf<String, CrateConfig>()
    private val placedCrates = mutableMapOf<Location, String>()
    
    fun loadCrates() {
        crates.clear()
        
        val cratesDir = File(plugin.dataFolder, "crates")
        if (!cratesDir.exists()) {
            cratesDir.mkdirs()
        }
        
        // Copy default example crate if no crates exist
        if (cratesDir.listFiles()?.isEmpty() == true) {
            plugin.saveResource("crates/example.yml", false)
        }
        
        cratesDir.listFiles { file -> file.extension == "yml" }?.forEach { file ->
            loadCrate(file)
        }
        
        plugin.logger.info("Loaded ${crates.size} crate(s)")
    }
    
    private fun loadCrate(file: File) {
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            val crateName = file.nameWithoutExtension
            
            val name = config.getString("name") ?: crateName
            
            // Load particle config
            val particleType = try {
                Particle.valueOf(config.getString("particles.type", "FLAME") ?: "FLAME")
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid particle type in ${file.name}, using FLAME")
                Particle.FLAME
            }
            
            val particles = ParticleConfig(
                type = particleType,
                deltaX = config.getDouble("particles.deltaX", 0.5),
                deltaY = config.getDouble("particles.deltaY", 0.5),
                deltaZ = config.getDouble("particles.deltaZ", 0.5),
                count = config.getInt("particles.count", 15),
                speed = config.getDouble("particles.speed", 0.1)
            )
            
            // Load loot table
            val lootJson = config.getString("loot")
            if (lootJson == null) {
                plugin.logger.warning("No loot table found in ${file.name}")
                return
            }
            
            // Parse and create loot table
            val lootTable = createLootTable(lootJson)
            if (lootTable == null) {
                plugin.logger.warning("Failed to create loot table for ${file.name}")
                return
            }
            
            val crateConfig = CrateConfig(name, particles, lootTable)
            crates[crateName] = crateConfig
            
            plugin.logger.info("Loaded crate: $crateName")
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load crate from ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createLootTable(lootJson: String): LootTable? {
        return try {
            // Validate JSON first
            JsonParser.parseString(lootJson)
            
            // Create a temporary loot table file
            val tempFile = File.createTempFile("skyecrates_loot", ".json")
            tempFile.writeText(lootJson)
            
            // Use Bukkit's loot table registry
            val namespacedKey = NamespacedKey(plugin, "temp_${System.currentTimeMillis()}")
            
            // For Paper/Spigot, we need to use the server's loot table functionality
            val server = Bukkit.getServer()
            val lootTableManager = server.lootTable
            
            // Create a custom loot table that generates items from the JSON
            object : LootTable {
                override fun populateLoot(random: java.util.Random?, context: org.bukkit.loot.LootContext?): MutableCollection<ItemStack> {
                    return generateLootFromJson(lootJson, random ?: java.util.Random())
                }
                
                override fun fillInventory(inventory: org.bukkit.inventory.Inventory?, random: java.util.Random?, context: org.bukkit.loot.LootContext?) {
                    val items = populateLoot(random, context)
                    inventory?.let { inv ->
                        items.forEach { item ->
                            inv.addItem(item)
                        }
                    }
                }
                
                override fun getKey(): NamespacedKey = namespacedKey
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to create loot table: ${e.message}")
            null
        }
    }
    
    private fun generateLootFromJson(lootJson: String, random: java.util.Random): MutableCollection<ItemStack> {
        val items = mutableListOf<ItemStack>()
        
        try {
            val jsonObject = JsonParser.parseString(lootJson).asJsonObject
            val pools = jsonObject.getAsJsonArray("pools")
            
            pools?.forEach { poolElement ->
                val pool = poolElement.asJsonObject
                val rolls = pool.get("rolls")?.asInt ?: 1
                val entries = pool.getAsJsonArray("entries")
                
                repeat(rolls) {
                    entries?.forEach { entryElement ->
                        val entry = entryElement.asJsonObject
                        val type = entry.get("type")?.asString
                        
                        if (type == "minecraft:item") {
                            val itemName = entry.get("name")?.asString
                            val weight = entry.get("weight")?.asInt ?: 1
                            
                            // Simple weight-based selection
                            if (random.nextInt(100) < weight * 10) { // Adjust weight scaling as needed
                                val material = Material.matchMaterial(itemName?.replace("minecraft:", "") ?: "STONE")
                                if (material != null) {
                                    var amount = 1
                                    
                                    // Handle functions (like set_count)
                                    val functions = entry.getAsJsonArray("functions")
                                    functions?.forEach { functionElement ->
                                        val function = functionElement.asJsonObject
                                        val functionType = function.get("function")?.asString
                                        
                                        if (functionType == "minecraft:set_count") {
                                            val count = function.getAsJsonObject("count")
                                            val min = count.get("min")?.asInt ?: 1
                                            val max = count.get("max")?.asInt ?: 1
                                            amount = random.nextInt(max - min + 1) + min
                                        }
                                    }
                                    
                                    items.add(ItemStack(material, amount))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error parsing loot table: ${e.message}")
            // Fallback to a default item
            items.add(ItemStack(Material.STONE))
        }
        
        return items
    }
    
    fun getCrate(name: String): CrateConfig? = crates[name]
    
    fun getAllCrates(): Map<String, CrateConfig> = crates.toMap()
    
    fun placeCrate(location: Location, crateName: String) {
        if (crates.containsKey(crateName)) {
            placedCrates[location] = crateName
            location.block.type = Material.CHEST
        }
    }
    
    fun removeCrate(location: Location): Boolean {
        return placedCrates.remove(location) != null
    }
    
    fun getCrateAt(location: Location): String? = placedCrates[location]
    
    fun getPlacedCrates(): Map<Location, String> = placedCrates.toMap()
    
    fun openCrate(location: Location, player: org.bukkit.entity.Player): Boolean {
        val crateName = getCrateAt(location) ?: return false
        val crate = getCrate(crateName) ?: return false
        
        // Generate loot
        val loot = crate.lootTable.populateLoot(java.util.Random(), null)
        
        // Give items to player
        loot.forEach { item ->
            val remaining = player.inventory.addItem(item)
            // Drop items that don't fit
            remaining.values.forEach { dropItem ->
                player.world.dropItemNaturally(player.location, dropItem)
            }
        }
        
        // Remove crate
        removeCrate(location)
        location.block.type = Material.AIR
        
        // Send message
        player.sendMessage(plugin.configManager.getMessage("crate-opened", "crate" to crate.name))
        
        return true
    }
}
