# Kingdom war/society stack — vertical-slice build order

**Status:** Planning  
**Date:** 2026-07-08  
**Derived from:** [`docs/adr/0001-plugin-first-society-stack.md`](adr/0001-plugin-first-society-stack.md), [`docs/plugin-vs-mod-evaluation.md`](plugin-vs-mod-evaluation.md), `CONTEXT.md` (`## War`, `## Police`, `## Parliament`)

This document turns the ADR recommended build order into tracer-bullet vertical slices. Each slice is a small, shippable increment: domain logic first (JUnit, TDD), then a thin Bukkit layer for manual verification on a test server.

**Conventions**

| Layer | Responsibility | Verification |
|-------|----------------|--------------|
| **Domain** | Pure Java services, models, persistence adapters without Bukkit types | `mvn test` |
| **Bukkit** | Listeners, commands, GUIs, entity spawn, WorldGuard bridge calls | Manual on Paper + WorldGuard test server |

Glossary terms are defined only in `CONTEXT.md`; this doc references them by name.

---

## Critical path

```
Police hop 3 (Act breach → warrant)
    → Build enforcement (conduct provisions become enforceable; **open PvP**)
    → [Early risk spike: chunk capture + WG region edit]
    → War state machine (war bill, muster, peace, demobilisation)
    → Loyalty/morale tiers
    → Squads (rank-and-file, officer morale)
    → Siege, decisive victory, annexation/tribute
```

The **chunk-capture spike** is scheduled after build/PvP enforcement so occupation rules have something to exercise, but **before** full siege design commits to data models and player UX. If the spike fails, trigger ADR escape hatch: soften Q4 to scripted conquest (monarch-confirmed **region merge** handoff).

---

## Phase 0 — Early risk spike (parallel after Phase 2)

### Slice 0.1 — WorldGuard chunk-capture spike

| | |
|---|---|
| **Goal** | Prove that per-chunk **capture** tallies and a test **region merge** on threshold are feasible via `WorldGuardBridge` without unacceptable lag or data loss. |
| **Domain** | Minimal `ChunkCaptureTally` value object and `RegionMergePlan` (chunk set → proposed region vertices); no war bill coupling. |
| **Bukkit** | Dev-only command or OP script: mark chunks captured in a sandbox region, run merge on test kingdom pair, measure tick cost. |
| **Depends on** | Existing `WorldGuardBridge`, linked territory on two test kingdoms. |
| **Acceptance (domain)** | Unit tests: tally flip when attacker presence > defender over N ticks; merge plan computes correct chunk set from tally. |
| **Acceptance (Bukkit)** | On test server: flip ≥3 chunks, run merge, defender region shrinks and attacker region grows; rollback documented. |
| **Spike vs flag** | **Spike** — time-boxed; findings recorded in spike notes; not player-facing. Outcome gates Phase 6 scope. |

---

## Phase 1 — Police hop 3: mechanical law

Extends hops 1–2 (sworn roles, **warrant** → **trial** → **sentence** pipeline, golems, **cells**, **court**) with automatic **Act breach** detection and crown workflow.

### Slice 1.1 — Conduct provisions on enacted Acts

| | |
|---|---|
| **Goal** | Enacted **Act**s can carry **conduct provision**s (build ban, curfew, war limit) separate from fiscal rate fields. PvP restrictions are out of scope under **open PvP**. |
| **Domain** | `ConductProvision` model; extend `AssentedAct` / enactment persistence; parser from bill text or structured bill fields. |
| **Bukkit** | Registrar book lore or bill GUI exposes provisions read-only after royal assent. |
| **Depends on** | Existing Parliament enactment (`ParliamentEnactment`, **Registrar**). |
| **Acceptance (domain)** | Tests: fiscal-only Act has no provisions; war/supply Act with conduct embeds provisions; round-trip YAML. |
| **Spike vs flag** | **Feature** — ships with Parliament; unused until enforcement listeners exist. |

### Slice 1.2 — Act breach detection service

| | |
|---|---|
| **Goal** | Domain service evaluates player actions against active conduct provisions for their **jurisdiction** kingdom. |
| **Domain** | `ActBreachDetector`: given event facts (broke block, dealt damage, etc.), returns optional breach record with Act id and provision type. |
| **Bukkit** | None in this slice — facts passed in from tests. |
| **Depends on** | Slice 1.1. |
| **Acceptance (domain)** | Tests: build in banned zone → breach; fiscal rate change alone → no breach; visitor in territory → breach attributed to visitor's presence kingdom. |
| **Spike vs flag** | **Feature** — domain-only until wired. |

### Slice 1.3 — Political offence and loyalty tier drop on breach

| | |
|---|---|
| **Goal** | First **Act breach** lowers **political loyalty** to **Doubtful**; repeat/severe breaches → **Disloyal** per `CONTEXT.md` weights. |
| **Domain** | `LoyaltyService` (political track only): tier transitions, persistence keys without UUID in domain interface. |
| **Bukkit** | Action bar / chat message on tier change; persist in `data.yml` war/loyalty section. |
| **Depends on** | Slice 1.2. |
| **Acceptance (domain)** | Tests: Faithful → Doubtful on first breach; Doubtful → Disloyal on second; **Traitor** not applied without conviction. |
| **Spike vs flag** | **Feature flag** `loyalty.political.enabled` (default on in dev). |

### Slice 1.4 — Auto-warrant pipeline for Act breach

