package net.skyenetwork.skyecrates

import net.skyenetwork.skyecrates.commands.SkyeCratesCommand
import net.skyenetwork.skyecrates.config.ConfigManager
import net.skyenetwork.skyecrates.crates.CrateManager
import net.skyenetwork.skyecrates.listeners.CrateListener
import net.skyenetwork.skyecrates.particles.ParticleManager
import org.bukkit.plugin.java.JavaPlugin

class SkyeCratesPlugin : JavaPlugin() {
    
    lateinit var configManager: ConfigManager
        private set
    
    lateinit var crateManager: CrateManager
        private set
    
    lateinit var particleManager: ParticleManager
        private set
    
    override fun onEnable() {
        // Initialize managers
        configManager = ConfigManager(this)
        crateManager = CrateManager(this)
        particleManager = ParticleManager(this)
        
        // Load configuration
        configManager.loadConfig()
        crateManager.loadCrates()
        
        // Register commands
        getCommand("skyecrates")?.setExecutor(SkyeCratesCommand(this))
        
        // Register listeners
        server.pluginManager.registerEvents(CrateListener(this), this)
        
        // Start particle manager
        particleManager.start()
        
        logger.info("SkyeCrates has been enabled!")
    }
    
    override fun onDisable() {
        particleManager.stop()
        logger.info("SkyeCrates has been disabled!")
    }
    
    fun reload() {
        configManager.loadConfig()
        crateManager.loadCrates()
        particleManager.reload()
    }
}
