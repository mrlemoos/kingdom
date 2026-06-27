# Kingdom

Paper plugin for admin-defined kingdoms, noble title slots, and a Corona economy.

## Features

### Kingdoms and titles
- `/kingdom create`, `move`, `title`, `setregion`, `setworld` (operators)
- `/kingdom join`, `list`, `info` (players)
- Fixed slots per kingdom: 1 King, 1 Queen, 1 Premier, 1 Speaker, 2 Dukes, 2 Lords, 4 Counts, 8 MPs, unlimited Knights
- Duchess / Countess / Lady / Dame via `feminine` style (or `lady` for Lord)
- Noble prefixes in chat, tab list, and nametags (scoreboard teams)
- Optional WorldGuard region linking per kingdom (soft dependency)
- YAML persistence in `plugins/Kingdom/data.yml`

### Corona economy
- **Corona** — fractional ledger currency; 1 gold nugget = 1 Corona at mints
- **Wallets** — players earn from harvest, craft, villager trades, life events, and `/corona pay` bonuses
- **Treasury** — villager GDP, tax, transfer fees, and budget spending
- **Tax** — skimmed at credit time; Premier proposes rates, King/Queen approves
- **Mints** — lecterns in kingdom territory; `/corona deposit` / `/corona withdraw`
- **Fiscal** — `/kingdom fiscal`, `budget`, `mint` subcommands
- Economy persistence in `plugins/Kingdom/economy.yml`

## Build

```bash
mvn test package
```

Copy `target/kingdom-0.1.0-SNAPSHOT.jar` to your server's `plugins/` folder.

Requires **Paper 1.21.x** (MC 26.x) and **Java 21**. WorldGuard is optional but recommended for territory-linked income and tax.

## Quick start

1. Start the server once to generate config.
2. As OP: `/kingdom create northmarch Northmarch`
3. Players: `/kingdom join northmarch`
4. As OP: `/kingdom title <player> premier` and `/kingdom title <player> king`
5. With WorldGuard: `/kingdom setregion northmarch my_region`
6. Premier: `/kingdom fiscal propose 0.10 0.10 0.01 0.03` then King: `/kingdom fiscal approve`
7. King: `/kingdom budget approve 100` (sets spending cap; treasury must hold Corona to spend)
8. If treasury is empty, OP: `/kingdom treasury credit <kingdom> 100` (or earn via tax and villager GDP)
9. Premier: `/kingdom mint place` at a lectern in territory (costs 50 Corona from treasury by default)
10. Players earn Corona from activity; use `/corona balance` and mints for gold nuggets

New kingdoms default to linked world `world`. Use `/kingdom setworld <kingdom> <world>` if your overworld has a different name.

## Tests

Domain logic is covered with JUnit (kingdom slots, economy tax, income calculators, YAML round-trips).

```bash
mvn test
```

Domain terms and rules are documented in `CONTEXT.md`.