| | |
|---|---|
| **Goal** | Detected **Act breach** opens a constable **warrant** application or auto-filed case per kingdom policy; **royal warrant approval** required before **active warrant**. |
| **Domain** | `MechanicalJusticeService`: breach → warrant draft → pending crown approval; reuses police case model. |
| **Bukkit** | Paper item workflow mirroring **resignation letter** / **royal warrant approval**; constable notified. |
| **Depends on** | Slices 1.2–1.3, existing `PoliceService`, **infrastructure gate**. |
| **Acceptance (domain)** | Tests: breach creates pending warrant; crown reject leaves no **active warrant**; approve activates; **immunity** blocks crown warrants. |
| **Acceptance (Bukkit)** | Manual: breach → crown receives review paper → approve → patrol golem may **arrest** in **jurisdiction**. |
| **Spike vs flag** | **Feature flag** `police.mechanical-act-breach.enabled`. |

### Slice 1.5 — Act breach through trial to sentence

| | |
|---|---|
| **Goal** | Complete hop 3: mechanical case flows through **pending trial**, **trial** (player or **realm-handled trial**), **sentence** (fine, **prison sentence**, **warning**). |
| **Domain** | Wire breach severity to default charge table; treason charges deferred to manual constable. |
| **Bukkit** | Existing trial GUI and **hard confinement** for prison; escape attempt logs as new breach. |
| **Depends on** | Slice 1.4, police trial pipeline. |
| **Acceptance (domain)** | Tests: warning sentence records offence without tier change beyond 1.3; fine debits Corona; prison blocks kingdom teleport. **Law hardening (shipped):** wanted nametag policy; prison spawn save/restore + elected vacate / appointed suspend; trial jury seating/majority/timeout→realm-handled; villager Speaker immunity + villager convict economy freeze. |
| **Acceptance (Bukkit)** | Manual end-to-end: break block under build-ban Act → warrant → trial → cell. Wanted nametag + `/tp` prison ban + State Opening skip wired; full villager warrant Bukkit path still TODO. |
| **Spike vs flag** | **Feature** — completes hop 3. |

---

## Phase 2 — Build enforcement

Conduct provisions become mechanically enforceable in linked territory. **Open PvP** applies: player damage is not gated in this phase (Slices 2.2 and 3.6 deferred).

### Slice 2.1 — Build conduct enforcement

| | |
|---|---|
| **Goal** | Cancel block place/break/interact when an enacted conduct provision forbids it in **jurisdiction**. |
| **Domain** | Reuse `ActBreachDetector` with `BlockActionFacts`; optional WG flag sync helper (interface only). |
| **Bukkit** | `BlockBreakEvent` / `BlockPlaceEvent` listeners; call breach pipeline from Slice 1.4 when violation intentional. |
| **Depends on** | Phase 1 complete, `WorldGuardBridge` territory resolution. |
| **Acceptance (domain)** | Tests: member vs visitor; exempt OP; breach fired once per action cluster (debounce). |
| **Acceptance (Bukkit)** | Manual: enact build-ban Act → non-exempt player cannot break stone in territory. |
| **Spike vs flag** | **Feature flag** `enforcement.build.enabled`. |

### Slice 2.2 — PvP and war-combat gating *(deferred)*

| | |
|---|---|
| **Goal** | *(Deferred.)* Future: **war combat**, **friendly fire**, **siege neutral** via damage events; **battlefield treason** detection. |
| **Domain** | `PvpConductEvaluator` stub only. |
| **Bukkit** | None until **open PvP** lifted. |
| **Depends on** | Slice 2.1, Phase 3 war state. |
| **Acceptance** | N/A — not shipped under open PvP. |
| **Spike vs flag** | **Deferred** — `enforcement.pvp.enabled` default off. |

### Slice 2.3 — Curfew and movement conduct (lightweight)

| | |
|---|---|
| **Goal** | Time-window conduct provisions (in-game day phase) block entry or flag breach outside **jurisdiction** hours. |
| **Domain** | `CurfewEvaluator` using world time facts. |
| **Bukkit** | Periodic tick or move listener; message on breach. |
| **Depends on** | Slice 2.1. |
| **Acceptance (domain)** | Tests: inside curfew window → allowed; outside → breach. |
| **Acceptance (Bukkit)** | Manual: enact curfew Act → night movement flagged. |
| **Spike vs flag** | **Feature flag** `enforcement.curfew.enabled` (optional slice — defer if scope tight). |

---

## Phase 3 — War state machine (no capture yet)

Parliamentary war authorisation and lifecycle without **chunk capture** — validates **war bill**, **peace bill**, **muster**, **demobilisation** flows.

### Slice 3.1 — War bill type and enactment

| | |
|---|---|
| **Goal** | Monarch tables **war bill** naming sole target kingdom, **war aim**, victory **outcome** (**annexation** or **war tribute**), and **muster deadline** duration; Commons **division** + **royal assent** required. |
| **Domain** | `WarBill` extends bill model; enactment creates `ActiveWar` state; coalitions rejected at validation. |
| **Bukkit** | Parliament GUI branch for war bills; **Registrar** records enacted war Act. |
| **Depends on** | Existing **Bill** / **Act** pipeline (`## Parliament`). |
| **Acceptance (domain)** | Tests: no second active war per attacker; invalid target rejected; enactment sets war clock and muster deadline. |
| **Acceptance (Bukkit)** | Manual: table war bill → division → assent → kingdom war status visible in `/kingdom info`. |
| **Spike vs flag** | **Feature flag** `war.enabled` (master). |

### Slice 3.2 — Declaration of war and bilateral war state

| | |
|---|---|
| **Goal** | On **war bill** enactment, create an **active war**; both **belligerents** are **at war**; broadcast **declaration of war** messaging. **Battlefield treason** becomes possible. Defender may later table **counter-war** as separate bill. |
| **Domain** | `WarStateMachine`: enacted → **at war** → ended; `ActiveWar` record; persist in `data.yml` without Bukkit world ids in domain records. |
| **Bukkit** | Broadcast + kingdom info war section. |
| **Depends on** | Slice 3.1. |
| **Acceptance (domain)** | Tests: peace only when active; counter-war bill validation requires prior defender role. |
| **Spike vs flag** | **Feature** under `war.enabled`. |

