# ADR 0001: Plugin-first medieval society stack

**Status:** Accepted  
**Date:** 2026-07-08  
**Deciders:** Design session (plugin-vs-mod evaluation + `CONTEXT.md` war glossary)

## Context

The Kingdom plugin already implements governance (Westminster Parliament, elections), economy (Corona, villager GDP), and law (police hops 1–2). The vision extends to a medieval society with loyalty to the crown, armies, parliamentary law-making, dynamic conquest, and mechanical enforcement.

We evaluated whether Paper plugin limits require a Minecraft mod. Grill sessions resolved six stack questions and produced a full war/loyalty glossary in `CONTEXT.md` (`## War`).

**Constraints agreed:**

| # | Decision |
|---|----------|
| Q1 | Vanilla Java client only — no mod install |
| Q2 | Loyalty = political + military (split tracks) |
| Q3 | Army = player officers + NPC rank-and-file |
| Q4 | Dynamic conquest — siege, chunk capture, border creep |
| Q5 | Mechanical law — Act breaches auto-punish; hard prison |
| Q6 | Single Paper server now; network migration possible later |

A mod would unlock custom UI, formation combat, siege engines, and bespoke units, but **contradicts Q1** unless that constraint is relaxed later.

## Decision

**Stay plugin-first.** Extend Kingdom on Paper with WorldGuard for territory and war, and an optional server resource pack for presentation. Do **not** start a parallel Fabric/NeoForge mod unless an escape hatch is triggered (below).

### Stack

```
Vanilla Java client
    └── Paper 1.21.x / MC 26.x
            ├── Kingdom plugin — state, parliament, economy, police, war domain
            ├── WorldGuard (soft dep) — regions, capture, borders, build/PvP flags
            └── Optional resource pack — heraldry, item art (no client mod)
```

### Domain model (summary)

Authoritative glossary: `CONTEXT.md` `## War`. Highlights:

| Area | Model |
|------|--------|
| **Loyalty** | Split tracks: **loyalty tier** (Faithful → Traitor) and **morale tier** (Steadfast → Rout) |
| **Fealty** | Members get political loyalty on join; military morale after **oath of service** or muster; **sworn outsiders** via oath |
| **Army** | **Standing force** + per-war **levy**; **squads** of **pressed villagers** + **crown squads** under player officers |
| **War** | Monarch tables **war bill**; Commons must approve; **war aim** and outcome (annexation or **war tribute**) named in bill |
| **Siege** | Combat in enemy linked territory with **chunk capture**; **battle** elsewhere |
| **Law** | **Act breach** = violating conduct provisions in enacted Acts; **dual-track offences** apply military and political rules independently; **Traitor** only on conviction |
| **End war** | **Peace bill** or **decisive victory** → **demobilisation** |

### Abstraction rules (vanilla ceiling)

Where historical fidelity would need a mod, the plugin **abstracts**:

| Ambition | Plugin approach |
|----------|-----------------|
| Military morale | Tier messages + potion debuffs + command restrictions; no HUD |
| NPC armies | Capped squads; officer morale tier drives follow/attack/rout |
| Siege feel | Capture points + chunk tally + timers; no breach physics or engines |
| Loyalty | Tier penalties + police escalation; no custom client UI |
| Conquest | WorldGuard region merge on threshold; spike before full design |

## Consequences

### Positive

- All players use vanilla client — low friction, matches current deploy path (`kingdom-0.1.0-SNAPSHOT.jar` + WorldGuard).
- One codebase (Java 21, Maven, TDD domain services) — parliament/economy/police patterns reuse for war.
- Domain terms in `CONTEXT.md` are implementation-agnostic; Bukkit layer can lag domain logic.

### Negative / accepted trade-offs

- No formation combat, morale HUD, or custom siege equipment.
- NPC squads are vanilla mobs with simple AI — scale caps required.
- Dynamic conquest (Q4) is the **highest-risk** plugin row; needs a technical spike on WG region edits.
- Prison and siege enforcement remain server-side heuristics (movement cancel, teleport loop) — good enough per Q5, not cheat-proof.

### Build order (recommended)

1. Police hop 3 — mechanical Act breach → warrant/trial pipeline  
2. Build enforcement — WorldGuard + plugin events (**open PvP**; war-combat gating deferred)  
3. War state machine — war bill, muster, peace, demobilisation (no capture yet)  
4. Loyalty/morale tiers — domain + persistence  
5. Squads — pressed villagers, crown squads, officer morale  
6. **Spike** — chunk capture + WG border shift on test server  
7. Siege + decisive victory + annexation/tribute outcomes  

### Data portability (Q6)

Keep kingdom/war/loyalty state in plugin YAML (or future shared DB schema) without Paper-specific IDs in domain layer — eases Velocity migration later.

## Escape hatches

Revisit this ADR if any trigger fires:

| Trigger | Response |
|---------|----------|
| Q1 relaxed — modded client acceptable for core players | Evaluate `docs/adr/0002-mod-required-for-military.md`; hybrid pack |
| Chunk capture + WG spike fails or performs poorly | Soften Q4 to scripted conquest (monarch-confirmed region handoff) |
| NPC army caps break immersion | Abstract military leg — player PvP + tribute; NPCs cosmetic only |
| War UX needs HUD/maps | Server resource pack first; mod only if insufficient |

## References

- `docs/build-order.md` — vertical-slice implementation plan derived from this ADR  
- `docs/plugin-vs-mod-evaluation.md` — capability matrix and grill answers  
- `CONTEXT.md` `## War` — loyalty, army, siege, parliament-war glossary (~40 terms)  
- `CONTEXT.md` `## Police` — enforcement pipeline for mechanical law  
- `CONTEXT.md` `## Parliament` — bills, Acts, royal assent  

## Supersedes

Nothing. Mod path deferred, not rejected permanently.
