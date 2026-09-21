# Handoff — Seasonal snowfall

Make it snow in winter. Grilled to a settled design on 2026-09-21; **nothing is built yet**.
Every decision below is agreed — do not re-open them, implement them. Genuinely new questions
(naming, a default value that looks wrong once you see the code) get grilled one at a time with
a recommended answer, per `AGENTS.md`.

Read `AGENTS.md` and `CONTEXT.md` first. British spelling in all user-facing messages. TDD for
domain logic. Tracer-bullet slices, minimal diff, match existing conventions. Do not commit or
push unless asked.

---

## Working-tree state you are inheriting

`main` is ahead of `origin/main`. Leave unrelated uncommitted files alone; do not revert or
commit them. At the time this was written that was `deploy/README.md`,
`.cursor/settings.local.json` and `docs/handoff-treaty-and-player-tax.md`.

---

## What the feature is

Two things, one season concern:

1. **Storminess.** Winter refuses to let the weather clear, so precipitation runs most of the
   season. Config-tuned per season.
2. **Snowfall in biomes that do not normally get it.** Inside linked territory only, winter
   rewrites biomes to snowy counterparts; the thaw rewrites them back and melts what winter left.

---

## Platform facts, already verified against `paper-api-26.1.2.build.74-stable`

Do not re-derive these. They were checked in the jar and the sources jar.

- `Player#setPlayerWeather` takes only `CLEAR`/`DOWNFALL`. **It cannot turn rain into snow.**
  Rain vs snow is decided by biome temperature, client-side. Per-player fake weather is a dead
  end; biome rewriting is the only path without NMS or ProtocolLib. **Do not add a dependency.**
- `RegionAccessor#setBiome(int x, int y, int z, Biome)` writes the **noise biome, one entry per
  4×4×4 quart**, not per block. The unit of work is a quart, not a block.
- `World#refreshChunk(int x, int z)` exists and is **not** deprecated. This is how the client
  sees a biome change without relogging. `regenerateChunk` is deprecated and throws — ignore it.
- `Biome` is a registry interface (`org.bukkit.block.Biome extends OldEnum<Biome>, Keyed`), not
  an enum. No `values()`, no `switch`. Resolve names through the registry
  (`RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)`) with a `NamespacedKey`, and
  handle an unknown key by dropping the pair with a warning, not by throwing.
- Vanilla does **not** melt ice or snow from sunlight — melting needs block light > 11. Leftover
  winter ice will not clear itself. Hence the melt sweep below.
- `WorldGuardBridge.regionBounds(worldName, regionId)` already exists
  (`src/main/java/dev/mrlemoos/kingdom/worldguard/WorldGuardBridge.java:131`) and returns a
  `RegionBounds(minX, minY, minZ, maxX, maxY, maxZ)`. Territory scoping is free — use it.

---

## Decisions already taken

| # | Decision | Rationale |
|---|---|---|
| 1 | Both storminess **and** real snowfall in warm biomes. Not particles, not a cosmetic fake. | User chose it knowing the cost. |
| 2 | Biome rewrite is scoped to **each kingdom's linked WorldGuard region only**. Wilderness stays vanilla. | Bounded extent, bounded revert, matches how hearths/permits/villager sweeps already scope. A kingdom with no linked region gets storminess and nothing else. |
| 3 | The thaw uses an **injective config pairing and its inverse**. Nothing is snapshotted, nothing is persisted. | A snapshot of a 200×200 region is tens of thousands of `data.yml` entries. The one lossy case — terrain that was *naturally* snowy gets thawed to its warm counterpart the first spring — is the feature, not a bug: a snowy city that stays snowy in Highmead is what seasons exist to abolish. |
| 4 | Snowy-biome side effects are **accepted**: water freezes, snow layers form on farmland, farmland dries under ice, strays and polar bears spawn. | Freezing and snowfall both come from biome temperature; they cannot be split. Winter already bites (crop growth 0.5, hearths required, levy upkeep 1.5×). Frozen farms fit. `snowAccumulationHeight` was rejected — it is per-world, so it would leak outside territory. |
| 5 | The thaw **melts**: `ICE` → `WATER`, `SNOW` (layer) → `AIR`. `PACKED_ICE`, `BLUE_ICE` and `SNOW_BLOCK` are left alone — those are player builds. | Vanilla will not melt them itself (see platform facts). |
| 6 | The thaw rule is **`season != WINTER`**, not "spring and summer". | One rule covers spring, summer and autumn. Autumn has nothing to thaw anyway. |
| 7 | Reconcile is **per-chunk and idempotent**, driven by `ChunkLoadEvent` plus a 1200-tick sweep over loaded chunks. No one-shot season-turn task. | No progress state, no force-loading, survives restart free, self-heals. Same shape as the villager despawn-protection sweep. A chunk nobody has loaded stays wrong — invisible, nobody is there to see it. |
| 8 | Biome work per chunk: **whole column within the region bounds**, ~1536 `setBiome` writes. **Dirty-check first**; only call `refreshChunk` if something actually changed. | Array writes are cheap; the dirty check makes repeat visits free. |
| 9 | Melt work per chunk: **surface only**. `getHighestBlockYAt(x, z)` for each of the 256 columns, check that block and the one above. | A full chunk scan is ~98k blocks. Ice under an overhang and snow in a ravine stay frozen — accepted. |
| 10 | Overworld only: `World.Environment.NORMAL`. | Nether and End have no seasons — same guard as `SeasonalHostileSpawnListener`. |
| 11 | No `enabled` flag. An empty `snow.biome-swap` map turns the whole biome half off. | One fewer knob. |

