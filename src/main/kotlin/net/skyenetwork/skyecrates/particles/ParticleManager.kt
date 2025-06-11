package net.skyenetwork.skyecrates.particles

import net.skyenetwork.skyecrates.SkyeCratesPlugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class ParticleManager(private val plugin: SkyeCratesPlugin) {
    
    private var particleTask: BukkitTask? = null
    
    fun start() {
        stop() // Stop any existing task
        
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, {
            spawnParticles()
        }, 0L, plugin.configManager.particleInterval)
    }
    
    fun stop() {
        particleTask?.cancel()
        particleTask = null
    }
    
    fun reload() {
        start() // Restart with new interval
    }
    
    private fun spawnParticles() {
        plugin.crateManager.getPlacedCrates().forEach { (location, crateName) ->
            val crate = plugin.crateManager.getCrate(crateName) ?: return@forEach
            val particles = crate.particles
            
            // Spawn particles above the crate
            val particleLocation = location.clone().add(0.5, 1.5, 0.5)
            
            if (plugin.configManager.forceParticles) {
                // Force particles to all players regardless of their settings
                location.world?.spawnParticle(
                    particles.type,
                    particleLocation,
                    particles.count,
                    particles.deltaX,
                    particles.deltaY,
                    particles.deltaZ,
                    particles.speed,
                    null, // data
                    true  // force - ignores player particle settings
                )
            } else {
                // Normal particles that respect player settings
                location.world?.spawnParticle(
                    particles.type,
                    particleLocation,
                    particles.count,
                    particles.deltaX,
                    particles.deltaY,
                    particles.deltaZ,
                    particles.speed
                )
            }
        }
    }
}
