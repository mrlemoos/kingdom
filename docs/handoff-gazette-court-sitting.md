# Handoff — Gazette, Court, and the Sitting Calendar

Design agreed with the user in a grilling session on 2026-08-12. Every decision below was
put to the user and confirmed; treat them as settled, not as suggestions. Implement in the
build order at the end.

Read `AGENTS.md` and `CONTEXT.md` first. British spelling in all user-facing messages. TDD
for domain logic. Tracer-bullet slices, minimal diff, match existing conventions. Do not
commit or push unless asked.

---

## Working-tree state you are inheriting

`main` has **uncommitted** work from two earlier efforts. Leave both alone; do not revert or
commit them.

1. Parliament / `RoyalStandard` / `KingdomFlag` work in progress (pre-existing).
2. A **player feedback layer** shipped this session (`mvn test` green, 1193 tests):
   - `feedback/RealmFeedback.java` — the single static helper for sounds, particles, titles,
     action bars, kingdom-scoped messages. Short-circuits when `Bukkit.getServer() == null`
     so domain services can call it without breaking unit tests. **Reuse this helper for all
     new feedback below — do not add a second one.**
   - `feedback/DivisionBarText.java` — pure: bar legend + window-remaining fraction.
   - `feedback/DailyRealmReport.java` — pure: record + `line()` + `delta()`.
   - `feedback/DivisionBossBarService.java` — one `BossBar` per kingdom, rebuilt each
     `ElectionTask` sweep (60s cadence).

Known gap from that work: there is **no per-player tax**, so "tax paid" has no personal-event
hook. The daily realm broadcast covers it at kingdom level.

---

## Slice A — Town Crier and the Gazette

### The Crier

New `city/TownCrierService`, modelled directly on `city/LordMayorService`.

- A **nitwit** villager, spawned fresh at the capital. Never claims a territory villager.
- PDC-tagged, invulnerable, no AI, persistent.
- Nametag `Town Crier`, **no profession label** — same treatment the villager Speaker gets.
- Outside the economy entirely: no wallet, no GDP, no trades, no strike. Not eligible for
  elections, not warrantable, not arrestable.
- No capital → no Crier → no Gazette. Same rule as build permits.
- Reconciled by the existing 60s territory sweep and on startup sync.

### The crying

One `TextDisplay` entity above the Crier, billboard `CENTER`.

- Cycles the newest 5 Crown-authored posts, one every 4 seconds.
- The ticker task only ticks when a player is within 24 blocks. No idle CPU at an empty capital.
- No posts → `Hear ye! No news today.` Never hide the display; a blank Crier reads as broken.
- The display despawns with the Crier.

Rejected: rewriting the villager's custom name (one short line, fights the nametag), and
chat messages to nearby players (spam, not "above head").

### The Gazette GUI

Right-click the Crier. Paginated, following the `city/gui/PermitRegisterGui` +
`PermitRegisterLayout` pattern.

Contents:
- **Authored posts** — persisted (see below).
- **Live realm state, read at open time and never stored**: open bills, next election date,
  wanted list, permit count, treasury.

Deliberately *not* built: an event log of auto-generated headlines (assent, election results,
arrests). That needs retention policy and unbounded storage growth. Live state is computed on
open instead.

### Composing

The monarch right-clicks the Crier while holding a **signed book**.

- GUI offers *Decree / Announcement / Cancel*.
- For a decree it additionally offers a curfew choice: *Dusk–Dawn (13000–23000)* /
  *Nightfall–Midnight (13000–18000)* / *Lift curfew* / *No curfew*. Presets only — no tick
  numbers typed anywhere.
- Book **title** = headline, **pages** = body. The book is consumed as the physical instrument,
  mirroring the resignation-letter and Speech-from-the-Throne paper pattern.

Rejected input methods: anvil rename (50 chars, one line), `Player#openSign` (4 short lines),
chat prompt (hijacks chat, needs listener + timeout).

### Announcement vs decree

| | Announcement | Decree |
|---|---|---|
| Styling | grey | bold gold, `DECREE` header |
| Retention | capped at 20, oldest dropped | permanent, exempt from the cap |
| Hansard | no | yes, via `HansardArchivist` |
| Broadcast | none | once to online members, with the royal-assent sound |
| Mechanics | none | may carry a curfew (below) |

### The curfew decree

**Fact to know before starting:** `police/CurfewEvaluator.java` and
`police/CurfewEnforcementConfig.java` exist but **nothing references them**. There is no
listener and no enforcement path. The config is per-server, not per-kingdom. This slice is the
first thing that gives them a job, and the enforcement path is new code.

