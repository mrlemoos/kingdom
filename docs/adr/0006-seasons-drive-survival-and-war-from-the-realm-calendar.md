# Seasons drive survival and war from the realm calendar

The realm calendar already keeps twelve months of thirty days off the world clock. We hang a **season** on it — four quarters of three whole months — and let one **season profile** feed every mechanic that ought to feel the weather, rather than letting each system grow a calendar check of its own. The season is a pure function of the realm day, so nothing is stored and nothing can drift out of step between systems.

Season boundaries follow the calendar, not the month names in the original sketch: spring is Frostwane, Thawtide, Seedfall; summer is Blossoming, Highmead, Sunwake; autumn is Harvest, Goldfall, Emberwane; winter is Hallowtide, Longnight, Yulewatch. Harvest sits in autumn at the cost of Frostwane reading as early spring, which its name bears. The **campaign season** therefore spans summer into autumn — a war window of six months rather than a single favoured month.

Six mechanics read the profile. Crop growth and mob spawns ride the vanilla `BlockGrowEvent` and `CreatureSpawnEvent`, so seasonal pressure obeys every vanilla rule already in force — light, mob caps, biome — and stays overworld-only; the Nether and the End have no seasons. Villager yield splits **outdoor** trades from indoor ones, so winter shifts the economy rather than merely shrinking it. Military **morale** mends more slowly in winter and decays outright under a winter siege. **Levy upkeep** is charged daily against the treasury, dearer for mustered levies than for the standing roster and dearest in winter; **arrears** cost morale and finally bring **desertion**, after a day's public warning. **Hearths** — any lit campfire with a container against it — burn coal or logs through winter, and villagers left **cold** yield less and finally strike, reusing the strike machinery already in place.

Parliament is left to judge for itself. A Premier who wars or dissolves in winter takes a hit to political standing and has it recorded, but no motion is tabled on their behalf: the motion of no confidence needs two seated MPs to choose it, and a calendar check has no business overriding that. The other five mechanics are what make the grievance real.

Only what cannot be re-derived is persisted, in `data.yml`: cold-day counts, arrears, and the day the season turn was announced. Hearths are found by the territory sweep that already reconciles villager nametags and despawn protection, because a hearth is a fact about the world and the world holds it.

## Considered options

- **Per-month modifier table** — rejected; seventy-two figures nobody would balance, where four profiles carry the same intent.
- **Per-system seasonal config** — rejected; guarantees the systems drift apart.
- **Reordering the months** so the sketch's pairings sit together — rejected; `RealmCalendar` indexes the enum, so every date already written to disk would shift.
- **`randomTickSpeed` per season** for crops — rejected; server-wide and blunt, moving fire, ice and leaf decay along with the wheat.
- **Warmth by proximity alone**, no fuel burnt — kept as the fallback; it asks a one-off build and never a delivery, so winter stops being a logistics problem.
- **A winter fuel levy in Corona** — rejected; no wood or coal would ever change hands.
- **Automatic motion of no confidence** on a winter war — rejected; makes Parliament a rubber stamp for a date check.
- **A separate `seasons.yml`** — rejected; a third state file earns nothing over `data.yml`.
