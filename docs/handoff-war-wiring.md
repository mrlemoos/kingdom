# Handoff — Wiring the war stack

Scoped on 2026-09-10 after an audit of unreachable powers. **Read this caveat first:** unlike
`docs/handoff-gazette-court-sitting.md`, the decisions below were *recommended* to the user and not
individually confirmed. The audit findings are verified fact; the build order is a recommendation.
Confirm the target with the user before starting — see [Open decision](#open-decision).

Read `AGENTS.md` and `CONTEXT.md` (`## War`) first. British spelling in all user-facing messages.
TDD for domain logic. Tracer-bullet slices, minimal diff, match existing conventions. Do not commit
or push unless asked.

---

## Working-tree state you are inheriting

`main` has **uncommitted** work. Leave all of it alone; do not revert or commit it.

Pre-existing, from before this session:

1. Corona merchant work in progress — `economy/villager/merchant/CoronaMerchantPayment`,
   `CoronaMerchantRecipeService`, `listener/CoronaMerchantListener` and their two tests.
2. An edit to `docs/idea-backlog.md`.

Shipped this session across three agents (`mvn test` green, **1782 tests**, 0 failures, 1 skipped,
up from a 1721 baseline):

3. **`model/RankAuthority.java`** — the single seam for "may this rank do this?". Six duplicated
   `isCrown` implementations (`city/CapitalSitingPolicy`, `calendar/RealmCalendarService`,
   `war/roster/StandingRosterAuthority`, `police/PoliceAuthority`, `mint/RoyalMintPlacementPolicy`,
   `mint/TreasuryLordManagementPolicy`) now delegate to it with their signatures unchanged.
   **Route every new rank gate through this class. Do not compare a `NobleRank` inline.**
   Powers delegated below the Crown: Duke → build permits + Gazette; Count → build permits;
   Lord → mints; Knight → police golem deploy + morale pardon.
4. **`hub/`** — the Realm Hub. `/kingdom` with no arguments opens a rank-filtered GUI for a player
   (console keeps the old text help). `hub/RealmHubView.entries(snapshot)` is the content-selection
   logic and is Bukkit-free and unit-tested; `hub/gui/RealmHubGui` renders it; `hub/RealmHubSnapshotFactory`
   reads the live realm. Powers the viewer *may not* use are shown greyed with a refusal naming who
   may — that is the discoverability mechanism. **Anything new that a player can do should get a hub
   topic**, or it will be invisible in exactly the way this session was convened to fix.
5. **`loyalty/MoralePardonRoll` + `loyalty/gui/MoralePardonGui` + `listener/MoralePardonListener`** —
   right-click the court's villager judge to grant a morale pardon. This wired up
   `MoraleService.pardon`, which had zero callers.
6. **`command/UnknownOrderRefusal` + `cloud/KingdomCloudExceptionHandlers`** — a mistyped `/kingdom`
   order now gets an in-world refusal plus a pointer to the hub, through both Cloud's syntax handler
   and `KingdomCommand.execute`.
7. `city/gui/GazetteLiveStateReader` — extracted out of `TownCrierGuiListener` so the Crier and the
   hub share one reader. **Reuse it for any new live-realm read; do not add a second.**

---

## The finding

`war/` is 95 files of domain code with thorough unit tests and **almost no wiring into
`KingdomPlugin`**. `docs/build-order.md` specs the whole thing across Phases 3–6, and every slice
there has a **Domain** row and a **Bukkit** row. The Domain rows are done. The Bukkit rows were
never done.

Constructed in `KingdomPlugin` today:

```
WarService · StandingRosterService · OathService · LevyUpkeepService
FieldMoraleDecayService · MilitaryParticipantRegistry · DemobilisationService
```

Never constructed anywhere in `src/main`:

```
war/muster · war/squad · war/crownsquad · war/conscription · war/siege (release)
war/tribute · war/victory · war/capture · war/annexation · war/capital · war/occupation
war/desertion
```

### Stranded powers — verified zero callers in `src/main`

Ranked by how much player-facing capability is stranded. All confirmed including method references.

| # | Power | Consequence |
|---|---|---|
| 1 | `war/roster/StandingRosterService.appoint` / `remove` | Service *is* live and feeds `LevyUpkeepService`. No hand can add a name, so the levy wage bill runs over a permanently empty roster. |
| 2 | `war/oath/OathService.swearAsMember` / `swearAsOutsider` | The only intended way a player *opens* a military morale track. Nobody can swear, so the morale ladder — and the pardon wired in item 5 above — is reachable only through decay paths. |
| 3 | `service/ParliamentService.tableWar` / `tablePeace` | The realm cannot enter or leave war through Parliament. |
| 4 | `war/muster/MusterService.openMuster` / `answer` / `refuse` | Whole service never constructed. Transitively kills `MoraleService.recordServiceCredit`, reachable only from `MusterService.creditServedMuster`. |
| 5 | `war/victory/VictoryEvaluator.evaluateAndApply` | No war can end by victory. |
| 6 | `war/squad/SquadService.assign` | Officers cannot assign squad members. |
| 7 | `war/tribute/WarTributeService.payDebt` | A defeated realm cannot pay down war debt. |
| 8 | `war/siege/SiegeReleaseService.evaluateDeparture` | Honourable release vs desertion is never decided. |
| 9 | `war/conscription/ConscriptionService.pressedView` | Villagers cannot be pressed into the levy. |
| 10 | `war/occupation/OccupationPolicy.evaluateBuild` | `BuildConductEnforcer` never consults it, so occupiers build freely in captured land. |
| 11 | `war/WarService.validateCounterWarBill` | `WarService` is live, so this is a real guard that simply never runs. |
| 12 | `loyalty/MoraleService.recordSiegeHostileAction` | Its two siblings (`recordUnpaidLevy`, `recordFieldAttrition`) are wired; this one is not. |
| 13 | `police/PoliceTrialService.applyVillagerPrison` | A villager conviction cannot reach a cell. |

Lower severity — dead *duplicates*, not stranded powers, and safe to delete if touched:
`police/TrialJuryService.expireDueSessions` (the scheduled sweep calls per-session
`expireIfTimedOut` instead) and `granary/GranarySiting.canSite` (the handler uses
`evaluateLink` / `evaluateClear`).

---

## Open decision

Put this to the user before writing code.

- **Minimum playable loop** (Slices 1–5 below) — Phase 3 plus the oath from Slice 4.5.
- **Full Phases 3–6** — adds squads, conscription, siege, chunk capture, annexation, tribute,
  victory. Roughly four times the work.

**Recommended: the minimum loop, and Slices 1 and 2 first.** Roster and oath are pure Bukkit
wiring onto persistence that already works, they need no design decisions, and the oath unblocks
the morale pardon that currently has almost nothing to pardon. Phase 6 is the risky half —
`docs/adr/0001` gave chunk capture and WorldGuard region merge their own escape hatch — and should
not be touched until the loop is playable.

Everything ships behind the existing master flag `war.enabled`, **default `false`** in
`src/main/resources/config.yml:275`, so a partial loop is safe to land.

---

## Slice 1 — The standing roster

`StandingRosterService` is constructed, its store is persisted, and its authority seam exists. Only
the player-facing door is missing.

- `WarResult appoint(String kingdomId, NobleRank actorRank, UUID playerId)` and `remove(...)` with
  the same shape. Both already refuse a non-member and enforce `config.rosterCap()`.
- Persistence is **already working**: `data.yml` sections `standing-roster` and `on-duty`, read and
  written by `storage/YamlKingdomStore` (see `setStandingRosterStore`). Nothing to add.
- `StandingRosterAuthority.isCrown` now delegates to `RankAuthority.canMaintainStandingRoster`.
- In-world first, per the user's standing principle: prefer a right-click GUI on an existing NPC
  over a new subcommand. The Lord Mayor at the capital already carries the permit register and the
  State of the Realm item and is the natural muster office. `docs/build-order.md` Slice 3.3 names
  `/kingdom war roster` commands; treat that as the fallback, not the first choice.
- Add a hub topic showing roster size against cap, and whether the viewer is rostered.

## Slice 2 — The oath of service

- `OathService` is constructed in `KingdomPlugin` (around line 214) and already read by
  `RealmSidebarService`. `swearAsMember(UUID)` and `swearAsOutsider(...)` need a door.
- Swearing calls `moraleService.oathOfService(playerId)`, which is what opens the military morale
  track. **This is the keystone of Phase 4** — without it, morale, desertion, service credit and the
  pardon are all unreachable by design rather than by accident.
- `docs/build-order.md` Slice 4.5 calls it a ceremony. The church and its cleric already run the
  realm's rites (`church/`, `listener/ClericGuiListener`), and coronation already goes through
  `CoronationGui`. Swearing an oath at the church is the obvious fit and needs no new NPC.
- Sworn outsiders are stored in `InMemorySwornOutsiderStore`. **Check whether it is persisted**
  before shipping — an outsider who loses their oath on restart is a bug, not a feature.
- Add a hub topic: whether you have sworn, your morale tier, and where to swear.

## Slice 3 — War and peace bills

- `ParliamentService.tableWar(kingdomId, rank, proposerId, targetKingdomId, aim, outcome,
  musterDeadlineMcDays, optionalTitle)` and `tablePeace(kingdomId, rank, proposerId, optionalTitle)`.
  `BillType.WAR` and `BillType.PEACE` already exist.
- Both gate on King/Queen with an **inline `NobleRank` comparison**. Fold them onto `RankAuthority`
  while you are in there.
- `tablePeace` already refuses when war is disabled or the kingdom is not at war.
- The war bill takes six parameters including two enums and a deadline — this needs a GUI branch off
  `parliament/gui/ParliamentHubGui`, not a command with six positional arguments. Follow
  `PublicWorkPrepareGui` and `MintPrepareGui`, which solve the same shape.
- `docs/build-order.md` Slices 3.1 and 3.2: enactment creates `ActiveWar`, both belligerents are at
  war, declaration is broadcast, and the Registrar records the enacted war Act.

## Slice 4 — Muster

**This slice carries the one real design decision in the loop.**

`MusterService` holds all its state in plain in-memory maps — `eligibleByWar`, `answersByWar`,
`levyMoraleByPlayer` — with **no store interface at all**, unlike every other war service. A muster
deadline runs across in-game days, so a restart mid-muster silently loses every answer and every
player who ignored the call gets the ignored-muster morale penalty unfairly.

Decide with the user before wiring:

- **(a) Add a `MusterStore` interface + `InMemory` impl + `YamlKingdomStore` section**, matching the
  pattern every sibling already follows. Correct, and the shape is well-trodden.
- **(b) Ship it memory-only and accept that a restart cancels the muster**, refunding nobody a
  penalty. Cheaper, and defensible if musters are short.

Recommend **(a)** — the deadline outliving a restart is the whole point of a deadline, and the
pattern is copy-paste from `InMemoryStandingRosterStore`.

Then: `openMuster` on war enactment, `answer` / `refuse` by members, deadline sweep on the existing
`ElectionTask` cadence. `docs/build-order.md` Slice 3.4: an ignored muster past the deadline drops
the member to **Shaken** and **Doubtful** on the dual track.

## Slice 5 — Peace and demobilisation

`DemobilisationService` is already constructed. Once Slice 3 lands `tablePeace`, wire enactment
through to it. Per Slice 3.5, peace without decisive victory carries **no** annexation and **no**
tribute — captured chunks reverting is a Phase 6 no-op today, which is correct for this loop.

---

## Conventions to honour

- TDD for domain logic: muster answer/refuse/deadline rules, roster cap and membership refusal, oath
  tier transitions, war/peace bill validation. Write the failing test first. Pure Bukkit glue
  (spawning, sounds, GUI rendering) needs no test.
- `mvn test` green before finishing. `mvn package` only if the user intends to deploy — and confirm
  the live server version first; `deploy/plugins-26.1.2/` is a legacy folder name, not a target.
- Run `ReadLints` on every touched Java file if the tool is available. Eclipse JDT null analysis
  applies: avoid `Optional.map` with method references on `@Nonnull` receivers; guard with
  `isPresent()` / `isEmpty()`.
- Every rank gate through `RankAuthority`. Every live-realm read through `GazetteLiveStateReader`.
  Every new player-facing power gets a `RealmHubTopic`.
- Custom items and GUI stacks through `helpers/ItemBuilder`; GUI titles through
  `ColourEncoder.component()`, not deprecated `ItemMeta` string APIs.
- Commands register through Incendo Cloud v2. Prefer an in-world interaction to a new subcommand.
- Add new domain terms to `CONTEXT.md` `## War` — glossary entries only, no implementation detail.

---

## Known issues deliberately left out of scope

- A Duke may `/kingdom permit grant` but cannot open the permit register GUI, which is still
  Crown-or-Prince because it also carries the State of the Realm statistics item. Inconsistent;
  splitting the two is a small change if wanted.
- The Realm Hub omits the Bar of the House and the Registrar from its sited places. One line each.
- `handleMintPrepare` and `handleMintRemove` stayed Crown-only rather than following mints down to
  Lord, because they table a `SPEND_MINT` bill — outside the delegation matrix.
- Phase 6 in full: chunk capture, occupation rules, capital fall, decisive victory, annexation,
  region merge, war tribute, counter-war. All domain-complete, all unwired, all out of scope here.