### Slice 3.3 — Standing roster and auto-on-duty

| | |
|---|---|
| **Goal** | Crown maintains **standing roster**; on **war bill** enactment, rostered members receive **auto-on-duty** at **Steadfast** with **hardened service** flag. |
| **Domain** | `StandingRosterService`: cap, appoint/remove, mobilise on enactment. |
| **Bukkit** | `/kingdom war roster` commands; nametag or scoreboard war duty indicator. |
| **Depends on** | Slice 3.2. |
| **Acceptance (domain)** | Tests: non-roster knight not auto-on-duty; sworn outsider never rostered. |
| **Spike vs flag** | **Feature** under `war.enabled`. |

### Slice 3.4 — Muster and muster deadline

| | |
|---|---|
| **Goal** | **Muster** call to **levy**; members answer or refuse; **ignored muster** after **muster deadline** → **Shaken** + **Doubtful** (dual-track). |
| **Domain** | `MusterService`: answer/refuse/deadline sweep; opens military morale track on answer. |
| **Bukkit** | Muster notification, `/kingdom war muster` respond; scheduler for deadline. |
| **Depends on** | Slice 3.3, loyalty domain from Slice 1.3 (political) — military track stubbed until Phase 4. |
| **Acceptance (domain)** | Tests: answer → Steadfast track opens; refuse → Shaken; deadline → ignored muster penalties. |
| **Spike vs flag** | **Feature** under `war.muster.enabled`. |

### Slice 3.5 — Peace bill and demobilisation (no territory change)

| | |
|---|---|
| **Goal** | **Peace bill** ends war: hostilities cease, **levy** **demobilisation**, **captured** chunk **revert** (no-op until Phase 6), no **region merge**. |
| **Domain** | `DemobilisationService`: close war, reset levy military tracks (stub), clear war flags. |
| **Bukkit** | Peace bill GUI; war status cleared on enactment. |
| **Depends on** | Slice 3.4. |
| **Acceptance (domain)** | Tests: peace without decisive victory → no annexation/tribute; standing roster persists. |
| **Acceptance (Bukkit)** | Manual: enact peace → muster closed, war info gone. |
| **Spike vs flag** | **Feature** under `war.enabled`. |

### Slice 3.6 — Battle phase PvP rules *(deferred)*

| | |
|---|---|
| **Goal** | *(Deferred under **open PvP**.)* Future belligerent damage matrix outside **siege**. |
| **Domain** | Extend `PvpConductEvaluator` when Slice 2.2 ships. |
| **Bukkit** | None for now. |
| **Depends on** | Slice 2.2, 3.2. |
| **Acceptance** | N/A until open PvP lifted. |
| **Spike vs flag** | **Deferred** — `war.battle-pvp.enabled`. |

---

## Phase 4 — Loyalty and morale tiers

Full **dual-track offence** behaviour; ties Phase 1 political track to military **morale tier** ladder.

### Slice 4.1 — Military morale track persistence

| | |
|---|---|
| **Goal** | **Fealty subject** military track: **Steadfast** → **Shaken** → **Breaking** → **Rout** with persistence across logout. |
| **Domain** | `MoraleService` mirroring political `LoyaltyService`; members, **sworn outsiders**, **civilian member** binding rules. |
| **Bukkit** | Persist alongside political tiers; `/kingdom loyalty` inspect (read-only). |
| **Depends on** | Slice 3.4 (muster opens track), Slice 1.3 (political). |
| **Acceptance (domain)** | Tests: oath of service opens tracks; civilian member hostile action in siege zone → Shaken (facts only, siege zone stub). |
| **Spike vs flag** | **Feature flag** `loyalty.military.enabled`. |

### Slice 4.2 — Morale breach and desertion offences

| | |
|---|---|
| **Goal** | **Morale breach** tier drops: refuse muster, leave **siege** without **siege release**, **fighting for the enemy** → **Rout** + treason review flag. |
| **Domain** | `DesertionEvaluator`; **dual-track offence** for **defection**; **hardened service** stricter siege absence rule. |
| **Bukkit** | Debuff application (potion effects per **morale penalty**); treason review routes to warrant pipeline. |
| **Depends on** | Slice 4.1, Phase 1 police. |
| **Acceptance (domain)** | Tests: each breach class drops correct tier; defection opens political treason review; Traitor only on conviction. |
| **Spike vs flag** | **Feature** under `loyalty.military.enabled`. |

### Slice 4.3 — Loyalty and morale recovery and pardon

| | |
|---|---|
| **Goal** | **Loyalty recovery** / **loyalty pardon** (crown); **morale recovery** in siege (honourable service); **morale pardon** (crown or knight at muster point). |
| **Domain** | Recovery timers by in-game day; pardon authority checks. |
| **Bukkit** | Crown commands at **court** or muster point; messages on tier restore. |
| **Depends on** | Slices 4.1–4.2, 1.3. |
| **Acceptance (domain)** | Tests: Traitor cannot time-recover; Rout requires morale pardon before next muster. |
| **Spike vs flag** | **Feature**. |

### Slice 4.4 — Loyalty penalties and office gates

| | |
|---|---|
| **Goal** | **Doubtful**/**Disloyal**/**Traitor** civil effects: appointment bars, Commons vote bars, constable scrutiny, warrant eligibility. |
| **Domain** | `LoyaltyGateService` integrated with Parliament seating and police eligibility. |
| **Bukkit** | Existing appointment/division flows consult gates; messages on denial. |
| **Depends on** | Slice 4.3, Parliament, police. |
| **Acceptance (domain)** | Tests: Disloyal cannot vote; Traitor arrestable with active warrant; no noble loyalty immunity. |
| **Spike vs flag** | **Feature**. |

### Slice 4.5 — Oath of service ceremony

