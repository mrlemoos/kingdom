# Plugin vs mod evaluation

Scaffold for deciding whether the Kingdom medieval-society vision stays **Paper plugin–only**, adds **server-side mods**, or requires **client mods**.

Status: **draft** — open questions filled via design sessions.

---

## Purpose

Kingdom already models governance, economy, and law on a Paper server. This doc maps:

1. What a **Paper plugin can do** (with optional plugins like WorldGuard).
2. What it **cannot do reliably** without bending vanilla.
3. Where a **mod** (or hybrid stack) becomes justified.

Goal: avoid premature mod fork; know exactly which ambitions force it.

---

## Current Kingdom scope (plugin today)

| Domain | Status | Notes |
|--------|--------|-------|
| Kingdom membership & noble titles | ✅ Built | Fixed slots, prefixes, hierarchy |
| Westminster Parliament | ✅ Built | Commons divisions, Lords assent, registrar Acts |
| Elections & villager MPs | ✅ Built | General/by-election, profession vote bias, Premier villager |
| Corona economy | ✅ Built | Wallets, treasury, tax, villager GDP, mints |
| Territory (WorldGuard) | ✅ Built | Soft dependency; region-linked income/tax |
| Police / courts | ✅ Built | Warrant → trial → sentence; golem officers |
| Teleport & checkpoints | ✅ Built | `/tp`, `/locate` |
| Build protection | ❌ Not built | AGENTS.md: no block enforcement yet |
| PvP rules | ❌ Not built | No kingdom-war or duel enforcement |
| Army / military | ❌ Not built | No levies, formations, siege |
| Loyalty / allegiance | ❌ Not built | No obedience, desertion, or crown fealty mechanic |
| Population / demographics | ⚠️ Partial | Villager economy proxies society; not full simulation |

---

## Paper plugin capability envelope

Plugins run **server-side only**. Vanilla clients connect unchanged.

### What plugins do well

| Capability | Mechanism | Kingdom examples |
|------------|-----------|------------------|
| Rules & state machines | Java services + YAML | Parliament bill lifecycle, elections |
| Commands & GUIs | Bukkit inventories, Adventure text | Fiscal propose, resignation letter |
| Event hooks | `Listener` on break/place/interact/death/chat | Economy activity, merchant trades |
| Territory | WorldGuard regions (reflection bridge) | Tax jurisdiction, territory wealth |
| NPC behaviour (limited) | Villagers, iron golems, pathfinding API | MPs, Treasury Lord, patrol golems |
| Persistence | Plugin data files | `data.yml`, `economy.yml` |
| Permissions | Bukkit/Paper permissions | Noble roles, teleport audit |
| Scheduled logic | Bukkit scheduler | GDP tick, wealth reconcile, elections |
| Custom items (metadata) | Item NBT / PDC | Locate compass, resignation letter |
| Scoreboard teams | Nametags, prefixes | Noble ranks, police prefixes |

### Workarounds plugins often use

| Need | Plugin pattern | Fidelity |
|------|----------------|----------|
| “Laws” | Server rules enforced on events + punishments | High for willing players; bypassable |
| Prison | Teleport loop + movement cancel | Medium; escape via disconnect/glitch |
| Army | Spawn mobs / tag players / kit commands | Low–medium; no drill, morale, supply |
| Loyalty | Hidden score / permission gates | Abstract; not felt in combat or UI |
| Build bans | Cancel `BlockBreakEvent` + WG flags | High with WorldGuard |
| War | Team damage rules + flagged zones | Medium; no siege engines, morale |

---

## Hard limitations (plugin ceiling)

These are **structural** — not “try harder in Java”.

| Limitation | Why | Symptom for medieval society |
|------------|-----|------------------------------|
| **No client UI** beyond inventories, boss bar, action bar, titles | Client renders vanilla screens | No loyalty meter, levy panel, battle map, law codex UI |
| **No custom entities/models** without resource pack + optional plugin | Server cannot ship new mob geometry | Distinct soldier types, heraldry on units, cavalry |
| **Vanilla AI only** | Mob goals are Mojang’s | No formation fighting, retreat, supply lines, siege AI |
| **Combat is vanilla** | Damage, weapons, armour unchanged | Army = “players with swords”; no pike square, morale rout |
| **Movement/auth is server-trust with client prediction** | Anti-cheat is heuristic | Hard prison, area denial — leaky under lag/exploit |
| **Block/world semantics fixed** | Cannot add new block types (without pack) | No custom fortifications, gates, siege equipment as blocks |
| **Player count & chunk cost** | Simulation in Java on main thread | Large NPC armies lag; must cap or abstract |
| **No authoritative narrative layer** | Quest/book UI only | Laws feel like menus, not embodied culture |
| **Cross-server** | Per-server plugin state unless proxy + DB | Federated realms need extra infra |

