# A granary feeds the realm through winter

Winter already costs a kingdom fuel and coin; it should cost it grain. The **granary** is a place in the world — a WorldGuard region the Crown links inside its own territory — which fills with hay through the growing year and empties through winter. Its stock is nothing but the hay standing in it, counted off the blocks, so a silo that is full looks full and a realm that is starving can be seen to be starving from the outside.

Grain comes off the realm's farmers, not off a ledger. The daily villager sweep counts farmer-profession villagers in territory and credits wheat scaled by the **season profile**'s outdoor yield — fat in Harvest, nil in winter — and every nine wheat lays one hay block in the granary, lowest course first. Members may pitch in by hand: hay placed in the region counts, and loose wheat left there is taken up on the same daily tick. Capacity is whatever air the builders left; a full granary wastes its surplus and says so, because how much a realm can store ought to be a question of what it built rather than a figure in config.

Through the ninety days of winter the granary is drawn down top-down, one ration a day of `ceil(villagers ÷ heads-per-hay)` bales. Players are not fed by it — they have vanilla hunger — so the granary is the realm's grain and not a larder. A day the ration cannot be drawn leaves the territory's villagers **hungry**, on a ramp deliberately shaped like the cold one and kept in a ledger of its own: the first day costs yield, the third sends them on **strike**, and from the seventh a villager starves to death each day. Seated villager MPs, Lords of the Treasury, the Town Crier and the villager Speaker are spared, as they are spared the strike. A fed day wipes the slate. Cold and hunger bite independently — their yield cuts multiply, either alone can call a strike, only hunger kills — so fuel and grain stay two logistics problems rather than one.

A kingdom with no granary is treated exactly as one with an empty granary: no ration can be drawn, so the ramp runs from the first winter day. Exempting the unbuilt would make building nothing strictly better than building badly. Grace comes from warning instead — on the Harvest season turn and again on the last day of autumn, every kingdom is told by Gazette and town crier how many bales short of the winter it stands. While a famine runs the Crown pays for it politically: subject loyalty falls a step and the grievance is entered in Hansard and cried, on the pattern of **winter censure**, and like winter censure it tables nothing — a motion of no confidence still needs two seated MPs to choose it.

The store is the Crown's. Only a King or Queen, or a Prince or Princess, may break hay out of their own kingdom's granary; anyone else doing so — member, permit holder or operator — commits **grain theft**, which opens an ordinary warrant through the existing pipeline and lands as a paper in the Crown inventory to be pressed or let go. The break is seen by the plugin itself, so no witness is wanted; but no flagrant path either, because a bale broken by a misclick has no business putting a subject in a cell without the Crown's word.

Only what cannot be re-derived is persisted, in `data.yml`: the linked region, the wheat remainder under nine, the run of hungry days behind each villager, the day the shortfall was last cried — as the season turn already keeps its own — and the day famine was last announced. The stock is never written down, so a silo griefed or hand-filled while the server was down still tells the truth the moment it is next read.

## Considered options

- **A stock figure in config, region as decoration** — rejected; a shed and a great barn would hold the same and the build would mean nothing.
- **Persisting the stock beside the blocks** — rejected; two sources of truth that drift the first time a bale is broken offline.
- **Banking surplus wheat invisibly past capacity** — rejected; the silo would stop telling the truth about the realm's stores.
- **Feeding players from the granary** — rejected; doubles up on vanilla hunger and punishes a realm for recruiting.
- **Draining all year, dearer in winter** — rejected; grain would never pile up, and autumn's surplus is the whole point of a store.
- **Exempting kingdoms with no granary**, as no capital exempts a kingdom from build permits — rejected; it rewards not engaging with the mechanic.
- **A grace winter for new realms** — rejected; per-kingdom founding state to track for what a warning already gives.
- **Killing from the first unfed day** — rejected; one night of downtime would empty a realm of villagers with no window to answer.
- **One privation ledger for cold and hunger together** — rejected; fixing one problem would hide the other.
- **Real vanilla farmer harvest events** as the source of grain — rejected; finicky AI, needs fields chunk-loaded, yields near nothing on an unattended server.
- **Sealing the granary to all but the Crown** by refusing the break outright — rejected in favour of theft; refusing teaches nothing, a warrant makes it the realm's business.
- **A flagrant grain-theft warrant** with patrol detain on sight — rejected; no royal filter on an accident.
- **A Reeve of the Granary** to hold the keys — rejected for now; a new office with slots and an appointment command is a feature of its own.
- **A granary keeper NPC** to open the stores GUI — rejected; a fourth NPC to spawn, reconcile and protect where a hay bale is already standing there to be clicked.
