package net.skyenetwork.skyecrates;

import net.skyenetwork.skyecrates.commands.SkyeCratesCommand;
import net.skyenetwork.skyecrates.config.ConfigManager;
import net.skyenetwork.skyecrates.crates.CrateManager;
import net.skyenetwork.skyecrates.listeners.CrateListener;
import net.skyenetwork.skyecrates.particles.ParticleManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyeCratesPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private CrateManager crateManager;
    private ParticleManager particleManager;
    
    @Override
    public void onEnable() {
        try {
            getLogger().info("Starting SkyeCrates plugin...");
            
            // Initialize managers
            configManager = new ConfigManager(this);
            crateManager = new CrateManager(this);
            particleManager = new ParticleManager(this);
            
            // Load configuration
            getLogger().info("Loading configuration...");
            configManager.loadConfig();
            crateManager.loadCrates();
            
            // Register commands
            getLogger().info("Registering commands...");
            if (getCommand("skyecrates") != null) {
                getCommand("skyecrates").setExecutor(new SkyeCratesCommand(this));
            }
            
            // Register listeners
            getLogger().info("Registering listeners...");
            getServer().getPluginManager().registerEvents(new CrateListener(this), this);
            
            // Start particle manager
            getLogger().info("Starting particle manager...");
            particleManager.start();
            
            getLogger().info("SkyeCrates has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable SkyeCrates: " + e.getMessage());
            e.printStackTrace();
            // Disable the plugin to prevent further issues
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (particleManager != null) {
            particleManager.stop();
        }
        getLogger().info("SkyeCrates has been disabled!");
    }
    
    public void reload() {
        configManager.loadConfig();
        crateManager.loadCrates();
        particleManager.reload();
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CrateManager getCrateManager() {
        return crateManager;
    }
    
    public ParticleManager getParticleManager() {
        return particleManager;
    }
}
