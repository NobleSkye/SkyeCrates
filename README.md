# SkyeCrates

A Minecraft plugin for Spigot/Paper servers that adds customizable crates with loot tables and particle effects.

## Features

- **Minecraft Loot Tables**: Uses native Minecraft loot table format (compatible with Misode's loot table generator)
- **Particle Effects**: Customizable particle effects for each crate type
- **Easy Configuration**: YAML configuration files for each crate type
- **Permissions**: Built-in permission system for admins and users
- **Commands**: Full command system for managing crates

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

### Example Crate Configuration

```yaml
# Name of the crate (displayed to players)
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
- `/skyecrates give <player> <crate>` - Give a crate item to a player
- `/skyecrates place <crate>` - Place a crate at your target location
- `/skyecrates remove` - Remove a crate you're looking at

## Permissions

- `skyecrates.admin` - Access to all SkyeCrates commands (default: op)
- `skyecrates.use` - Allows using crates (default: true)

## Using Misode's Loot Table Generator

1. Go to [Misode's Loot Table Generator](https://misode.github.io/loot-table/)
2. Create your desired loot table
3. Copy the generated JSON
4. Paste it into the `loot:` section of your crate configuration

## Building from Source

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`

## License

This project is licensed under the MIT License.
