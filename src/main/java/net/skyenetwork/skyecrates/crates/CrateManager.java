package net.skyenetwork.skyecrates.crates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class CrateManager {
    
    private final SkyeCratesPlugin plugin;
    private final Map<String, CrateConfig> crates;
    private final Map<Location, String> placedCrates;
    private final Map<UUID, Map<String, Integer>> playerKeys; // Player UUID -> Crate Name -> Key Count
    
    public CrateManager(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.crates = new HashMap<>();
        this.placedCrates = new HashMap<>();
        this.playerKeys = new HashMap<>();
    }
    
    public void loadCrates() {
        crates.clear();
        
        File cratesDir = new File(plugin.getDataFolder(), "crates");
        if (!cratesDir.exists()) {
            cratesDir.mkdirs();
        }
        
        // Copy default example crate if no crates exist
        File[] crateFiles = cratesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (crateFiles == null || crateFiles.length == 0) {
            plugin.saveResource("crates/example.yml", false);
        }
        
        crateFiles = cratesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (crateFiles != null) {
            for (File file : crateFiles) {
                loadCrate(file);
            }
        }
        
        plugin.getLogger().info("Loaded " + crates.size() + " crate(s)");
    }
    
    private void loadCrate(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String crateName = file.getName().replace(".yml", "");
            
            String name = config.getString("name", crateName);
            
            // Load particle config
            Particle particleType;
            try {
                String particleTypeString = config.getString("particles.type", "FLAME");
                particleType = Particle.valueOf(particleTypeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in " + file.getName() + ", using FLAME");
                particleType = Particle.FLAME;
            }
            
            ParticleConfig particles = new ParticleConfig(
                particleType,
                config.getDouble("particles.deltaX", 0.5),
                config.getDouble("particles.deltaY", 0.5),
                config.getDouble("particles.deltaZ", 0.5),
                config.getInt("particles.count", 15),
                config.getDouble("particles.speed", 0.1)
            );
            
            // Load loot table
            String lootJson = config.getString("loot");
            if (lootJson == null) {
                plugin.getLogger().warning("No loot table found in " + file.getName());
                return;
            }
            
            // Validate JSON
            try {
                JsonParser.parseString(lootJson);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid JSON in loot table for " + file.getName() + ": " + e.getMessage());
                return;
            }
            
            CrateConfig crateConfig = new CrateConfig(name, particles, lootJson);
            crates.put(crateName, crateConfig);
            
            plugin.getLogger().info("Loaded crate: " + crateName);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load crate from " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public List<ItemStack> generateLootFromCrate(String crateName) {
        CrateConfig crate = getCrate(crateName);
        if (crate == null) {
            return new ArrayList<>();
        }
        return generateLootFromJson(crate.getLootJson(), new Random());
    }
    
    private List<ItemStack> generateLootFromJson(String lootJson, Random random) {
        List<ItemStack> items = new ArrayList<>();
        
        try {
            JsonObject jsonObject = JsonParser.parseString(lootJson).getAsJsonObject();
            JsonArray pools = jsonObject.getAsJsonArray("pools");
            
            if (pools != null) {
                for (JsonElement poolElement : pools) {
                    JsonObject pool = poolElement.getAsJsonObject();
                    int rolls = pool.has("rolls") ? pool.get("rolls").getAsInt() : 1;
                    JsonArray entries = pool.getAsJsonArray("entries");
                    
                    for (int i = 0; i < rolls; i++) {
                        if (entries != null) {
                            for (JsonElement entryElement : entries) {
                                JsonObject entry = entryElement.getAsJsonObject();
                                String type = entry.has("type") ? entry.get("type").getAsString() : "";
                                
                                if ("minecraft:item".equals(type)) {
                                    String itemName = entry.has("name") ? entry.get("name").getAsString() : "";
                                    int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                                    
                                    // Simple weight-based selection
                                    if (random.nextInt(100) < weight * 10) { // Adjust weight scaling as needed
                                        String materialName = itemName.replace("minecraft:", "").toUpperCase();
                                        Material material = Material.matchMaterial(materialName);
                                        if (material != null) {
                                            int amount = 1;
                                            
                                            // Handle functions (like set_count)
                                            if (entry.has("functions")) {
                                                JsonArray functions = entry.getAsJsonArray("functions");
                                                for (JsonElement functionElement : functions) {
                                                    JsonObject function = functionElement.getAsJsonObject();
                                                    String functionType = function.has("function") ? function.get("function").getAsString() : "";
                                                    
                                                    if ("minecraft:set_count".equals(functionType) && function.has("count")) {
                                                        JsonObject count = function.getAsJsonObject("count");
                                                        int min = count.has("min") ? count.get("min").getAsInt() : 1;
                                                        int max = count.has("max") ? count.get("max").getAsInt() : 1;
                                                        amount = random.nextInt(max - min + 1) + min;
                                                    }
                                                }
                                            }
                                            
                                            items.add(new ItemStack(material, amount));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing loot table: " + e.getMessage());
            // Fallback to a default item
            items.add(new ItemStack(Material.STONE));
        }
        
        return items;
    }
    
    public CrateConfig getCrate(String name) {
        return crates.get(name);
    }
    
    public Map<String, CrateConfig> getAllCrates() {
        return new HashMap<>(crates);
    }
    
    public void placeCrate(Location location, String crateName) {
        if (crates.containsKey(crateName)) {
            placedCrates.put(location, crateName);
            location.getBlock().setType(Material.BARRIER);
        }
    }
    
    public boolean removeCrate(Location location) {
        return placedCrates.remove(location) != null;
    }
    
    public String getCrateAt(Location location) {
        return placedCrates.get(location);
    }
    
    public Map<Location, String> getPlacedCrates() {
        return new HashMap<>(placedCrates);
    }
    
    public boolean openCrate(Location location, Player player) {
        String crateName = getCrateAt(location);
        if (crateName == null) {
            return false;
        }
        
        CrateConfig crate = getCrate(crateName);
        if (crate == null) {
            return false;
        }
        
        // Check if player has a key for this crate
        if (!hasKey(player, crateName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-key", "crate", crate.getName()));
            return false;
        }
        
        // Use the key
        if (!useKey(player, crateName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-key", "crate", crate.getName()));
            return false;
        }
        
        // Generate loot
        List<ItemStack> loot = generateLootFromCrate(crateName);
         // Give items to player
        for (ItemStack item : loot) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            // Drop items that don't fit
            for (ItemStack dropItem : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
            }
        }

        // Send message (crate stays in place)
        player.sendMessage(plugin.getConfigManager().getMessage("crate-opened", "crate", crate.getName()));

        return true;
    }
    
    // Key management methods
    public void addKeys(Player player, String crateName, int amount) {
        UUID playerId = player.getUniqueId();
        playerKeys.computeIfAbsent(playerId, k -> new HashMap<>());
        playerKeys.get(playerId).merge(crateName, amount, Integer::sum);
    }
    
    public boolean hasKey(Player player, String crateName) {
        UUID playerId = player.getUniqueId();
        return playerKeys.containsKey(playerId) && 
               playerKeys.get(playerId).getOrDefault(crateName, 0) > 0;
    }
    
    public boolean useKey(Player player, String crateName) {
        UUID playerId = player.getUniqueId();
        if (!hasKey(player, crateName)) {
            return false;
        }
        
        Map<String, Integer> keys = playerKeys.get(playerId);
        int currentKeys = keys.get(crateName);
        if (currentKeys > 1) {
            keys.put(crateName, currentKeys - 1);
        } else {
            keys.remove(crateName);
        }
        
        return true;
    }
    
    public int getKeyCount(Player player, String crateName) {
        UUID playerId = player.getUniqueId();
        return playerKeys.getOrDefault(playerId, new HashMap<>()).getOrDefault(crateName, 0);
    }
    
    public Map<String, Integer> getPlayerKeys(Player player) {
        UUID playerId = player.getUniqueId();
        return new HashMap<>(playerKeys.getOrDefault(playerId, new HashMap<>()));
    }

    public static class CrateConfig {
        private final String name;
        private final ParticleConfig particles;
        private final String lootJson;
        
        public CrateConfig(String name, ParticleConfig particles, String lootJson) {
            this.name = name;
            this.particles = particles;
            this.lootJson = lootJson;
        }
        
        public String getName() {
            return name;
        }
        
        public ParticleConfig getParticles() {
            return particles;
        }
        
        public String getLootJson() {
            return lootJson;
        }
    }
    
    public static class ParticleConfig {
        private final Particle type;
        private final double deltaX;
        private final double deltaY;
        private final double deltaZ;
        private final int count;
        private final double speed;
        
        public ParticleConfig(Particle type, double deltaX, double deltaY, double deltaZ, int count, double speed) {
            this.type = type;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.deltaZ = deltaZ;
            this.count = count;
            this.speed = speed;
        }
        
        public Particle getType() {
            return type;
        }
        
        public double getDeltaX() {
            return deltaX;
        }
        
        public double getDeltaY() {
            return deltaY;
        }
        
        public double getDeltaZ() {
            return deltaZ;
        }
        
        public int getCount() {
            return count;
        }
        
        public double getSpeed() {
            return speed;
        }
    }
}
