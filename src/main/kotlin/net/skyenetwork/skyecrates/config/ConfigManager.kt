package net.skyenetwork.skyecrates.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.skyenetwork.skyecrates.SkyeCratesPlugin
import org.bukkit.configuration.file.FileConfiguration

class ConfigManager(private val plugin: SkyeCratesPlugin) {
    
    private lateinit var config: FileConfiguration
    private val miniMessage = MiniMessage.miniMessage()
    
    var pluginName: String = "SkyeCrates"
        private set
    
    var pluginPrefix: Component = Component.text("[SkyeCrates]")
        private set
    
    var particleInterval: Long = 20L
        private set
    
    var forceParticles: Boolean = false
        private set
    
    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
        
        pluginName = config.getString("plugin.name", "SkyeCrates") ?: "SkyeCrates"
        
        val prefixString = config.getString("plugin.prefix", "<gray>[<gold>SkyeCrates<gray>]</gray>") 
            ?: "<gray>[<gold>SkyeCrates<gray>]</gray>"
        pluginPrefix = miniMessage.deserialize(prefixString)
        
        particleInterval = config.getLong("particle-interval", 20L)
        forceParticles = config.getString("particle-mode", "normal")?.lowercase() == "force"
    }
    
    fun getMessage(key: String, vararg placeholders: Pair<String, String>): Component {
        var messageString = config.getString("messages.$key", "<red>Message not found: $key") 
            ?: "<red>Message not found: $key"
        
        // Replace placeholders
        for ((placeholder, value) in placeholders) {
            messageString = messageString.replace("{$placeholder}", value)
        }
        
        val message = miniMessage.deserialize(messageString)
        return pluginPrefix.append(Component.space()).append(message)
    }
    
    fun getConfig(): FileConfiguration = config
}
