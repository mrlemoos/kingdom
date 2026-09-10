# Spike 0.1: WorldGuard chunk-capture

**Status:** Domain pass complete; safe Bukkit capture-plan probe added. WorldGuard mutation not attempted.
**Date:** 2026-07-09  
**Slice:** `docs/build-order.md` § Phase 0 — Slice 0.1

## Goal

Prove that per-chunk **capture** tallies and a test **region merge** on threshold are feasible, starting with a domain-only model with no war bill coupling.

## What was built

Pure Java domain types under `dev.mrlemoos.kingdom.war.capture` — no Bukkit types:

- `ChunkCoord(String worldName, int chunkX, int chunkZ)` — domain chunk identifier.
- `ChunkCaptureTally` — records attacker/defender presence per chunk each tick; flips control to the attacker once their presence has outnumbered the defender's for a configurable number of consecutive ticks (`flipThresholdTicks`). Equal presence resets the streak (debounce). Symmetric recapture: sustained defender presence after a flip clears the attacker's control.
- `BlockVertex(int blockX, int blockZ)` — 2D block-coordinate vertex for a proposed region boundary.
- `RegionMergePlan` — `fromCapturedChunks(attackerKingdomId, defenderKingdomId, capturedChunks)` computes the bounding-rectangle vertices (block coordinates) enclosing the captured chunk set, and retains the chunk set itself as `chunksToMerge`.
- `/kingdom-spike capture <spike-attacker> <spike-defender>` — OP-only, in-memory mark for the caller's current chunk. Both kingdom ids must begin `spike-`; linked regions must begin `kingdom-spike-`; caller must stand in defender region.
- `/kingdom-spike plan <spike-attacker> <spike-defender>` — after three marks, prints `RegionMergePlan` vertices. It cannot mutate WorldGuard.

## Findings

- A simple consecutive-tick streak counter per chunk is enough to express "attacker presence > defender over N ticks" and symmetric recapture, without needing a rolling window or timestamps — the Bukkit presence sampler just calls `tickPresence` once per sample.
- Deriving a **bounding rectangle** from an arbitrary (possibly non-contiguous or non-rectangular) captured-chunk set is a reasonable first approximation for `proposedVertices`, but it can enclose *uncaptured* chunks if the captured area is not itself rectangular (e.g. an L-shape). This is acceptable for a first WorldGuard cuboid-region merge, but a polygonal-region merge (tracing the actual chunk-set outline) would be more accurate and is worth revisiting once we see real capture shapes in Slice 6.6.
- Keeping `ChunkCoord` string/int-based (no `World`/`Chunk` references) means the domain is trivially unit-testable without MockBukkit and keeps `RegionMergePlan` free of any WorldGuard/Bukkit dependency until the merge is actually applied.

## Automated findings

- `mvn test` verifies tally flip/recapture/debounce and rectangular plan coordinates. The probe reuses those domain types and has no WorldGuard write path.
- Existing `WorldGuardBridge` can query region membership and bounds only. It has no region-update or persistence API. No live Paper/WorldGuard server was available here, so no tick-cost or merge claim is made.

## Manual acceptance gate

On an isolated Paper + WorldGuard server, create two linked `spike-*` kingdoms and `kingdom-spike-*` regions in one world. Mark three chunks inside defender region, run `plan`, then record vertices and sampling cost. Do not edit either region until an operator has made an external WorldGuard region-file backup and approved a restore procedure. A real merge requires a separate, restore-backed WorldGuard mutation slice.

## Open questions for the Bukkit merge pass

1. **Cuboid vs polygonal region** — `WorldGuardBridge` currently wraps whatever region type is linked; does annexation need a new polygonal region, or is redrawing the existing cuboid's bounds (via the computed vertices) acceptable for the first cut?
2. **Presence sampling cost** — what tick interval and radius keeps `tickPresence` calls affordable at scale (many simultaneous sieges)? Slice 0.1's Bukkit half should measure this directly.
3. **Rollback** — if `region merge` fails partway (e.g. WorldGuard save error), what is the safe rollback: keep per-chunk `occupation` state until merge confirms, or snapshot the old region bounds first?
4. **Chunk ownership overlap** — can a chunk be simultaneously "captured" by two different attacker kingdoms if two wars touch the same defender? `ChunkCaptureTally` currently allows only one controlling attacker per chunk, which should be fine for the "one defender per war bill" scope, but multi-war overlap on the same defender needs a decision before Slice 6.2.
5. **Territory threshold interaction** — Slice 6.6 will gate `region merge` on the war-aim percentage; this spike's `RegionMergePlan.fromCapturedChunks` is a pure computation and does not itself check any threshold — that check belongs in the Bukkit/war-service caller.

## Not done

- No actual `WorldGuardBridge` region redraw.
- No tick-cost measurement on a real server.

These remain for the Bukkit half of Slice 0.1 before Phase 6 scope is finalised.