| | |
|---|---|
| **Goal** | **Oath of service** at **court** lectern, throne checkpoint, or muster point binds **sworn outsider** or early member military obligation. |
| **Domain** | `OathService`: fealty subject registration, bounded purpose. |
| **Bukkit** | Interact ritual at configured blocks; chat vow + persistence. |
| **Depends on** | Slice 4.1. |
| **Acceptance (domain)** | Tests: sworn outsider Faithful + Steadfast on oath; no Commons seat. |
| **Acceptance (Bukkit)** | Manual: outsider swears → appears on levy eligible list. |
| **Spike vs flag** | **Feature flag** `war.oath.enabled`. |

---

### Slice 4.6 — Recovery clock persistence

| | |
|---|---|
| **Goal** | **Recovery clock** survives restart; today `recoveryMarks` is an in-memory `HashMap` in `LoyaltyService` and `MoraleService`, so a restart silently resets the wait. Invisible now, a reported bug the moment the **loyalty ledger** prints a countdown. |
| **Domain** | `LoyaltyStore` / `MoraleStore` gain `findMark` / `putMark` / `clearMark` (tier + in-game day). Services read and write through the store; the self-healing tier-mismatch restart rule is unchanged. |
| **Bukkit** | `YamlKingdomStore` persists marks to `data.yml` beside the tier. |
| **Depends on** | Slice 4.3. |
| **Acceptance (domain)** | Tests: mark survives a store round-trip; a fresh offence still restarts the clock; a mark for a tier the subject no longer holds is ignored. |
| **Spike vs flag** | **Refactor**, no flag. No player-visible change on its own. |

### Slice 4.7 — Service credit and acts of service

| | |
|---|---|
| **Goal** | **Service credit**: an **act of service** moves the **recovery clock**'s marked start day back a configured number of in-game days. Never grants a tier, never applies to **Traitor** or **Rout**. |
| **Domain** | `shortenRecovery(playerId, days)` on `LoyaltyService` and `MoraleService`: moves the mark's start day backwards; fails when the track is at top tier, closed, or non-recovering. Config `loyalty.service-credit.days` (default 1). |
| **Bukkit** | Two hooks only — income tax paid (`EconomyService.creditWallet`, wired in `KingdomPlugin`), and a **muster** served out to demobilisation (`MusterService.clearForWar`, domain-only until the war phase is wired). The day roll (`RealmCalendarTask`) now also ticks both recovery clocks, which nothing did before. |
| **Depends on** | Slice 4.6. |
| **Acceptance (domain)** | Tests: credit brings the next tick forward by exactly the configured days; credit past the threshold ticks at most one tier, never two; Traitor and Rout are unaffected; credit with no clock running is a no-op. |
| **Spike vs flag** | **Feature** under existing loyalty flags. |

### Slice 4.8 — Loyalty ledger GUI

| | |
|---|---|
| **Goal** | `/kingdom loyalty` opens the **loyalty ledger**: one item per track — tier, what lowered it, in-game days to the next tick, credit applied, and the next useful **act of service**. Read-only. |
| **Domain** | A ledger view record assembled from both services; tips computed from live state, never static lore (Traitor → petition the crown; closed military track → swear the **oath of service**; below Faithful → pay income tax). |
| **Bukkit** | GUI via `ItemBuilder`, title through `ColourEncoder.component()`. Members, self only; OP may pass a player name. `/kingdom info` keeps its one-line summary and points at the new command. |
| **Depends on** | Slices 4.6–4.7. |
| **Acceptance (domain)** | Tests: tip text per tier and per track state; countdown matches the persisted clock; no ledger path mutates a tier or a clock. |
| **Spike vs flag** | **Feature**. |

## Phase 5 — Army squads

**Rank-and-file** NPCs under player officers; officer **morale tier** drives **squad** behaviour.

### Slice 5.1 — Army caps and pressed villager conscription

| | |
|---|---|
| **Goal** | **Pressed villager** conscription from territory population; removed from villager economy while pressed; cap per kingdom. |
| **Domain** | `ConscriptionService`: select villagers, track pressed state, return on **demobilisation**. |
| **Bukkit** | Officer command to press/release; villager tags PDC. |
| **Depends on** | Phase 3 demobilisation, villager economy. |
| **Acceptance (domain)** | Tests: cap enforced; seated MP villagers ineligible; pressed excluded from GDP tick. |
| **Spike vs flag** | **Feature flag** `war.conscription.enabled`. |

### Slice 5.2 — Crown squad treasury purchase

| | |
|---|---|
| **Goal** | **Crown squad** spawned mobs funded from treasury per approved war spending; counts against army cap. |
| **Domain** | `CrownSquadService`: cost, cap, ledger entry. |
| **Bukkit** | Spawn vanilla mobs (zombie/skeleton/golem per config); kingdom command tag. |
| **Depends on** | Slice 5.1, Corona treasury. |
| **Acceptance (domain)** | Tests: insufficient treasury → reject; demob destroys crown squads. |
| **Spike vs flag** | **Feature flag** `war.crown-squads.enabled`. |

### Slice 5.3 — Squad assignment and officer command

| | |
|---|---|
| **Goal** | Officer assigns **squad** of **rank-and-file** to follow/attack; simple AI goals. |
| **Domain** | `SquadRegistry`: officer → squad list, state machine idle/follow/attack. |
| **Bukkit** | Pathfinding API goals; periodic tick on main thread with strict per-kingdom cap. |
| **Depends on** | Slices 5.1–5.2, officer must be **military participant**. |
| **Acceptance (domain)** | Tests: squad cap; officer unfit at Rout → **squad rout**. |
| **Acceptance (Bukkit)** | Manual: officer issues follow → mobs trail; attack → mobs target. |
| **Spike vs flag** | **Feature flag** `war.squads.enabled`. |

### Slice 5.4 — Squad behaviour by morale tier

