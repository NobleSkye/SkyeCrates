package net.skyenetwork.skyecrates.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyenetwork.skyecrates.SkyeCratesPlugin;
import net.skyenetwork.skyecrates.crates.CrateManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SkyeCratesCommand implements CommandExecutor, TabCompleter {
    
    private final SkyeCratesPlugin plugin;
    private final MiniMessage miniMessage;
    
    public SkyeCratesCommand(SkyeCratesPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skyecrates.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "place":
                handlePlace(sender, args);
                break;
            case "remove":
                handleRemove(sender);
                break;
            case "givekey":
                handleGiveKey(sender, args);
                break;
            case "keys":
                handleKeys(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gold>=== SkyeCrates Commands ==="));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates reload <gray>- Reload plugin configuration"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates list <gray>- List all available crates"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates give <player> <crate> [amount] <gray>- Give a crate placer to a player"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates place <crate> <gray>- Place a crate at your target location"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates remove <gray>- Remove a crate you're looking at"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates givekey <player> <crate> [amount] <gray>- Give keys to a player"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/skyecrates keys <gray>- Show your key counts"));
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
    }
    
    private void handleList(CommandSender sender) {
        Map<String, CrateManager.CrateConfig> crates = plugin.getCrateManager().getAllCrates();
        if (crates.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize("<red>No crates found!"));
            return;
        }
        
        sender.sendMessage(miniMessage.deserialize("<gold>=== Available Crates ==="));
        for (Map.Entry<String, CrateManager.CrateConfig> entry : crates.entrySet()) {
            String key = entry.getKey();
            CrateManager.CrateConfig crate = entry.getValue();
            sender.sendMessage(miniMessage.deserialize("<yellow>" + key + " <gray>- <white>" + crate.getName()));
        }
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /skyecrates give <player> <crate> [amount]"));
            return;
        }
        
        String targetName = args[1];
        String crateName = args[2];
        int amount = 1;
        
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(miniMessage.deserialize("<red>Invalid amount: " + args[3]));
                return;
            }
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("crate-not-found"));
            return;
        }
        
        // Create crate placer item (paper with custom display name)
        ItemStack crateItem = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = crateItem.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize("<gold>" + crate.getName() + " Placer"));
            meta.lore(Arrays.asList(
                miniMessage.deserialize("<gray>Right-click on a block to place this crate!"),
                miniMessage.deserialize("<yellow>Crate: " + crate.getName())
            ));
            
            // Add NBT data to identify the crate type
            NamespacedKey nbtKey = new NamespacedKey(plugin, "crate_type");
            meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, crateName);
            
            crateItem.setItemMeta(meta);
        }
        
        // Give item to player
        target.getInventory().addItem(crateItem);
        
        sender.sendMessage(plugin.getConfigManager().getMessage("crate-given", 
            "amount", String.valueOf(amount),
            "crate", crate.getName(),
            "player", target.getName()
        ));
    }
    
    private void handlePlace(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by players!"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /skyecrates place <crate>"));
            return;
        }
        
        Player player = (Player) sender;
        String crateName = args[1];
        CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("crate-not-found"));
            return;
        }
        
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-target-block"));
            return;
        }
        
        plugin.getCrateManager().placeCrate(targetBlock.getLocation(), crateName);
        sender.sendMessage(plugin.getConfigManager().getMessage("crate-placed", "crate", crate.getName()));
    }
    
    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by players!"));
            return;
        }
        
        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-crate-to-remove"));
            return;
        }
        
        if (plugin.getCrateManager().removeCrate(targetBlock.getLocation())) {
            targetBlock.setType(Material.AIR);
            sender.sendMessage(plugin.getConfigManager().getMessage("crate-removed"));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-crate-to-remove"));
        }
    }
    
    private void handleGiveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /skyecrates givekey <player> <crate> [amount]"));
            return;
        }
        
        String targetName = args[1];
        String crateName = args[2];
        int amount = 1;
        
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(miniMessage.deserialize("<red>Invalid amount: " + args[3]));
                return;
            }
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("crate-not-found"));
            return;
        }
        
        // Give keys to player
        plugin.getCrateManager().addKeys(target, crateName, amount);
        
        sender.sendMessage(plugin.getConfigManager().getMessage("key-given",
            "amount", String.valueOf(amount),
            "crate", crate.getName(),
            "player", target.getName()
        ));
        
        target.sendMessage(plugin.getConfigManager().getMessage("key-received",
            "amount", String.valueOf(amount),
            "crate", crate.getName()
        ));
    }
    
    private void handleKeys(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by players!"));
            return;
        }
        
        Player player = (Player) sender;
        Map<String, Integer> playerKeys = plugin.getCrateManager().getPlayerKeys(player);
        
        if (playerKeys.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize("<yellow>You don't have any keys!"));
            return;
        }
        
        sender.sendMessage(miniMessage.deserialize("<gold>=== Your Keys ==="));
        for (Map.Entry<String, Integer> entry : playerKeys.entrySet()) {
            String crateName = entry.getKey();
            int keyCount = entry.getValue();
            CrateManager.CrateConfig crate = plugin.getCrateManager().getCrate(crateName);
            String displayName = crate != null ? crate.getName() : crateName;
            
            sender.sendMessage(miniMessage.deserialize(
                "<yellow>" + displayName + " <gray>- <white>" + keyCount + " key" + (keyCount == 1 ? "" : "s")
            ));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("skyecrates.admin")) {
            return new ArrayList<>();
        }
        
        switch (args.length) {
            case 1:
                return Arrays.asList("reload", "list", "give", "place", "remove", "givekey", "keys")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            case 2:
                switch (args[0].toLowerCase()) {
                    case "give":
                    case "givekey":
                        return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    case "place":
                        return plugin.getCrateManager().getAllCrates().keySet().stream()
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    default:
                        return new ArrayList<>();
                }
            case 3:
                if ("give".equals(args[0].toLowerCase()) || "givekey".equals(args[0].toLowerCase())) {
                    return plugin.getCrateManager().getAllCrates().keySet().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
                return new ArrayList<>();
            case 4:
                if ("give".equals(args[0].toLowerCase()) || "givekey".equals(args[0].toLowerCase())) {
                    return Arrays.asList("1", "5", "10");
                }
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }
}