### Soft limits (painful but possible on Paper)

| Area | Plugin can… | But… |
|------|-------------|------|
| PvP / friendly fire | Team rules, WG PvP flags, damage events | Griefing edge cases; no war exhaustion |
| Build enforcement | WG + plugin cancel | Needs WG; creative/op bypass |
| “Laws” as mechanics | Acts change config rates, permissions | Criminal law ≠ economic law unless modelled |
| Levies / conscription | Command + kit + teleport | No resistance, draft lottery, or desertion simulation |
| Diplomacy | Treaties as data + chat | No map, borders, or visible envoys |
| Religion / culture | Prefixes, rituals (commands) | Cosmetic unless tied to deep mechanics |

---

## Mod capability envelope (contrast)

| Mod type | Install | Unlocks |
|----------|---------|---------|
| **Server-only** (e.g. some Fabric/NeoForge server mods) | Server jar only | New blocks/entities if clients also have mod — usually **not** vanilla-friendly |
| **Server + client** (typical) | Both sides | Custom UI, entities, combat, structures, keybinds |
| **Proxy + plugin** (Velocity + Paper) | Network layer | Cross-realm identity; still plugin limits per server |

Mods help when the vision needs **embodied** systems: morale, formations, custom weapons, siege, visible society stats, NPC armies that fight like armies.

---

## Feature-area matrix (draft)

Legend: **P** = plugin-sufficient · **H** = hybrid (plugin + WG/pack/other plugin) · **M** = likely needs mod · **?** = needs design answer

| Feature area | Vision (medieval society) | P | H | M | Notes |
|--------------|---------------------------|---|---|---|-------|
| **Parliament & law-making** | Elected MPs, Acts, royal assent | ✅ | | | Already built |
| **Fiscal state** | Tax, budget, treasury spend | ✅ | | | Already built |
| **Courts & police** | Warrants, trial, prison, fines | ✅ | | | Built; mechanical enforcement = wire Act violations → police pipeline |
| **Territory & borders** | Kingdom land, wilderness | | ✅ | | WorldGuard; no dynamic conquest yet |
| **Build / land law** | Who may build where | | ✅ | | WG + plugin; not wired |
| **Noble hierarchy & titles** | Rank, privilege, display | ✅ | | | Built |
| **Loyalty to crown** | Fealty, oath, betrayal; morale & desertion in war | | ? | ⚠️ | Political leg: plugin. Military leg: plugin can abstract (scores, desert debuffs, leave-combat penalties); embodied formations need mod — blocked by Q1 unless abstracted |
| **Army & levies** | Player officers command NPC rank-and-file | | ⚠️ | ⚠️ | Plugin: levy tags, kits, muster zones, player PvP rules. NPC leg: capped mob squads (zombies/skeletons/villagers/iron golems), simple follow/attack — not historical formations |
| **War & conquest** | Siege, chunk capture, border creep | | ⚠️ | ⚠️ | Plugin: war state, capture points, per-chunk control tally, WG region resize/merge on threshold. Missing on vanilla: siege engines, breach physics, supply lines — abstract or RP |
| **PvP** | **Open PvP** for now — vanilla damage on; **war combat** gating deferred; no trial arena |
| **Population simulation** | Villagers as economic citizens | ✅ | | | Villager GDP/trade; not political loyalty |
| **Diplomacy** | Alliances, truces, embargoes | ? | ? | | Data model possible on plugin |
| **Religion / legitimacy** | Divine right, unrest | ? | | ? | Depends on depth |
| **Trade & embargo** | Realm-controlled commerce | ✅ | | | Corona + Acts |
| **Espionage** | Spies, informants | | ? | | Mostly social; plugin tracks flags |
| **Succession crisis** | Regency, pretenders | ? | ✅ | | Title slots exist; crisis flow TBD |

---

## Decision criteria

Favour **stay plugin** when:

- All players must use **vanilla client** (Bedrock/Java vanilla).
- Society is **political & economic** — law, tax, elections, courts, trade.
- Military is **role-play** or **player PvP** with light rules.
- NPCs are **symbols** (MPs, judges, treasury) not battlefield actors.