- A decree writes its curfew window **per kingdom** into `data.yml`.
- `enforcement.curfew.*` in the plugin config becomes the fallback default when no decree is in
  force. A *Lift curfew* decree clears it.
- Reuse `CurfewEvaluator` as-is; feed it the per-kingdom window. Its `isInsideWindow`
  midnight-wrap branch is untested in practice — cover it.
- **Breach files a warrant application** on the offender, which flows into the existing Crown
  approval → arrest → trial pipeline. No new enforcement primitive.
- Scope: inside linked territory only, members **and** visitors, matching jurisdiction.
- King/Queen/Prince immune (existing immunity rule covers this — no special case needed).
  **OP is not exempt**, matching the build-permit gate rather than `BuildConductEnforcer`.
- Throttled so one night does not produce ten warrants.

Rejected: refusing movement or teleporting offenders home (new movement-blocking surface,
fights the open-PvP stance), and warning-plus-loyalty-drop only (toothless, decree feels fake).

Keep the decree→effect binding **narrow and explicit**. Curfew is the only mechanical decree.
Do not build a generic decree-effect framework; every future decree type will want one and
that is a decision for whoever adds the second type.

---

## Slice B — The court

### Anchor: no block at all

`model/police/CourtLocation` is already just `(worldName, x, y, z)` — the lectern was only ever
a command-time requirement, so this is a swap, not a migration.

- `/kingdom police court set` records the **monarch's standing position**, which must be inside
  linked territory.
- `/kingdom police court clear` removes the court, its judge, and its golems.
- **One verb only.** With no block there is no difference between setting and moving, so do not
  add a separate `court move`.
- Delete `findLecternBlock` / `isLecternInTerritory` from the court path in
  `command/KingdomPoliceHandler.java` (around line 282). **Mint and parliament keep theirs.**

Reason for dropping the lectern: lecterns claim nearby villagers as librarians. `MintLecternGuard`
exists solely to undo that damage for mints — the court should not repeat the mistake. No
profession-claiming block is acceptable here (lectern, cartography table, barrel, smoker, blast
furnace, brewing stand, composter, fletching table, grindstone, loom, smithing table,
stonecutter, cauldron).

### Moving the court

Currently a move leaves orphan NPCs at the old site. On a move:

- Despawn the villager judge at the old position via the existing `removeJudgesAtCourt`.
- `ensureJudge` at the new position.
- **Relocate guard golems** posted at the court, or they stand guarding bare ground.
- Trials in flight fall back to the villager judge, as they do today.

### The trial as spectacle

- On arrest, the **accused and the judge (or the 3 jurors) are summoned to the court**. Reuse the
  ceremony summon mechanic. Pre-trial summon is clean: the teleport bar on prisoners only applies
  after a prison sentence.
- The ballot / verdict GUI opens **only within ~8 blocks** of the court position.
- A juror who strays ≥8 blocks **forfeits**, counted as an abstention.
- If the player judge leaves, the case falls to the villager judge (existing fallback).
- **Window: 120 real seconds**, config `police.trial.window-seconds`.
- Boss bar counts the window down on **its own 1-second task**, running only while at least one
  trial is open. Reuse `DivisionBossBarService`'s class shape but **not** its 60s sweep cadence —
  divisions run in in-game days, trials in real seconds, and 60s granularity makes the bar stutter.
- On lapse: decide on ballots cast so far; all-abstain falls to the villager judge.
- Verdict fires sound, particles, and a realm broadcast via `RealmFeedback`. Guilty and acquittal
  must read differently.

### The all-villager court

When fewer than 3 eligible players are online, seat **villager jurors** so a trial runs
end-to-end with zero players present — the same pattern as the villager Speaker and villager MPs.

- Jurors are **claimed** from territory villagers and released to stored origin when the trial
  ends, exactly like villager MPs. A jury of peers should be actual residents, and the
  claim/release machinery already exists. (Spawning fresh was rejected: the Speaker is spawned
  because it is an *office*; a jury is the opposite.)
- Claiming wakes sleepers and resets lying pose, as Commons seating already does.
- **Excluded from selection**: the accused (when a villager), seated MPs and the Premier, the
  villager Speaker, Treasury Lords, the villager judge itself, the Town Crier, and striking
  villagers.
- Fewer than 3 eligible villagers → villager judge alone, as today.
- Verdicts still come from the weighted `RealmHandledSentenceTable`; the point is that the court
  *looks* like a court — bodies in seats, sound, boss bar.

---

## Slice C — The sitting calendar

Villager MPs alternate: profession work one realm day, Parliament the next.