| | |
|---|---|
| **Goal** | **Shaken** hesitation, **Breaking** scatter, **Rout** → **squad rout** (pressed flee home, crown squads lost). |
| **Domain** | `SquadMoralePolicy` reads officer **morale tier**. |
| **Bukkit** | AI goal swaps; pressed villager path toward territory centroid. |
| **Depends on** | Slice 5.3, Phase 4 morale. |
| **Acceptance (domain)** | Tests: tier transitions propagate to squad state on next tick. |
| **Acceptance (Bukkit)** | Manual: drop officer to Rout → squad disperses. |
| **Spike vs flag** | **Feature** under `war.squads.enabled`. |

### Slice 5.5 — Siege release command

| | |
|---|---|
| **Goal** | Officer or crown/knight grants **siege release** so departure is not **desertion**. |
| **Domain** | `SiegeReleaseGrant` with expiry; audited. |
| **Bukkit** | Field command; crown at muster point. |
| **Depends on** | Slice 4.2 (desertion rules), Phase 6 siege zone stub or full. |
| **Acceptance (domain)** | Tests: released departure → no breach; unreleased > hardened threshold → Breaking. |
| **Spike vs flag** | **Feature**. |

---

## Phase 6 — Siege, decisive victory, and outcomes

Requires **Slice 0.1 spike** success (or scripted-conquest fallback). Implements **chunk capture**, **occupation**, **decisive victory**, **region merge**, **war tribute** / **war debt**.

### Slice 6.1 — Siege zone and military participant presence

| | |
|---|---|
| **Goal** | **Siege** = combat in defender linked territory with capture active; **military participant** counting for presence; **civilian member** hostile action binds track. |
| **Domain** | `SiegeZoneResolver` from linked WG region; participant registry from war + muster + roster state. |
| **Bukkit** | Chunk enter/periodic presence tick; siege status in `/kingdom war`. |
| **Depends on** | Phase 3 war state, Phase 4 morale, Slice 0.1 findings. |
| **Acceptance (domain)** | Tests: participant only counts in contested chunk; civilian bind on first hostile fact. |
| **Spike vs flag** | **Feature flag** `war.siege.enabled`. |

### Slice 6.2 — Chunk capture and recapture

| | |
|---|---|
| **Goal** | **Chunk capture** flips **captured chunk** when attacker **military participants** outnumber defenders over tick window; defender **recapture** symmetric. |
| **Domain** | `ChunkCaptureService` (from spike); war tally per kingdom pair. |
| **Bukkit** | Presence sampler scheduler; chunk state cache; action bar progress (optional). |
| **Depends on** | Slice 6.1, Slice 0.1. |
| **Acceptance (domain)** | Tests: flip/recapture tally; debounce; equal presence → no flip. |
| **Acceptance (Bukkit)** | Manual: 3:1 attackers in chunk → flip; defenders return → recapture. |
| **Spike vs flag** | **Feature** under `war.siege.enabled`. |

### Slice 6.3 — Occupation rules in captured chunks

| | |
|---|---|
| **Goal** | **Occupation**: attacker build/PvP rights in **captured chunk**; defender civilians retain political rights, not military presence credit. |
| **Domain** | `OccupationPolicy` extends conduct enforcement with per-chunk control state. |
| **Bukkit** | Phase 2 listeners consult occupation; visual marker (particle or map colour optional). |
| **Depends on** | Slice 6.2, Phase 2 enforcement. |
| **Acceptance (domain)** | Tests: attacker may build when occupation allows; defender member breach on break. |
| **Spike vs flag** | **Feature**. |

### Slice 6.4 — Capital subregion and capital fall aim

| | |
|---|---|
| **Goal** | Monarch sets **capital** WG subregion; **capital fall** **war aim** (majority or total chunk capture in subregion). |
| **Domain** | `CapitalService`; war aim evaluator for capital vs **territory threshold**. |
| **Bukkit** | `/kingdom setcapital` region hook; war bill selects aim type. |
| **Depends on** | Slice 6.2, Slice 3.1. |
| **Acceptance (domain)** | Tests: capital fall satisfied on configured threshold; territory threshold separate. |
| **Spike vs flag** | **Feature**. |

### Slice 6.5 — Decisive victory detection

| | |
|---|---|
| **Goal** | **Decisive victory** when enacted **war aim** met — automatic war end without **peace bill**. |
| **Domain** | `VictoryEvaluator` on capture tally vs bill; triggers demobilisation + outcome dispatch. |
| **Bukkit** | Broadcast victory; lock siege actions. |
| **Depends on** | Slices 6.2–6.4, 3.5 demobilisation. |
| **Acceptance (domain)** | Tests: peace bill path still available before aim met; aim met → war closed. |
| **Spike vs flag** | **Feature**. |

### Slice 6.6 — Annexation and region merge

| | |
|---|---|
| **Goal** | **Annexation** outcome: **region merge** folds **captured** chunks into attacker linked territory after **decisive victory**. |
| **Domain** | `RegionMergeExecutor` interface; domain plans merge from capture set. |
| **Bukkit** | `WorldGuardBridge` apply merge; backup before edit; audit log. |
| **Depends on** | Slice 0.1 success, 6.5. |
| **Acceptance (domain)** | Tests: merge plan only includes captured chunks; peace path never merges. |
| **Acceptance (Bukkit)** | Manual: win annexation war → defender region shrinks on live server. |
| **Spike vs flag** | **Feature flag** `war.annexation.enabled` — **fallback** if spike failed: scripted handoff (monarch confirms merge GUI, no chunk creep). |

### Slice 6.7 — War tribute and war debt

