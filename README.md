# JM Waypoint Manager (JMM)

> A modern UI on top of JourneyMap's waypoint list — search, filter, bulk operations.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-jm--waypoint--manager-00AF5C)](https://modrinth.com/mod/jm-waypoint-manager)
[![CurseForge](https://img.shields.io/badge/CurseForge-jm--waypoint--manager-F16436)](https://www.curseforge.com/minecraft/mc-mods/jm-waypoint-manager)

---

## What it does

JourneyMap is great, but its built-in waypoint list is minimal — no search,
limited sorting, no bulk operations. JM Waypoint Manager adds a screen
that lets you actually **manage** a large waypoint collection:

- **Search** by name (live filter)
- **Sort** by name / distance from player / dimension
- **Filter** by enabled/disabled, dimension
- **Bulk-select** multiple waypoints (checkboxes)
- **Bulk delete** / bulk toggle enabled
- **Click the color dot** to cycle through an 8-color palette
- **Click the row** to toggle enabled

All waypoints — whether you created them manually in JM, or auto-generated
by [Compass to Map](https://modrinth.com/mod/compass-to-map),
[Ping to Map](https://modrinth.com/mod/ping-to-map), or other JM-aware
mods — show up in one unified list.

## How to open

Default keybind: **Y** (rebindable in Controls). Avoids `J` / `M` / `B`
(used by JourneyMap itself) and `L` (vanilla advancement screen).

## Supported Loaders / Versions

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | — | — ¹ |
| 1.20.1 |  —  | ✅ | ✅ |

¹ *JourneyMap has no Fabric release for MC 1.21.1 (only 1.21.11+); the
Fabric 1.21.1 build is therefore skipped.*

## Installation

1. Install **JourneyMap** for your loader (required).
2. Drop the matching `jmwaypointmanager-{loader}-{mc}-{version}.jar` into
   your `mods/` folder.
3. Launch — press **Y** in-game to open the manager.

## Dependencies

- **JourneyMap** (required — this mod has nothing to manage without it)

## How it differs from JM's built-in waypoint list

| Feature | JM built-in | JM Waypoint Manager |
|---|:---:|:---:|
| Search by name | ✗ | ✅ |
| Sort by distance | ✗ | ✅ |
| Sort by dimension | ✗ | ✅ |
| Multi-select | ✗ | ✅ |
| Bulk delete | ✗ | ✅ |
| Bulk toggle enabled | ✗ | ✅ |
| Quick color change | ✗ | ✅ |
| Filter by enabled state | ✗ | ✅ |

## License

[MIT License](LICENSE) — modpack inclusion welcome, no credit required.

## Credits

- Author: KURONAMI
- Built on the [JourneyMap API](https://github.com/TeamJM/journeymap-api)
  (TeamJM)
