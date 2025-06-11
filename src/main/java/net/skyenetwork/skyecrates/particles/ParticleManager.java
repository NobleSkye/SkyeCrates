package net.skyenetwork.skyecrates.particles;

import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import net.skyenetwork.skyecrates.crates.CrateManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class ParticleManager {
    
    private final SkyeCratesPlugin plugin;
    private BukkitTask particleTask;
    
    public ParticleManager(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        stop(); // Stop any existing task
        
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, 
                0L, plugin.getConfigManager().getParticleInterval());
    }
    
    public void stop() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
    
    public void reload() {
        start(); // Restart with new interval
    }
    
    private void spawnParticles() {
        Map<Location, String> placedCrates = plugin.getCrateManager().getPlacedCrates();
        
        for (Map.Entry<Location, String> entry : placedCrates.entrySet()) {
            Location location = entry.getKey();
            String crateName = entry.getValue();
            
            CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
            if (crate == null) {
                continue;
            }
            
            CrateManager.ParticleConfig particles = crate.getParticles();
            
            // Spawn particles above the crate
            Location particleLocation = location.clone().add(0.5, 1.5, 0.5);
            
            if (plugin.getConfigManager().isForceParticles()) {
                // Force particles to all players regardless of their settings
                if (location.getWorld() != null) {
                    location.getWorld().spawnParticle(
                        particles.getType(),
                        particleLocation,
                        particles.getCount(),
                        particles.getDeltaX(),
                        particles.getDeltaY(),
                        particles.getDeltaZ(),
                        particles.getSpeed(),
                        null, // data
                        true  // force - ignores player particle settings
                    );
                }
            } else {
                // Normal particles that respect player settings
                if (location.getWorld() != null) {
                    location.getWorld().spawnParticle(
                        particles.getType(),
                        particleLocation,
                        particles.getCount(),
                        particles.getDeltaX(),
                        particles.getDeltaY(),
                        particles.getDeltaZ(),
                        particles.getSpeed()
                    );
                }
            }
        }
    }
}