| | |
|---|---|
| **Goal** | **War tribute** outcome transfers Corona on **decisive victory**; shortfall → **war debt** until paid. |
| **Domain** | `WarTributeService` integrated with treasury; debt persists in `economy.yml`. |
| **Bukkit** | Treasury debit/credit; `/kingdom info` shows debt. |
| **Depends on** | Slice 6.5, Corona economy. |
| **Acceptance (domain)** | Tests: partial treasury → remainder war debt; debt cleared on payment Act or OP credit. |
| **Spike vs flag** | **Feature**. |

### Slice 6.8 — Peace revert captured chunks

| | |
|---|---|
| **Goal** | **Peace bill** **revert** all **captured** chunks to defender home control; clear **occupation**; no **region merge**. |
| **Domain** | `RevertCapturedChunks` on peace enactment. |
| **Bukkit** | Clear capture cache; restore build/PvP to defender norms. |
| **Depends on** | Slice 6.2, 3.5. |
| **Acceptance (domain)** | Tests: revert idempotent; annexation war already ended cannot peace-revert. |
| **Spike vs flag** | **Feature**. |

### Slice 6.9 — Counter-war bill

| | |
|---|---|
| **Goal** | Defender tables new **war bill** (**counter-war**) to **siege** former attacker's homeland after prior war ended. |
| **Domain** | Validation: prior defender role, no active war. |
| **Bukkit** | Standard war bill flow with counter-war label. |
| **Depends on** | Slice 3.1, 6.5. |
| **Acceptance (domain)** | Tests: cannot counter while active; distinct from **recapture**. |
| **Spike vs flag** | **Feature**. |

---

## Phase 7 — Parliamentary politics

Six ideas from [`docs/idea-backlog.md`](idea-backlog.md), designed 2026-08-11. Independent of the war critical path — **may run in parallel with any phase**, since it touches only Parliament and election code.

Design decisions taken:

- **Motions ride `BillType`.** `NO_CONFIDENCE` and `REFERENDUM` are new `BillType` values on the single order paper. No second business slot. Consequence accepted: a referendum blocks legislation for its polling window; a no-confidence motion cannot be tabled while a budget is live.
- **Motions take no royal assent.** Division close branches on **motion** vs **bill**; motions decide in the Commons and never reach the Lords.
- **A carried no-confidence motion does not prorogue.** Session stays open, Premier election runs, no State Opening. (A dissolution-flavoured motion that *does* prorogue was considered and deferred.)
- **Referendums are advisory.** Binding referendums carrying a nested `BillPayload` were considered and deferred; the payload plumbing on `Bill` makes it a clean follow-up.
- **Manifestos and parties are free text.** No pledge-matching engine, no party registry.

### Slice 7.1 — Manifestos and parties on nomination

| | |
|---|---|
| **Goal** | Candidates declare a **manifesto** line, a **party** name, and a party colour when they nominate; all three surface in the election GUI and the **return of the Commons**. |
| **Domain** | Extend `ElectionState` nominations to carry `(manifesto, partyName, partyColour)`; validation caps manifesto ~60 chars and party name shorter; round-trip in `data.yml`. |
| **Bukkit** | Nomination command/GUI prompts for the three fields; election GUI renders manifesto beside each candidate; `CommonsReturnAnnouncer` reads party with the name. |
| **Depends on** | Existing election nomination flow. |
| **Acceptance (domain)** | Tests: nomination without manifesto is valid (blank); over-length manifesto rejected; fields survive YAML round-trip and by-election. |
| **Spike vs flag** | **Feature** — cosmetic, no vote-logic change. |

### Slice 7.2 — Division tally by party and profession bloc

| | |
|---|---|
| **Goal** | Division results group player MPs by **party** and villager MPs by **profession bloc**. |
| **Domain** | Tally function over `Bill.votes` joined to seat party/profession; returns grouped aye/nay/abstain counts. |
| **Bukkit** | `DivisionVoteGui` and the result broadcast render the grouping. |
| **Depends on** | Slice 7.1. |
| **Acceptance (domain)** | Tests: two players same party group together; villagers group by profession, never by party; partyless player MP groups as independent. |
| **Spike vs flag** | **Feature**. |

### Slice 7.3 — Hansard

| | |
|---|---|
| **Goal** | Every division and referendum result is recorded as it closes; at prorogation the session's record is written to the registrar as one **Hansard** volume. |
| **Domain** | `HansardRecord` per decided business (title, type, tally, in-game day); appended to `ParliamentState` and persisted on each close; book-page renderer that splits into volumes past the 100-page/256-char MC limit. |
| **Bukkit** | `RegistrarShelfWriter` places the volume(s) at prorogation. |
| **Depends on** | Slice 7.2 for the tally shape. |
| **Acceptance (domain)** | Tests: records survive restart mid-session; prorogue empties the live record; a 60-division session renders to ≥1 volume with no truncation. |
| **Spike vs flag** | **Feature**. |

### Slice 7.4 — Motion of no confidence

| | |
|---|---|
| **Goal** | A seated player MP tables a **motion of no confidence**; a **seconder** confirms; the House divides; carrying it removes the Premier and opens a Premier election. |
| **Domain** | `BillType.NO_CONFIDENCE`; motion-vs-bill branch at division close (no assent path); pending-second state modelled on `PendingResignation`; Premier and proposer excluded from seconding; **confidence cooldown** in in-game days (config, default 7) after a failed motion; villager Premier dismissal reuses `VillagerMpDespawnPolicy` release. |
| **Bukkit** | Table/second entries in `ParliamentHubGui`; broadcast on tabling, on seconding, and on result. |
| **Depends on** | Slice 7.3 (result must be recordable). |
| **Acceptance (domain)** | Tests: unavailable with one seated player MP; Premier may neither table nor second; carried motion clears the Premier rank and opens a `PREMIER` election without prorogation; villager Premier released on carry; second motion inside cooldown refused; motion never produces an `AssentedAct`. |
| **Spike vs flag** | **Feature**. |

### Slice 7.5 — Questions to the Premier

