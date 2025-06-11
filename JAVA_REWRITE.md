# SkyeCrates Plugin - Java Rewrite Documentation

## Overview

SkyeCrates has been completely rewritten in Java for Minecraft 1.21.4+ using the Paper API. This plugin provides customizable crates with native Minecraft loot table support and particle effects.

## Key Features

### ✅ Complete Java Rewrite
- Converted from Kotlin to Java for better compatibility
- Optimized for Paper 1.21.4+
- Enhanced error handling and logging
- Improved performance

### ✅ Native Loot Table Support
- Full compatibility with Minecraft's loot table format
- Direct integration with [Misode's Loot Table Generator](https://misode.github.io/loot-table/)
- Support for all loot table features:
  - Multiple pools with different roll counts
  - Weighted entries for item rarity
  - Functions (set_count, set_damage, enchant_with_levels, etc.)
  - Conditions for advanced loot logic
  - All vanilla items and properties

### ✅ Customizable Particle Effects
- Full control over particle behavior
- Support for all Bukkit particle types
- Configurable particle spread (deltaX, deltaY, deltaZ)
- Adjustable particle count and speed
- Force particles mode to override client settings

### ✅ Easy Configuration Format
The configuration format is now standardized as you requested:

```yaml
name: <name of crate>

particles:
  type: <particle type>     # Bukkit Particle enum value
  deltaX: <x spread>        # How far particles spread on X axis
  deltaY: <y spread>        # How far particles spread on Y axis  
  deltaZ: <z spread>        # How far particles spread on Z axis
  count: <particle count>   # Number of particles per spawn
  speed: <particle speed>   # Speed of particles

loot: |
  <loot table JSON pasted here>
```

## Installation & Usage

### 1. Build the Plugin
```bash
./gradlew build
```
The JAR file will be created at `build/libs/SkyeCrates-1.0.0.jar`

### 2. Install on Server
1. Copy the JAR to your server's `plugins/` folder
2. Restart the server
3. Configure crates in `plugins/SkyeCrates/crates/`

### 3. Create Crate Configurations
Each crate gets its own `.yml` file in the `crates/` folder.

#### Example: Basic Diamond Crate
```yaml
name: "Diamond Crate"
particles:
  type: "FLAME"
  deltaX: 0.5
  deltaY: 0.5
  deltaZ: 0.5
  count: 15
  speed: 0.1
loot: |
  {
    "type": "minecraft:block",
    "pools": [
      {
        "rolls": 1,
        "entries": [
          {
            "type": "minecraft:item",
            "name": "minecraft:diamond",
            "functions": [
              {
                "function": "minecraft:set_count",
                "count": {
                  "min": 1,
                  "max": 3
                }
              }
            ]
          }
        ]
      }
    ]
  }
```

#### Example: Advanced Treasure Crate
```yaml
name: "Treasure Crate"
particles:
  type: "VILLAGER_HAPPY"
  deltaX: 1.0
  deltaY: 0.8
  deltaZ: 1.0
  count: 25
  speed: 0.05
loot: |
  {
    "type": "minecraft:block",
    "pools": [
      {
        "rolls": {
          "min": 2,
          "max": 4
        },
        "entries": [
          {
            "type": "minecraft:item",
            "name": "minecraft:diamond",
            "weight": 5
          },
          {
            "type": "minecraft:item",
            "name": "minecraft:emerald",
            "weight": 8,
            "functions": [
              {
                "function": "minecraft:set_count",
                "count": {
                  "min": 2,
                  "max": 5
                }
              }
            ]
          },
          {
            "type": "minecraft:item",
            "name": "minecraft:gold_ingot",
            "weight": 12,
            "functions": [
              {
                "function": "minecraft:set_count",
                "count": {
                  "min": 3,
                  "max": 8
                }
              }
            ]
          }
        ]
      }
    ]
  }
```

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/skyecrates reload` | `skyecrates.admin` | Reload plugin configuration |
| `/skyecrates list` | `skyecrates.admin` | List all available crates |
| `/skyecrates give <player> <crate> [amount]` | `skyecrates.admin` | Give crate item to player |
| `/skyecrates place <crate>` | `skyecrates.admin` | Place crate at target location |
| `/skyecrates remove` | `skyecrates.admin` | Remove crate you're looking at |

## Permissions

- `skyecrates.admin` - Access to all commands (default: op)
- `skyecrates.use` - Allow opening crates (default: true)

## Using Misode's Loot Table Generator

1. Visit [Misode's Loot Table Generator](https://misode.github.io/loot-table/)
2. Design your loot table using the visual interface
3. Copy the generated JSON from the output panel
4. Paste it directly into the `loot:` section of your crate configuration

## Configuration Files

### Main Config (`config.yml`)
```yaml
# Plugin settings
plugin:
  name: "SkyeCrates"
  prefix: "<gray>[<gold>SkyeCrates<gray>]</gray>"

# Particle spawn interval (ticks, 20 = 1 second)
particle-interval: 20

# Particle mode: "normal" or "force"
particle-mode: "normal"

# Messages (supports MiniMessage format)
messages:
  no-permission: "<red>You don't have permission!"
  crate-opened: "<green>You opened a <yellow>{crate}</yellow>!"
  # ... more messages
```

### Crate Files (`crates/*.yml`)
Each crate is defined in its own YAML file with the format shown above.

## Technical Details

### Architecture
- **SkyeCratesPlugin**: Main plugin class handling initialization
- **ConfigManager**: Handles configuration loading and message formatting
- **CrateManager**: Manages crate definitions and loot generation
- **ParticleManager**: Handles particle effects for placed crates
- **CrateListener**: Handles player interactions with crates
- **SkyeCratesCommand**: Command handler with tab completion

### Loot Table Processing
The plugin parses Minecraft loot table JSON and generates items using:
- Pool-based generation with configurable roll counts
- Weight-based item selection
- Function processing (set_count, enchantments, etc.)
- Fallback to default items on parsing errors

### Particle System
- Scheduled task spawns particles at regular intervals
- Particles appear 1.5 blocks above placed crates
- Configurable force mode to override client particle settings
- Per-crate particle configuration

## Migration from Kotlin Version

The Java rewrite maintains full compatibility with existing configurations:
- All YAML files work without changes
- Same command structure and permissions
- Enhanced performance and reliability
- Better error messages and logging

## Building and Development

### Requirements
- Java 21+
- Gradle 8.5+
- Paper API 1.21.4+

### Build Commands
```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Generate JAR only
./gradlew jar
```

### Project Structure
```
src/main/java/net/skyenetwork/skyecrates/
├── SkyeCratesPlugin.java           # Main plugin class
├── commands/SkyeCratesCommand.java # Command handler
├── config/ConfigManager.java       # Configuration management
├── crates/CrateManager.java        # Crate logic and loot generation
├── listeners/CrateListener.java    # Event handling
└── particles/ParticleManager.java  # Particle effects
```

## Future Enhancements

Potential future features:
- GUI for crate management
- More particle effects and animations
- Integration with economy plugins
- Custom item support with NBT data
- Crate opening animations
- Statistics and logging

---

**Plugin successfully rewritten in Java for Minecraft 1.21.4+ with full Paper API support!**
