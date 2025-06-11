package net.skyenetwork.skyecrates.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final SkyeCratesPlugin plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage;
    
    private String pluginName;
    private Component pluginPrefix;
    private long particleInterval;
    private boolean forceParticles;
    
    public ConfigManager(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        pluginName = config.getString("plugin.name", "SkyeCrates");
        
        String prefixString = config.getString("plugin.prefix", "<gray>[<gold>SkyeCrates<gray>]</gray>");
        pluginPrefix = miniMessage.deserialize(prefixString);
        
        particleInterval = config.getLong("particle-interval", 20L);
        forceParticles = "force".equalsIgnoreCase(config.getString("particle-mode", "normal"));
    }
    
    public Component getMessage(String key, String... replacements) {
        String messageString = config.getString("messages." + key, "<red>Message not found: " + key);
        
        // Replace placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                messageString = messageString.replace("{" + placeholder + "}", value);
            }
        }
        
        Component message = miniMessage.deserialize(messageString);
        return pluginPrefix.append(Component.space()).append(message);
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    public Component getPluginPrefix() {
        return pluginPrefix;
    }
    
    public long getParticleInterval() {
        return particleInterval;
    }
    
    public boolean isForceParticles() {
        return forceParticles;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