| | |
|---|---|
| **Goal** | The villager Speaker announces a **Questions to the Premier** window at intervals through an open session. |
| **Domain** | Interval check in the `ElectionTask` sweep: session open + Premier seated + N in-game days since last, else skip. |
| **Bukkit** | Broadcast from the Speaker; no queue, no command, no record. |
| **Depends on** | Villager Speaker sweep. |
| **Acceptance (domain)** | Tests: no announcement while prorogued or with no Premier; interval respected across restart. |
| **Spike vs flag** | **Feature** — ceremony only. |

### Slice 7.6 — Referendum

| | |
|---|---|
| **Goal** | The Premier or the Crown puts a question to every member; ballots accepted anywhere for a **polling window**; result and **turnout** proclaimed and recorded. |
| **Domain** | `BillType.REFERENDUM` with a question string; electorate = kingdom members, not seats; window in in-game days (config, default 2) reusing `divisionClosesOnMcDay`; early close by the Premier; no quorum; turnout computed against member count. |
| **Bukkit** | `/kingdom referendum` opens an aye/nay GUI shaped like `DivisionVoteGui`; login prompt while polling is open; result broadcast carries turnout. |
| **Depends on** | Slice 7.3, 7.4 (motion/bill branch already in place). |
| **Acceptance (domain)** | Tests: seated MPs get no extra weight; one ballot per member, re-vote replaces; window closes on in-game day or on early close; turnout counts members entitled, not online; referendum never produces an `AssentedAct`; occupies the order paper for its window. |
| **Spike vs flag** | **Feature**. |

---

## Slice count and sequencing notes

| Phase | Slices | Cumulative |
|-------|--------|------------|
| 0 — WG spike | 1 | 1 |
| 1 — Police hop 3 | 5 | 6 |
| 2 — Build/PvP | 3 | 9 |
| 3 — War state | 6 | 15 |
| 4 — Loyalty/morale | 5 | 20 |
| 5 — Squads | 5 | 25 |
| 6 — Siege/outcomes | 9 | **34** |
| 7 — Parliamentary politics | 6 | **40** |

**Parallelism:** Slice 0.1 should start as soon as Phase 2 build enforcement is far enough along to need realistic territory edits — do not wait for squads. Phase 4 Slice 4.1 can overlap late Phase 3 once muster exists. Phase 7 is off the war critical path entirely and may run at any time.

**Persistence:** Keep war/loyalty/capture state in plugin YAML with domain IDs (kingdom id, player uuid as strings in adapter layer only) per ADR Q6 portability.

**Resource pack:** Heraldry and war presentation are optional and can trail Phase 6; no slice blocks on pack work.

---

## Phase 8 — Church and rites

Ceremony layer per [`docs/adr/0008-a-priest-keeps-the-realms-rites.md`](adr/0008-a-priest-keeps-the-realms-rites.md). Entirely off in a kingdom with no **church**. No faith stat, no rival religions.

### Slice 8.1 — Church point, consecration, and the cleric

| | |
|---|---|
| **Goal** | Monarch sites a **church** inside linked territory; it is inert until **consecrated**; a **cleric** villager stands there whenever no player **priest** is sworn. |
| **Domain** | `Church` value object (location, consecrated flag) on kingdom state; `data.yml` round-trip; consecration invariants (new point unconsecrates). |
| **Bukkit** | `/kingdom church set|clear` (King/Queen, must be inside linked territory); cleric spawned fresh at the point, cleric profession, `[Cleric]` nametag, no profession label, reconciled on startup and by the 60s territory sweep; despawned when a priest is sworn. |
| **Depends on** | Capital/Lord Mayor pattern (`city`), villager Speaker spawn/reconcile pattern, `WorldGuardBridge` territory check. |
| **Acceptance (domain)** | Tests: set outside territory refused; clear removes point; re-set clears consecration; YAML round-trip. |
| **Acceptance (Bukkit)** | On test server: set church, cleric appears within one sweep, survives restart and chunk reload, disappears on `clear`. |
| **Spike vs flag** | **Feature** — no rites yet; the cleric is the tracer bullet. |

### Slice 8.2 — Priest sworn office

| | |
|---|---|
| **Goal** | One **priest** to a kingdom, sworn by the Crown, exclusive of police roles, suspended by prison. |
| **Domain** | Extend sworn-role model with `PRIEST`; XOR against `CONSTABLE`/`JUDGE`; suspend-on-sentence and restore-on-release reuse the police path. |
| **Bukkit** | `/kingdom church swear|unswear <player>`; bold `[Priest]` prefix in chat, tab and nametag; listed in `/kingdom info`; cleric despawns while the seat is filled and returns when vacated or the priest is gaoled. |
| **Depends on** | 8.1; existing sworn-role and prison suspension machinery in `police`. |
| **Acceptance (domain)** | Tests: swearing a constable as priest refused; second priest refused; sentence suspends and release restores; not resignable via `/resign`. |
| **Acceptance (Bukkit)** | Swear, see prefix; gaol the priest, cleric returns; release, priest restored and cleric goes. |
| **Spike vs flag** | **Feature**. |

### Slice 8.3 — Consecration and blessing rites

| | |
|---|---|
| **Goal** | The first two **rite**s: consecrating the church, and the free **blessing**. |
| **Domain** | `Rite` dispatch with the standing preconditions (church set, consecrated, celebrant present at the point, subject a member); per-player blessing cooldown of one in-game day. |
| **Bukkit** | Priest/cleric interaction at the church point performs consecration; priest right-clicks a member for Regeneration II + Resistance I (~2 min, config); broadcast lines in British spelling. |
| **Depends on** | 8.1, 8.2. |
| **Acceptance (domain)** | Tests: rite at unconsecrated church refused; blessing inside cooldown refused; non-member refused. The cooldown is memory-only, as open police cases are. |
| **Acceptance (Bukkit)** | Consecrate, bless, observe effects and the refusal message on a second blessing the same day. |
| **Spike vs flag** | **Feature**. |

