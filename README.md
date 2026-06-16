# Kingdom

Spigot plugin for admin-defined kingdoms, one-time player membership, and noble title slots.

## Features

- `/kingdom create`, `move`, `title`, `setregion`, `setworld` (operators)
- `/kingdom join`, `list`, `info` (players)
- Fixed slots per kingdom: 1 King, 1 Queen, 2 Dukes, 4 Counts
- Duchess / Countess via `feminine` style on duke/count slots
- Noble prefixes in chat, tab list, and nametags (scoreboard teams)
- Optional WorldGuard region linking per kingdom (soft dependency)
- YAML persistence in `plugins/Kingdom/data.yml`

## Build

```bash
mvn test package
```

Copy `target/kingdom-0.1.0-SNAPSHOT.jar` to your server's `plugins/` folder.

Requires **Spigot 1.21.x** and **Java 21**. WorldGuard is optional for `/kingdom setregion`.

## Quick start

1. Start the server once to generate config.
2. As OP: `/kingdom create northmarch Northmarch`
3. Players: `/kingdom join northmarch`
4. As OP: `/kingdom title <player> duke feminine`
5. With WorldGuard: `/kingdom setregion northmarch my_region`

New kingdoms default to linked world `world`. Use `/kingdom setworld <kingdom> <world>` if your overworld has a different name.

## Tests

Domain logic is covered with JUnit (slot limits, one-time join, move clears title, prefixes, territory labels).

```bash
mvn test
```