Consider **mod** when:

- Military must **feel** historical (morale, formation, siege engines).
- **Loyalty** must affect combat, UI, and AI behaviour visibly.
- Laws need **automatic world enforcement** beyond block/chat (custom crimes, evidence).
- You want **custom content** (units, weapons, fort blocks) as first-class.

Consider **hybrid** (plugin + resource pack + WG + optional client mod for enthusiasts):

- Plugin owns **state & law**; pack/mod owns **presentation**.
- Lowest risk path: extend Kingdom on Paper until matrix rows flip to **M**.

---

## Open questions

Filled during grill sessions. Each answer may move matrix cells.

| # | Question | Answer | Impact |
|---|----------|--------|--------|
| 1 | Must players use vanilla client? | **Yes** — vanilla Java only; no mod install | Mod path off unless vision changes; plugin + WG + resource pack ceiling |
| 2 | What does “loyalty” mean mechanically? | **C — Both** — political (oath, treason, rank) + military (morale, desertion, cohesion) | Political = plugin; military depth fights vanilla ceiling — abstract/RPG layer or soften Q1 later |
| 3 | What does “army” mean — players, NPCs, or both? | **C — Both** — player officers + NPC rank-and-file | Player war = plugin; NPC armies = vanilla mobs + pathfinding ceiling — cap scale, abstract reserves, or soften Q1 |
| 4 | Is conquest (land transfer by war) in scope? | **C — Dynamic** — siege, chunk capture, automatic border creep | Tension with Q1: plugin can do chunk flags + WG region edits; siege “feel” (engines, breach, morale rout) needs abstraction or mod |
| 5 | How strict must law enforcement be? | **B — Mechanical** — Act breaches auto-punish; hard prison; escape = crime | Plugin-sufficient; prison = teleport loop + movement cancel; hop 3 justice pipeline aligns |
| 6 | Single server or realm network? | **C — Start single; network later if needed** | Single Paper now; keep YAML/domain portable for future Velocity + shared DB |

---

## Grill verdict (2026-07-08)

All open questions resolved. **Stay plugin-first** — Q1 (vanilla client) blocks mod path.

| Answer | Implication |
|--------|-------------|
| Q1 vanilla only | No client UI, custom entities, formation combat |
| Q2 loyalty = political + military | Split: courts/ranks vs abstract morale/desertion |
| Q3 army = players + NPCs | Player PvP war + capped NPC squads; not Total War |
| Q4 conquest = dynamic | Chunk capture + WG border edits — **hardest plugin row** |
| Q5 law = mechanical | Act violations → police pipeline; hard prison |
| Q6 single → maybe network | One server now; don't bake in proxy assumptions |

### Verdict

**Mod not required** if ambitions abstracted:

- **Loyalty (military leg):** hidden score, desert debuffs, leave-war-zone penalties — no morale HUD
- **NPC army:** small capped squads, follow/attack — not formations
- **Siege:** capture points + chunk tally + timer — not breach physics

**Mod would help** (ruled out by Q1 unless vision changes): custom units, siege engines, loyalty HUD, formation AI.

### Recommended stack

```
Paper plugin (Kingdom) — state, law, war, economy
    + WorldGuard — territory, capture, borders, build/PvP flags
    + optional resource pack — heraldry, item art (no client mod)
```

### Escape hatches (if plugin ceiling hit)

1. Soften Q1 → modded client for enthusiasts  
2. Abstract military further (war = player PvP + tribute; NPCs cosmetic)  
3. Soften Q4 → scripted conquest (ex-Q4-B) not chunk creep  

---

## Related docs (to create if needed)

| Doc | When |
|-----|------|
| `docs/adr/0001-plugin-first-society-stack.md` | ✅ Accepted 2026-07-08 |
| `docs/adr/0002-mod-required-for-military.md` | If army simulation forces client mod |
| `CONTEXT.md` | Glossary terms only (loyalty, army, fealty) — after terms agreed |

---

## Next steps

1. ~~Resolve open questions~~ ✅  
2. Add glossary terms to `CONTEXT.md` (loyalty, army, siege, desertion) — grill-with-docs  
3. ~~Write `docs/adr/0001-plugin-first-society-stack.md`~~ ✅  
4. Prioritise build order: mechanical law (hop 3) → build/PvP flags → war state → chunk capture  
5. Spike chunk-capture + WG region edit on test server before full siege design