### Slice 8.4 — Marriage, divorce, and annulment

| | |
|---|---|
| **Goal** | Two members wed at the church; spouses share a respawn point and may teleport to each other; the bond can be undone. |
| **Domain** | `Marriage` record (pair, date) in `data.yml`; one spouse each; same-kingdom invariant; divorce needs both, annulment needs the Crown. |
| **Bukkit** | Consent prompt at the church for both parties; `/tp <spouse>` routes through the existing teleport gate (prison bars it as it bars other teleports); shared respawn honoured on death. |
| **Depends on** | 8.3; existing `/tp` and checkpoint machinery. |
| **Acceptance (domain)** | Tests: second marriage refused; cross-kingdom refused; divorce without both refused; annulment by Crown succeeds; `/kingdom move` or leaving the kingdom ends the bond. |
| **Acceptance (Bukkit)** | Marry two accounts, die and respawn at the shared point, `/tp` to spouse, divorce, both perks gone. |
| **Spike vs flag** | **Feature**. |

### Slice 8.5 — Player funeral and held experience

| | |
|---|---|
| **Goal** | Death inside territory holds the dropped experience; the **funeral** returns half of it. |
| **Domain** | `FuneralRecord` (player, XP, expiry three in-game days); one record to a player, overwritten by a later death; expiry swept on the daily tick. |
| **Bukkit** | Death listener captures dropped XP inside linked territory only; rite requires the deceased present at the church; return share configurable (default 50%). |
| **Depends on** | 8.3; calendar day tick. |
| **Acceptance (domain)** | Tests: second death overwrites, does not stack; expired record returns nothing; death outside territory records nothing; share rounds down. |
| **Acceptance (Bukkit)** | Die in territory, hold the rite, receive half back; wait out the window, rite refused. |
| **Spike vs flag** | **Feature**. |

### Slice 8.6 — Villager funeral and the tithe

| | |
|---|---|
| **Goal** | A dead **productive villager**'s wallet waits **awaiting rites**; a funeral pays the treasury less a **tithe** to the priest; silence lets it escheat whole. |
| **Domain** | New `awaiting rites` wallet state beside freeze/escheat; tithe rate on config; cleric-presided tithe goes to the treasury. |
| **Bukkit** | Villager death listener marks the wallet; rite performed at the church names the deceased villager; escheat processor honours the window. |
| **Depends on** | 8.5; villager wallet freeze/escheat processor in `economy`. |
| **Acceptance (domain)** | Tests: funeral inside window splits correctly; outside window escheats whole; cleric funeral sends the tithe to the treasury; MPs and Treasury Lords follow the same path. |
| **Acceptance (Bukkit)** | Kill a productive villager, hold the rite, see treasury and priest wallet move by the right amounts. |
| **Spike vs flag** | **Feature**. |

### Slice 8.7 — Coronation and the coronation gate

| | |
|---|---|
| **Goal** | An uncrowned monarch cannot exercise ceremonial powers in a kingdom that has a consecrated church. |
| **Domain** | `crowned` flag per kingdom monarch; gate checked on royal assent, **honours**, role swearing and title granting; gate inert where no consecrated church exists; flag cleared when the monarch changes. |
| **Bukkit** | Coronation rite at the church with the rightful holder present; refusal messages name the gate; `/kingdom info` shows uncrowned status. |
| **Depends on** | 8.3; Parliament assent path, honours, sworn-role and title commands. |
| **Acceptance (domain)** | Tests: no church → gate inert; consecrated church + uncrowned → assent refused, permits still allowed; crowning a non-monarch refused; new monarch is uncrowned. |
| **Acceptance (Bukkit)** | Set and consecrate a church, try assent uncrowned (refused), crown, assent succeeds. |
| **Spike vs flag** | **Feature**. |

---

## Not in scope yet

The following are explicitly deferred per ADR, evaluation doc, or `CONTEXT.md`:

| Item | Reason |
|------|--------|
| **Coalitions** and multi-target wars | War bill authorises one defender only until a later Act type |
| Fabric/NeoForge **mod path** | Q1 vanilla client; see ADR escape hatches |
| **Client mod** / morale HUD / battle map UI | Plugin ceiling; optional server resource pack only |
| Formation combat, siege engines, breach physics | Abstracted per ADR; not plugin-feasible at fidelity |
| Supply lines, war exhaustion, diplomatic treaties | Not in glossary or ADR phase 1–7 |
| **Faith** stat, rival religions, conversion, worship services | Rejected in ADR 0008; religion is rites only |
| Legitimacy and succession crisis mechanics | Titles stay operator-assigned; **coronation** gates powers only |
| Trade **embargo** as separate from fiscal Acts | Corona + Acts partially cover; dedicated embargo TBD |
| Espionage and spy networks | Social/play-led; no mechanical slice planned |
| Anti-AFK for life events or muster | User preference: no AFK detection |
| Network / Velocity shared DB | Q6 single server now; schema portability only |
| Cheat-proof prison | Accepted leaky confinement per Q5 |
| Noble **loyalty immunity** | Explicitly excluded in glossary |
| Automatic royal assent or timed assent | Monarch must be present in **House of Lords** |
| Player **MP** assignment by crown | MPs come from elections only |

---

## References

- [`docs/adr/0001-plugin-first-society-stack.md`](adr/0001-plugin-first-society-stack.md) — decision, build order, escape hatches  
- [`docs/plugin-vs-mod-evaluation.md`](plugin-vs-mod-evaluation.md) — capability matrix, Q1–Q6 answers  
- `CONTEXT.md` `## War`, `## Police`, `## Parliament`, `## Church` — authoritative glossary  
- `AGENTS.md` — deploy path, existing built domains, police hops 1–2 status  
