package net.skyenetwork.skyecrates.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import net.skyenetwork.skyecrates.crates.CrateManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CratePreviewGUI implements Listener {
    
    private final SkyeCratesPlugin plugin;
    private final MiniMessage miniMessage;
    
    public CratePreviewGUI(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    public void openPreview(Player player, String crateName) {
        CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, miniMessage.deserialize("<gold>Preview: " + crate.getName()));
        
        List<PreviewItem> previewItems = parsePreviewItems(crate.getLootJson());
        
        // Fill GUI with preview items
        int slot = 0;
        for (PreviewItem previewItem : previewItems) {
            if (slot >= 54) break;
            
            ItemStack displayItem = previewItem.item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                if (meta.hasLore() && meta.lore() != null) {
                    for (var component : meta.lore()) {
                        lore.add(miniMessage.serialize(component));
                    }
                }
                
                lore.add("");
                lore.add(miniMessage.serialize(miniMessage.deserialize("<gray>Weight: <white>" + previewItem.weight)));
                
                // Calculate approximate chance percentage
                int totalWeight = previewItems.stream().mapToInt(p -> p.weight).sum();
                double chance = ((double) previewItem.weight / totalWeight) * 100;
                lore.add(miniMessage.serialize(miniMessage.deserialize("<gray>Chance: <yellow>" + String.format("%.1f", chance) + "%")));
                
                if (previewItem.minCount != previewItem.maxCount) {
                    lore.add(miniMessage.serialize(miniMessage.deserialize("<gray>Amount: <white>" + previewItem.minCount + "-" + previewItem.maxCount)));
                } else if (previewItem.minCount > 1) {
                    lore.add(miniMessage.serialize(miniMessage.deserialize("<gray>Amount: <white>" + previewItem.minCount)));
                }
                
                meta.lore(lore.stream().map(miniMessage::deserialize).toList());
                displayItem.setItemMeta(meta);
            }
            
            gui.setItem(slot++, displayItem);
        }
        
        // Fill empty slots with gray glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(miniMessage.deserialize("<gray> "));
            filler.setItemMeta(fillerMeta);
        }
        
        for (int i = slot; i < 54; i++) {
            gui.setItem(i, filler);
        }
        
        player.openInventory(gui);
    }
    
    private List<PreviewItem> parsePreviewItems(String lootJson) {
        List<PreviewItem> items = new ArrayList<>();
        
        try {
            JsonObject jsonObject = JsonParser.parseString(lootJson).getAsJsonObject();
            JsonArray pools = jsonObject.getAsJsonArray("pools");
            
            if (pools != null) {
                for (JsonElement poolElement : pools) {
                    JsonObject pool = poolElement.getAsJsonObject();
                    JsonArray entries = pool.getAsJsonArray("entries");
                    
                    if (entries != null) {
                        for (JsonElement entryElement : entries) {
                            JsonObject entry = entryElement.getAsJsonObject();
                            String type = entry.has("type") ? entry.get("type").getAsString() : "";
                            
                            if ("minecraft:item".equals(type)) {
                                String itemName = entry.has("name") ? entry.get("name").getAsString() : "";
                                int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                                
                                String materialName = itemName.replace("minecraft:", "").toUpperCase();
                                Material material = Material.matchMaterial(materialName);
                                if (material != null) {
                                    int minCount = 1;
                                    int maxCount = 1;
                                    
                                    // Check for set_count function
                                    if (entry.has("functions")) {
                                        JsonArray functions = entry.getAsJsonArray("functions");
                                        for (JsonElement functionElement : functions) {
                                            JsonObject function = functionElement.getAsJsonObject();
                                            String functionType = function.has("function") ? function.get("function").getAsString() : "";
                                            
                                            if ("minecraft:set_count".equals(functionType) && function.has("count")) {
                                                JsonObject count = function.getAsJsonObject("count");
                                                minCount = count.has("min") ? count.get("min").getAsInt() : 1;
                                                maxCount = count.has("max") ? count.get("max").getAsInt() : 1;
                                            }
                                        }
                                    }
                                    
                                    items.add(new PreviewItem(new ItemStack(material), weight, minCount, maxCount));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing loot table for preview: " + e.getMessage());
        }
        
        return items;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (miniMessage.stripTags(title).startsWith("Preview:")) {
            event.setCancelled(true);
        }
    }
    
    private static class PreviewItem {
        ItemStack item;
        int weight;
        int minCount;
        int maxCount;
        
        PreviewItem(ItemStack item, int weight, int minCount, int maxCount) {
            this.item = item;
            this.weight = weight;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }
    }
}