`calendar/RealmCalendar` provides a monotonic `realmDay`, so the parity check is one modulo.

- **Villager MPs only.** Player MPs go where they like — compelling a human means teleporting
  players on a timer, which turns the feature into a chore. The **Premier villager and villager
  Speaker stay at Parliament full-time**: they hold offices, not constituency jobs, and a Speaker
  who presides on alternate days breaks divisions.
- **Sitting day = even `realmDay`; recess = odd.** No config key — add one when somebody actually
  wants a different rhythm.
- **On recess**: release the villager to its stored origin with normal AI and despawn behaviour
  restored, using the existing release path. It trades with players normally. Re-claim and re-seat
  on the next sitting day. Do **not** invent a half-claimed "at the job site but still claimed"
  state — that is a new mode every sweep, strike check, and nametag reconcile would have to know
  about.
- The `[MP]` nametag is **kept on recess days** — they are still an MP, just not sitting. Keep
  despawn protection while they are in territory.
- **Re-claim failure**: re-claim by UUID; if the villager is gone, substitute the next eligible
  territory villager — same profession first, then any eligible — and announce it in chat
  (`The member for … has been replaced`). Dead villager → the substitution is permanent. Merely
  absent → it returns to the roll next sitting day. No eligible villager at all → the seat is
  empty for that sitting day and the division tally counts only seated members, as it already does
  when professions run short. Do **not** trigger a by-election for a dead MP.

### Divisions and recess — this fixes a live bug

The villager Speaker currently opens a division as soon as a bill is tabled, and closes it at once
when no player MPs are seated. On a recess day with the villager MPs away, that combination decides
the bill on the Speaker's tie-breaking **nay** with nobody present — a silent bill graveyard.

- **Divisions open only on sitting days.** A bill tabled in recess sits on the order paper; the
  Speaker opens the division at the start of the next sitting day. Westminster-correct: the House
  does not divide in recess.
- Guard the "close at once when no player MPs are seated" rule so it fires **only on a sitting day
  with no villager MPs seated either**.
- **Prorogation overrides everything**: while Parliament is prorogued (after a general election,
  before State Opening) there are no sitting days at all, so villager MPs work their professions
  every day and are re-claimed and seated at State Opening. Pleasant side effect: after an election
  the realm's villagers visibly go back to work until the Speech from the Throne.

---

## Persistence

New persisted state in `data.yml`, via `storage/YamlKingdomStore`:

- Gazette posts (title, body, author, in-game date, `decree` | `announcement`) — announcements
  capped at 20, decrees permanent.
- The per-kingdom curfew window.
- The Town Crier's location.

**Trials stay memory-only**, as they are today. A 120-second window means a restart mid-trial just
drops the case back to no-warrant. Serialising in-flight ballots for a two-minute window is a lot
of YAML for a rare crash.

---

## Build order

Sequential — 3 through 6 all touch police or parliament, so do not parallelise them.

1. Crier + Gazette + announcements
2. Decree + Hansard + curfew enforcement
3. Court: no-block anchor, `set` / `clear`, golem relocation
4. Trial spectacle: summon, proximity gate, boss bar, verdict effects
5. Villager jury
6. Sitting calendar

Slice A is materially bigger than slice B — steps 1 and 2 are the long ones.

---

## Conventions to honour

- TDD for domain logic: parity/sitting-day rules, post capping and retention, curfew window
  membership, juror eligibility, substitution selection, ballot tallying with abstentions. Write
  the failing test first. Pure Bukkit glue (spawning, sounds, bar rendering) needs no test.
- `mvn test` green before finishing. `mvn package` only if the user intends to deploy.
- Run `ReadLints` on every touched Java file. Eclipse JDT null analysis applies: avoid
  `Optional.map` with method references on `@Nonnull` receivers; guard with
  `isPresent()` / `isEmpty()`.
- Custom items and GUI stacks go through `helpers/ItemBuilder`; GUI titles through
  `ColourEncoder.component()`, not deprecated `ItemMeta` string APIs.
- Commands register through Incendo Cloud v2.
- Add the new domain terms to `CONTEXT.md` — glossary entries only, no implementation detail.
  New terms: Town Crier, Gazette, Decree, Announcement, Curfew, Sitting day, Recess, Villager jury.
- Consider an ADR for the decree mechanism, following `docs/adr/0003`.

---

## Known issues deliberately left out of scope

- Mint and parliament still use lecterns and still need `MintLecternGuard`.
- A player judge cannot decline or hand off a case; the judge is picked at random from online judges.
- A restart drops open cases silently, with no message to the accused.
