# SkyeCrates

A Minecraft plugin for Paper/Spigot servers that adds customizable crates with native Minecraft loot tables and particle effects.

## Features

- **Native Minecraft Loot Tables**: Full support for Minecraft's loot table format, compatible with [Misode's Loot Table Generator](https://misode.github.io/loot-table/)
- **Particle Effects**: Customizable particle effects for each crate type with full control over particle behavior
- **Easy Configuration**: YAML configuration files for each crate type
- **Permissions**: Built-in permission system for admins and users
- **Commands**: Complete command system for managing crates
- **Paper API**: Built specifically for Paper 1.21.4+ with modern Java features

## Requirements

- Minecraft 1.21.4+
- Paper server (recommended) or Spigot
- Java 21+

## Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure your crates in `plugins/SkyeCrates/crates/`

## Configuration

Each crate is configured in its own YAML file in the `plugins/SkyeCrates/crates/` folder.

### Crate Configuration Format

```yaml
name: <name of crate>

particles:
  type: <particle type>    # Bukkit Particle enum value
  deltaX: <x spread>       # How far particles spread on X axis
  deltaY: <y spread>       # How far particles spread on Y axis  
  deltaZ: <z spread>       # How far particles spread on Z axis
  count: <particle count>  # Number of particles per spawn
  speed: <particle speed>  # Speed of particles

loot: |
  <loot table JSON pasted here>
```

### Example Crate Configuration

```yaml
# Example Crate Configuration
name: "Example Crate"

# Particle configuration
particles:
  type: "FLAME"          # Particle type (see Bukkit Particle enum)
  deltaX: 0.5           # Particle spread on X axis
  deltaY: 0.5           # Particle spread on Y axis  
  deltaZ: 0.5           # Particle spread on Z axis
  count: 15             # Number of particles per spawn
  speed: 0.1            # Particle speed

# Loot table (JSON format from Misode's loot table generator)
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
            "weight": 1,
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

## Commands

- `/skyecrates reload` - Reload plugin configuration
- `/skyecrates list` - List all available crates
- `/skyecrates give <player> <crate> [amount]` - Give a crate item to a player
- `/skyecrates place <crate>` - Place a crate at your target location
- `/skyecrates remove` - Remove a crate you're looking at

## Permissions

- `skyecrates.admin` - Access to all SkyeCrates commands (default: op)
- `skyecrates.use` - Allows using crates (default: true)

## Using Misode's Loot Table Generator

1. Go to [Misode's Loot Table Generator](https://misode.github.io/loot-table/)
2. Create your desired loot table using the visual interface
3. Copy the generated JSON from the output
4. Paste it directly into the `loot:` section of your crate configuration file

### Supported Loot Table Features

- **Multiple pools** with different roll counts
- **Weighted entries** for item rarity
- **Functions** like `set_count`, `set_damage`, `enchant_with_levels`
- **Conditions** for advanced loot logic
- **All vanilla items** and their properties

## Example Configurations

### Basic Crate
Simple crate with a single guaranteed diamond:

```yaml
name: "Basic Crate"
particles:
  type: "FLAME"
  deltaX: 0.5
  deltaY: 0.5
  deltaZ: 0.5
  count: 10
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
            "name": "minecraft:diamond"
          }
        ]
      }
    ]
  }
```

### Treasure Crate
Advanced crate with multiple items and different rarities:

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
          }
        ]
      }
    ]
  }
```

## Building from Source

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`

## Migration from Kotlin Version

This is a complete Java rewrite of the original Kotlin plugin. Configuration files are compatible, but the plugin is now:
- Written in pure Java for better compatibility
- Optimized for Paper 1.21.4+
- Enhanced with better loot table parsing
- Improved error handling and logging

## License

This project is licensed under the MIT License.