---

## Files

Two new, three touched.

### New — `listener/SeasonalSnowListener.java`

One class, `Listener` **and** `Runnable`. Deliberately not split into two: storms and snow are
the same season concern, and fewest files wins.

- `WeatherChangeEvent` — when the event is turning weather **off**, refuse it with probability
  `stormChance`. Expose the decision as a pure static, mirroring
  `SeasonalHostileSpawnListener.shouldReinforce(double, double)`:
  `static boolean shouldRefuseClearing(double stormChance, double roll)`.
- `ChunkLoadEvent` — reconcile that chunk.
- `run()` — sweep loaded chunks, reconcile each.
- Reconcile, for a chunk overlapping a linked region: winter → ensure snowy; otherwise → ensure
  thawed and melt the surface. Idempotent, both directions, no stored state.

Name follows the existing `Seasonal*Listener` convention in `listener/`.

### New — `calendar/SnowBiomeMap.java`

A record. Parses `snow.biome-swap`, validates injectivity, exposes `freeze(Biome)` and
`thaw(Biome)` as `Optional<Biome>`.

**Injectivity is the load-time contract.** Two warm biomes sharing one snowy counterpart make
the inverse ambiguous, so the colliding pair is **dropped and logged at WARNING** — never snow a
region you cannot thaw. Do not throw; a bad config line must not stop the plugin loading.

### Touched — `calendar/SeasonProfile.java`

Add `double stormChance` as a tenth record component. All five `new SeasonProfile(...)` call
sites are inside that one file, so this is a cheap change — update `defaults(Season)` and
`fromPluginConfig(...)` together. Suggested defaults: winter 0.7, autumn 0.2, spring 0.1,
summer 0.0 (0.0 = vanilla, never interferes).

### Touched — `src/main/resources/config.yml`

```yaml
season:
  winter:
    storm-chance: 0.7    # chance a weather-clear is refused
  autumn:
    storm-chance: 0.2
  spring:
    storm-chance: 0.1
  summer:
    storm-chance: 0.0

# Winter rewrites these biomes inside linked territory and puts them back at the thaw.
# The table must be injective — one snowy biome per warm biome — or the thaw cannot tell
# what a quart used to be. Colliding pairs are dropped with a warning. Anything unmapped
# never snows. Empty table turns snowfall off entirely.
snow:
  biome-swap:
    plains: snowy_plains
    sunflower_plains: ice_spikes
    forest: snowy_taiga
    taiga: grove
    meadow: snowy_slopes
    beach: snowy_beach
    river: frozen_river
    ocean: frozen_ocean
    deep_ocean: deep_frozen_ocean
```

**Be honest about the ceiling in the config comment:** vanilla has roughly ten snowy biomes, so
injectivity caps the table at about ten pairs. Desert, savanna and jungle are unmapped on
purpose — there is no spare snowy counterpart for them. If a desert kingdom must have snow, give
`desert: ice_spikes` and drop `sunflower_plains`; that trade-off is the user's call, so put it
to them rather than deciding it.

Keep the prose voice of the surrounding config — the existing comments are written in-world
("What each season asks of the realm and grants it"). Match that.

### Touched — `KingdomPlugin.java`

Register the listener and schedule the sweep at 1200L, alongside the existing sweeps near
`KingdomPlugin.java:707`.

---

## Tests — TDD, write them failing first

Pure statics only. **Do not reach for MockBukkit here** — biome and chunk mocks are unreliable,
and the reconcile logic worth testing is already extracted into the statics below.

- `calendar/SnowBiomeMapTest` — injective validation drops a colliding pair and keeps the rest;
  `freeze`/`thaw` round-trip; unknown biome returns empty; unknown registry key is dropped, not
  thrown; empty map is a total no-op.
- `listener/SeasonalSnowListenerTest` — `shouldRefuseClearing(stormChance, roll)`. Mirror
  `SeasonalHostileSpawnListenerTest`, which is the established shape for exactly this.
- `calendar/SeasonProfileTest` — **already exists**. Extend it for `stormChance` defaults and the
  config read; do not write a new file.

`mvn test` must pass. Run `ReadLints` on every touched Java file — Eclipse JDT null analysis is
on, and `Optional.map` with a method reference on a `@Nonnull` receiver will trip it. Guard with
`isPresent()`/`isEmpty()`.

---

## Explicitly out of scope

- Per-player biome or weather spoofing, ProtocolLib, NMS, any new dependency.
- Snow outside linked territory.
- Snapshotting original biomes.
- Any change to `hearth`, crop growth, granary or levy upkeep — winter already handles those.
- Touching `PACKED_ICE`, `BLUE_ICE` or `SNOW_BLOCK`.

---

## Before you finish

- `CONTEXT.md` — add the agreed terms to the seasons section (glossary only, no implementation
  detail): **snowfall**, **thaw**, **biome swap**.
- `docs/idea-backlog.md` — mark the row done only once the work lands.
- If the user intends to deploy, run `mvn package` and give them the JAR path. **Confirm the live
  server version first** — the server is on MC 26.2 and `deploy/plugins-26.1.2/` is a legacy
  folder name, not a target.
