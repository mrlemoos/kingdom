# Kingdom Economy

Economic layer for the Kingdom plugin: player activity and villager life generate wealth; kingdoms collect tax into a shared treasury.

## Language

**Wealth**:
Value held by a player or kingdom, spendable within the economy.
_Avoid_: Money, balance, funds

**Player wallet**:
A player's personal wealth store. Earned from activity; taxed; spent freely.
_Avoid_: Account, balance, pocket

**Kingdom treasury**:
A kingdom's collective wealth pool. Fed by tax; spent on realm-level purposes.
_Avoid_: Bank, vault, national account

**Realm wealth**:
A kingdom's total economic standing: treasury Corona plus the valued worth of material reserves and estates in linked territory, plus the sum of active productive villager wallet balances. Frozen villager wallets are excluded. Informational and for comparison; does not change what the treasury can spend.
_Avoid_: Net worth, GDP, total assets

**Material reserves**:
The Corona worth of precious blocks placed inside a kingdom's WorldGuard region: gold, diamond, emerald, iron, and copper blocks. Each block type has a fixed Corona value in config. Counted incrementally on place and break; reconciled daily.
_Avoid_: Stockpile, ore wealth, block GDP

**Estate**:
A significant structure inside kingdom territory detected automatically by scanning for beacon, conduit, or lodestone blocks. Each type has a fixed Corona worth in config. No manual registration.
_Avoid_: Plot, homestead, land deed, property title

**Corona**:
The kingdom economy's unit of wealth. Tracked on an abstract ledger; one gold nugget represents one Corona when withdrawn or deposited.
_Avoid_: Coin, crown, currency, money

**Deposit**:
Converting held Corona nuggets into Corona on a player's wallet. Only whole nuggets accepted.
_Avoid_: Mint, convert, cash in

**Withdrawal**:
Converting Corona from a wallet or treasury into physical gold nuggets in the player's inventory. Only whole nuggets; ledger balance rounds down.
_Avoid_: Payout, redeem, cash out

**Mint**:
A place inside a kingdom's territory where players deposit Corona nuggets into Corona or withdraw Corona as gold nuggets. One nugget equals one Corona. Mints are placed by the Premier using treasury funds within an approved budget; each kingdom has a limited number.
_Avoid_: Bank, ATM, exchange

**Ledger**:
The authoritative record of Corona balances. Supports fractional amounts; physical gold nuggets exist only after withdrawal.
_Avoid_: Database, account, balance sheet

**Villager GDP**:
Daily Corona income credited to each economically active villager's wallet each in-game day. Scales by profession at configured rates and soft-caps at higher kingdom populations. Credits the villager wallet, not the kingdom treasury.
_Avoid_: Villager tax, population income, NPC revenue

**Productive villager**:
A villager whose bed and workstation are both inside a kingdom's territory. Only productive villagers receive a villager wallet and villager GDP.
_Avoid_: Working villager, employed villager, citizen

**Villager wallet**:
A productive villager's personal Corona balance on the ledger, keyed by the villager entity's UUID. Only productive villagers receive a wallet. Villagers may earn, hold, and spend Corona without player involvement, including villager-to-villager transfers within the same kingdom.
_Avoid_: NPC account, villager balance, mob wallet

**Frozen villager wallet**:
A villager wallet whose balance persists on the ledger but receives no villager GDP and participates in no villager trades while the villager is not productive. Economic activity resumes when the same villager UUID becomes productive again.
_Avoid_: Dormant wallet, inactive account, suspended balance

**Villager wallet escheatment**:
After a configured number of in-game days with a frozen villager wallet, the wallet balance transfers to the kingdom treasury and the wallet is cleared.
_Avoid_: Forfeiture, unclaimed funds, treasury claim

**Seated MP economic participation**:
Seated profession villager MPs act as kingdom-wide proxies for their profession in the villager economy. They receive villager GDP and participate in villager trades even while seated in the Commons and not territory-productive.
_Avoid_: MP income, parliament wage, seated villager GDP

**Villager trade**:
A configured profession trade graph that drives background Corona payments between villager wallets. Each settlement cycle the realm runs a configured number of settlement passes per trade edge; each pass selects a random buyer and seller where the seller's profession exists in the kingdom. Payment is either a configured percentage of the buyer's daily GDP income or a fixed Corona amount on commoner edges involving the `none` profession. A trade is skipped if the buyer cannot pay.
_Avoid_: NPC transaction, villager commerce, profession barter

**Corona merchant trade**:
A player-facing villager merchant offer priced in Corona, defined in config as profession-specific extras added alongside that villager's vanilla emerald trades. Applies only when the villager stands inside a kingdom's linked WorldGuard territory and is an ordinary territory villager—not a Treasury Lord or seated MP. The player pays with gold nuggets first, then player-wallet Corona if short; whole Corona only. Commerce tax routes to that territory's kingdom treasury; the net credits the traded villager's wallet when productive, otherwise the treasury. Player activity Corona from emerald trades remains unchanged.
_Avoid_: Nugget shop, player villager purchase, GUI trade

**Emerald villager trade taxation**:
When a player completes a vanilla emerald villager trade with an ordinary territory villager inside a kingdom's linked WorldGuard territory, the emerald cost is valued as Corona using a configured commerce multiplier separate from the player activity reward rate. Commerce tax on that Corona-equivalent amount routes to that territory's kingdom treasury; the net credits the traded villager's wallet when productive, otherwise the treasury. Does not apply to Treasury Lords or seated MPs. Separate from the player's activity Corona reward for the same trade.
_Avoid_: Emerald duty, emerald sales tax, villager emerald levy

**Villager commerce tax**:
A fixed configured percentage of each villager trade payment routed to the kingdom treasury. Applied at payment time on Corona merchant trades, emerald villager trade taxation (on the Corona-equivalent value), and background villager-wallet trade settlements.
_Avoid_: Trade duty, transaction levy, sales tax

**Villager income tax**:
The kingdom base tax rate applied when villager GDP is credited to a villager wallet. Routed to the kingdom treasury. Noble rank discounts do not apply to villager income.
_Avoid_: Villager levy, GDP tax, profession tithe

**Economic activity**:
Player actions that earn value-weighted Corona into a personal wallet: harvesting crops, crafting items, trading with villagers, and player-to-player commerce. Each category has cooldowns and diminishing returns to discourage farming loops.
_Avoid_: Work, labour, grinding

**Income location**:
The territory where an economic activity took place. Determines whether foreign-income tax surcharge applies. Locations outside every kingdom region count as wilderness.
_Avoid_: Tax jurisdiction, source region, earn zone

**Wilderness**:
Any place not inside a kingdom's WorldGuard region. In practice this is chiefly the Nether and the End, because each kingdom's linked overworld is set via `/kingdom setworld` and fully partitioned into kingdom territories.
_Avoid_: Unclaimed land, frontier, neutral zone

**Wilderness income**:
Corona earned from economic activity or life events in wilderness. Pays at a reduced rate and is not taxed, because no kingdom treasury claims that territory.
_Avoid_: Frontier bonus, unclaimed earnings, neutral income

**Life event**:
Small routine player actions that earn a minor Corona drip into a personal wallet, capped daily: sleeping, eating, building in territory, and social presence near kingdom members. Events inside a player's own kingdom earn a bonus multiplier.
_Avoid_: Daily reward, login bonus, participation trophy

**Tax**:
A slice of wallet income routed to the player's kingdom treasury at credit time. Rate depends on kingdom base rate, payer noble rank, and whether income was earned inside or outside the kingdom's territory.
_Avoid_: Levy, tithe, duty

**Premier**:
The noble who proposes a kingdom's fiscal rates: tax (base, per-rank modifiers, foreign-income surcharge) and Corona transfer fees. Rates take effect only after the King or Queen approves. One Premier slot per kingdom. Elected by seated player MPs after a general election; if no player MPs are seated, the monarch may appoint a Premier.
_Avoid_: Chancellor, treasurer, finance minister

**Premier election**:
A contest among seated player MPs to fill the Premier seat after a general election closes. MPs nominate and vote; ties for Premier are broken as for MP seats—see **election casting vote**.
_Avoid_: Leadership vote, prime-minister ballot, executive election

**Premier villager**:
When a general election seats no player MPs—the full villager parliament—the Premier role passes to a villager drawn from seated profession MPs only; **[MP] Citizen** backfill seats are not eligible. The villager from the profession with the highest count in the election scan is chosen; seat order breaks ties. The seated Premier villager displays a **[Premier]** nametag prefix in place of **[MP]** on that villager only, using the same prefix colour as a player Premier, followed by the profession label (e.g. **[Premier] Farmer**). On appointment after such a full villager general election, the realm automatically tables an **inaugural fiscal package** on the Premier villager's behalf. All bills follow the usual Westminster path: tabled in the Commons, divided under the Speaker, then royal assent in the Lords. The Premier villager holds no casting vote—the Chair does. Once a bill passes division, the monarch must still grant or withhold assent manually in the Lords; there is no automatic or timed assent shortcut.
_Avoid_: Villager chancellor, NPC premier, profession premier

**Inaugural fiscal package**:
The pair of bills tabled automatically when a Premier villager is appointed after a full villager general election. Submitted in sequence on the Premier villager's behalf: first an **inaugural FISCAL bill**, then a BUDGET bill setting the approved spending cap to 50% of the kingdom's current treasury balance, rounded down.
_Avoid_: Starter budget, auto-fiscal, opening bills

**Inaugural FISCAL bill**:
The first bill in an inaugural fiscal package. Proposes fiscal rates adjusted from the kingdom's current enacted rates: each rate field—base tax, per-rank modifiers, foreign-income surcharge, and transfer fees—moves by exactly one percentage point according to the seated Premier villager's **profession vote bias** on FISCAL bills: aye lowers, nay raises, abstain leaves every field unchanged.
_Avoid_: Policy bill, rate shuffle, default fiscal

**Full villager parliament**:
A Commons where every seated MP is a villager and no player MPs hold seats. The Premier role passes to a Premier villager and fiscal bills are submitted on their behalf; divisions run as in any other House, presided over by the Speaker, and close at once because no player MP is waiting to vote. Bills that pass Commons still require the King or Queen to grant royal assent manually in the Lords; the realm does not assent on the monarch's behalf.
_Avoid_: NPC parliament, all-villager chamber, automated legislature

**Tax proposal**:
A pending set of fiscal rates submitted by the Premier: tax rates and transfer fees. Inactive until approved by the King or Queen.
_Avoid_: Bill, decree, budget

**Treasury budget**:
A spending allowance the King or Queen approves for the Premier. The Premier allocates Corona from the treasury within this budget for stipends, projects, and realm upkeep.
_Avoid_: Allowance, spending limit, fiscal cap

**Treasury withdrawal**:
Spending Corona from a kingdom treasury. Small allocations within an approved budget are handled by the Premier; large or exceptional spends require King or Queen approval.
_Avoid_: Payout, disbursement, transfer

**Corona transfer**:
Sending Corona from one player wallet to another. A fee is deducted and routed to the sender's kingdom treasury; cross-kingdom transfers incur a higher fee.
_Avoid_: Payment, remittance, wire

**Transfer fee**:
The percentage charged on player-to-player Corona transfers. Proposed by the Premier and enacted after King or Queen approval, alongside tax rates.
_Avoid_: Transaction fee, service charge, commission

## Parliament

**Parliament**:
The kingdom legislature. Fiscal rates, treasury budget caps, and treasury spending pass as Acts through the House of Commons and receive royal assent in the House of Lords.
_Avoid_: Congress, senate, government

**House of Commons**:
The elected chamber where the Premier tables bills and Members of Parliament vote. Debate and divisions take place within the chamber bounds set by the monarch, presided over by the Speaker.
_Avoid_: Lower house (in player-facing text), assembly

**Speaker**:
The presiding officer of the House of Commons. Opens and closes divisions, and casts the casting vote when the House is tied. Not a Member of Parliament: the Speaker holds no seat, votes in no ordinary division, and sits apart from the House in the Speaker's Chair. One Speaker per kingdom. Held by a player of Speaker rank when the monarch has assigned one; otherwise by a **villager Speaker**.
_Avoid_: Chairman, moderator, house leader

**Speaker's Chair**:
The place in the Commons where the Speaker presides, set by the monarch. It is not one of the eight MP seats—seating a Speaker never costs the House a vote. Where no chair has been set, the villager Speaker stands at the Commons chamber point.
_Avoid_: Speaker seat, ninth seat, throne

**Villager Speaker**:
The villager who presides over the Commons whenever no player holds the Speakership. Seated in the Speaker's Chair at the close of a general election, dismissed when a player is assigned Speaker and returning if that player leaves office, and dismissed on prorogation. Spawned unaligned: it has no profession, no profession vote bias, and never claims a territory villager, so no villager is drawn out of the economy to preside. Its nametag reads **[Speaker]** alone, with no profession label—that absence is the sign of an impartial Chair. Damageable like any villager and protected from despawning like a seated MP; if killed, the realm seats a replacement at the chair. It is not an elected office and cannot resign.
_Avoid_: NPC Speaker, speaker bot, acting Speaker

**House of Lords**:
The chamber where the King or Queen grants or withholds royal assent on bills passed by the Commons. Assent and rejection both require the monarch to be present in the Lords.
_Avoid_: Upper house (in player-facing text), senate

**Act**:
A bill that has passed the Commons and received royal assent. Enacts fiscal rates, a budget cap, or an authorised treasury spend. Recorded as a written book in the registrar.
_Avoid_: Law, statute, decree

**Bill**:
A formal proposal before Parliament: fiscal rates, treasury budget, or treasury spend (including mint placement). Only one bill may be in progress per kingdom at a time. Under a full villager parliament, fiscal bills may be submitted on the Premier villager's behalf; they still table in the Commons and pass through division, but royal assent in the Lords is unchanged—the monarch must be present and grant or withhold assent manually. No bill becomes an Act without that step, and there is no automatic or timed assent for villager-submitted bills.
_Avoid_: Proposal, motion, decree

**Division**:
A Commons vote on the bill before the House. The Speaker opens and closes the division; MPs vote aye, nay, or abstain while the division is open. A player Speaker opens and closes at will. A villager Speaker opens the division as soon as a bill is tabled and closes it at the end of the **division window**—or at once when no player MPs are seated, there being nobody to wait for. Either way, a division that ties is settled by the **casting vote**. Presiding over the Commons never reaches the Lords: a bill that passes division still awaits royal assent granted manually by the monarch.
_Avoid_: Poll, ballot, referendum, auto-pass

**Division window**:
The time a villager Speaker holds a division open for seated player MPs to vote, counted in in-game days. Ends the division whether or not every MP has voted.
_Avoid_: Voting period, timer, debate clock

**Casting vote**:
The tie-breaking vote when aye and nay are equal in a Commons division, cast by whoever holds the Chair. A player Speaker chooses aye or nay, and must cast before a tied division can pass or fail. A villager Speaker casts nay by convention: an unelected Chair leaves the standing position undisturbed rather than carrying a bill on an evenly divided House, so a tied division fails.
_Avoid_: Tie-breaker, deciding vote, government casting vote

**Royal assent**:
The monarch's approval of a bill passed by the Commons, given in the House of Lords. Withholding assent rejects the bill without enacting it. Assent and rejection both require the King or Queen to be present in the Lords and to act manually; this applies equally to bills from a full villager parliament, including those tabled on the Premier villager's behalf and divided under a villager Speaker. There is no automatic assent, timed assent, or shortcut of any kind—only the monarch may grant or withhold assent.
_Avoid_: Signature, ratification, approval

**Registrar**:
The archive where assented Acts are stored as written books on chiseled bookshelves. The monarch sets the anchor bookshelf; further Acts fill slots and extend to adjacent shelves.
_Avoid_: Archive, library, record office

**Member of Parliament**:
A seated MP who may vote in Commons divisions. Citizens without the MP title cannot vote.
_Avoid_: Representative, congressman, delegate

**General election**:
A kingdom-wide contest for all eight Commons seats. Citizens may stand for up to four player MP seats; remaining seats are filled by profession villager MPs from the top productive-villager professions. Called by the monarch or on a fixed in-game-day schedule.
_Avoid_: Primary, poll, national vote

**By-election**:
A contest to fill a single vacant MP seat between general elections. Uses the same nomination and voting period as a general election.
_Avoid_: Special election, runoff, recall

**Profession MP**:
A villager MP representing one of the kingdom's largest productive-villager professions. Stands in the Commons as a persistent villager with an MP prefix; division votes are cast automatically from **profession vote bias** on each bill type.
_Avoid_: NPC delegate, villager representative, profession delegate

**Profession vote bias**:
The configured vote leaning assigned to each villager profession per bill type. Profession MPs cast Commons division votes automatically from these biases: aye, nay, or abstain. On FISCAL bills, the seated Premier villager's profession bias also sets the direction of the ±1 percentage point adjustment applied to every enacted rate field when an inaugural FISCAL bill is tabled—aye lowers, nay raises, abstain unchanged.
_Avoid_: Party line, whip, ideology table

**Citizen MP**:
A villager MP with no productive profession, seated when fewer distinct professions exist than villager MP seats. Displayed as **[MP] Citizen**; division votes abstain unless configured.
_Avoid_: Generic MP, placeholder delegate, none profession

**Commoner**:
An ordinary villager with no profession. Shown on the villager nametag when they are not a seated profession MP. In the villager economy, commoners receive a small configured villager GDP rate and may participate in dedicated commoner trade edges alongside standard profession trade graph edges.
_Avoid_: Citizen, peasant, unemployed villager

**Territory villager despawn protection**:
Ordinary villagers (not seated MPs, not Treasury Lords) standing inside a kingdom's linked WorldGuard region are kept from despawning. Protection is position-scoped: it applies while the villager is in territory and reverts to vanilla despawn rules when they leave. Re-evaluated on server startup, chunk load, villager spawn, and a periodic sweep.
_Avoid_: Persistent villagers, anti-despawn tag, villager anchor

**Election casting vote**:
The tie-breaking choice when two or more citizen candidates tie for the last available player MP seat, or for the Premier seat. Required before that election can close. A player Speaker chooses, and the count waits on them. With no player Speaker seated, the realm decides in the Speaker's name by earliest nomination and the count never waits—a villager Speaker cannot cast it, being seated only once the election has closed.
_Avoid_: Tie-breaker, deciding ballot, runoff vote

**Resignation offer**:
A pending request from a seated Premier, player MP, or villager MP (including the Premier villager) to leave office. The office-holder remains fully in post—with parliamentary powers intact—until the King or Queen accepts the offer, or a Prince accepts when no King or Queen is seated. Rejection leaves them in office. Only one resignation may await royal approval per kingdom at a time.
_Avoid_: Quit notice, immediate removal, self-dismissal

**Royal resignation approval**:
The monarch's—or, in the absence of a seated King or Queen, a Prince's—asynchronous acceptance or rejection of a resignation offer. A **resignation letter** (paper item) is delivered to the Crown's inventory when any resignation is offered; right-clicking the letter opens the review interface. If the Crown is offline, the letter is delivered on next login. The letter is removed once the resignation is accepted or rejected. Review remains available in the House of Lords as a fallback.
_Avoid_: Instant quit, auto-vacancy, Speaker dismissal

**Resignation letter**:
A paper item delivered to the King, Queen, or regent Prince when a resignation offer is pending. Right-clicking it opens the resignation review interface. Tagged to the kingdom so only the Crown may use it. Removed when the offer is resolved or if it is no longer current.
_Avoid_: Chat-only notice, Lords-only review, forged paper

**Locate compass**:
A compass given by `/locate` that points at the requested place: a kingdom checkpoint, or the nearest structure or biome in the player's current world. Named with the target label and block coordinates on the lore. Untracked lodestone behaviour — no physical lodestone required.
_Avoid_: Recovery compass, map marker, waypoint pearl

**Staff teleport notification**:
A private audit message sent to online players with `minecraft.command.teleport` when another player with that permission teleports someone else. The actor is not notified; console actors appear as "Console". Destinations name players, kingdom checkpoints, or coordinates as appropriate.
_Avoid_: Teleport broadcast, public teleport announce, server-wide tp alert

**Session**:
The working life of a Parliament, from State Opening to prorogation. Parliament conducts no business outside a session: no bill may be tabled, no division opened or voted on, and no royal assent granted or withheld. A kingdom that has never held a general election is in session by default.
_Avoid_: Term, sitting, mandate

**Prorogation**:
The end of a session, triggered when a general election is called. The bill before Parliament dies on the order paper — it is discarded, not carried over — along with any prepared mint and pending inaugural fiscal or budget package. The new Parliament must re-table its business after the State Opening.
_Avoid_: Dissolution, adjournment, recess

**State Opening**:
The ceremony in which the Crown opens a new session once a government has formed after a general election. The Crown summons the realm to the House of Lords, then declares Parliament open from within the chamber. Until it happens, Parliament remains prorogued.
_Avoid_: Inauguration, swearing-in, coronation

**Speech from the Throne**:
The summons delivered to the King or Queen — or to the heir acting as regent when no monarch is seated — when a session awaits opening. Right-clicking it opens the State Opening interface: summon the realm, then declare Parliament open. Redelivered on login if missing, and removed once the session opens.
_Avoid_: Royal decree, king's speech scroll, opening address book

**Royal commission**:
The fallback that opens a session without ceremony: used when the kingdom has no House of Lords set, or when neither monarch nor regent has opened Parliament within three in-game days of the government forming. Parliament opens by announcement alone — no summons, no teleport — so the realm's business is never frozen by an absent Crown.
_Avoid_: Auto-open, timeout open, forced opening

## Police

**Police department**:
The kingdom law-and-order system administered under `/kingdom police`. Cases follow a warrant → trial → sentence pipeline for role-play enforcement inside linked territory.
_Avoid_: Police force, sheriff's office, militia

**Sworn role**:
A kingdom law-enforcement appointment separate from noble rank. Constable and Judge are sworn roles; the King or Queen appoints and removes them.
_Avoid_: Noble title, rank, office of state

**Constable**:
A sworn role authorised to file warrant applications and arrest suspects with an active warrant inside kingdom territory. Displays a **[Constable]** chat prefix. Appointed by the King or Queen. A player may hold constable or judge, not both.
_Avoid_: Officer, deputy, guard

**Judge**:
A sworn role that adjudicates trials and passes sentence. Displays a **[Judge]** chat prefix. Appointed by the King or Queen. A player may hold constable or judge, not both.
_Avoid_: Magistrate, justice, arbiter

**Court**:
The kingdom trial venue, anchored at a lectern placed in linked territory. When no player judges are online, the seated villager judge at the court conducts a realm-handled trial.
_Avoid_: Courthouse, tribunal hall, hearing room

**Villager judge**:
A villager NPC seated at the court lectern as the court anchor. When no player judge is available to take a case, the villager judge conducts a realm-handled trial with a weighted random verdict.
_Avoid_: NPC justice, court clerk, automated judge

**Warrant**:
A constable's formal application to pursue a named suspect. Inactive until the Crown approves it; until then patrol golems and constables may not act on it.
_Avoid_: Bounty, hit list, detention order

**Royal warrant approval**:
The Crown's acceptance or rejection of a warrant application, using the same paper-and-review workflow as a resignation letter. On approval the warrant becomes active; on rejection it does not.
_Avoid_: Instant warrant, auto-approve, chat approval

**Active warrant**:
An approved warrant that authorises constables and patrol golems to arrest the named suspect inside kingdom territory.
_Avoid_: Open case, manhunt tag, wanted flag

**Infrastructure gate**:
A requirement that at least one numbered prison cell and one court lectern are configured in linked territory before warrant applications or arrests are permitted.
_Avoid_: Police unlock, setup check, readiness flag

**Cell**:
A numbered confinement point inside kingdom territory, set by the King, Queen, or an operator. Kingdoms may configure any number of cells. Prison sentences assign the lowest free numbered configured slot.
_Avoid_: Jail bed, spawn point, detention zone

**Patrol golem**:
An iron golem officer tagged as kingdom police. Inside linked territory it automatically pursues and detains players with an active warrant, placing them in the same pending-trial flow as a constable arrest. Deployed separately from guard golems; each kingdom has a configurable cap (default two).
_Avoid_: Warrant bot, auto-cop, pursuit mob

**Guard golem**:
A stationary iron golem officer posted at the court or prison cells. Provides presence and security but has no power to arrest or detain. Deployed separately from patrol golems; each kingdom has a configurable cap (default two).
_Avoid_: Court sentry, prison warden mob, bouncer

**Jurisdiction**:
The scope of police authority: any person physically inside the kingdom's linked WorldGuard territory, whether a member or a visitor.
_Avoid_: Citizens only, member crimes, home turf rule

**Immunity**:
The King, Queen, and Prince cannot be subject to a warrant or arrest under kingdom police law.
_Avoid_: Royal exemption, crown privilege, diplomatic immunity

**Arrest**:
Taking a suspect with an active warrant into custody and opening a pending trial. Constables arrest manually; patrol golems detain automatically inside territory. A player judge is chosen at random from online judges, excluding the accused, the arresting constable, and the Crown who approved the warrant; if none qualify, the villager judge hears the case.
_Avoid_: Ban, kick, instant jail

**Pending trial**:
The state between arrest and verdict. The assigned judge must adjudicate before a sentence takes effect.
_Avoid_: Pre-trial hold, limbo, cooldown

**Trial**:
The hearing before a player judge or villager judge. Verdict options: guilty with a prison sentence, guilty with a fine paid to the treasury, a formal warning (record only), or not guilty.
_Avoid_: Hearing, prosecution, court session

**Prison sentence**:
A guilty verdict that confines the player to an assigned numbered cell for a configured real-world duration (presets of five, fifteen, thirty, or sixty minutes). Hard confinement applies: kingdom teleport is blocked and the player is returned to the cell if they move more than eight blocks away.
_Avoid_: Temp ban, mute sentence, soft jail

**Fine sentence**:
A guilty verdict that levies Corona from the convicted player to the kingdom treasury without imprisonment.
_Avoid_: Bounty payment, damages, restitution order

**Warning**:
A guilty verdict that records the offence without prison time or a treasury fine.
_Avoid_: Caution, slap on the wrist, strike

**Realm-handled trial**:
A trial conducted by the villager judge at the court when no eligible player judge is online to take the case. The villager judge returns a weighted random verdict from the standard options.
_Avoid_: Auto-conviction, script trial, NPC prosecution

**Sentence**:
The outcome of a completed trial: prison, fine, warning, or acquittal. Closes the warrant → trial → sentence pipeline for that case.
_Avoid_: Punishment roll, karma, penalty phase

## War

**Loyalty**:
A subject's bond to a kingdom, tracked as two independent measures: **political loyalty** and **military morale**. A change to one does not automatically change the other; a **dual-track offence** applies each track's automatic rules separately.
_Avoid_: Reputation, karma, favour, standing

**Dual-track offence**:
A single act that triggers both **morale breach** and **political offence** rules independently—for example **defection**. Automatic tier drops apply per track immediately where defined; **Traitor** applies only on treason **conviction**, not on battlefield report alone.
_Avoid_: Double jeopardy ban, merged sentence, one strike rule

**Political loyalty**:
How faithfully a subject upholds civil obligations to the crown, expressed as a **loyalty tier**: Faithful, Doubtful, Disloyal, or Traitor. Lowered by **political offences**; governs access to office, crown trust, and non-combat penalties.
_Avoid_: Morale, honour score, alignment

**Loyalty tier**:
The political loyalty ladder for fealty subjects. **Faithful** is default on kingdom join or **oath of service** for sworn outsiders; lower tiers follow **political offences** or court sentences. Tier recovers via **loyalty recovery** or **loyalty pardon**.
_Avoid_: Reputation rank, karma level, trust score

**Faithful**:
Full civil trust. Eligible for office, parliamentary votes if seated, and crown appointments without extra scrutiny.
_Avoid_: VIP citizen, trusted flag, green name

**Doubtful**:
Political standing after a minor **political offence** or recorded warning from court. Cannot receive new crown appointments until restored; seated office unchanged until resignation or election loss.
_Avoid_: Yellow card, probation tag, soft ban

**Disloyal**:
Political standing after repeated offences or a fine sentence for treason-related crime. Barred from holding office and from voting in Commons; may still serve on levy if military morale permits.
_Avoid_: Greylist, muted citizen, half-citizen

**Traitor**:
Political standing after conviction for **treason** or equivalent court sentence. Barred from office, levy, and crown trust; subject to warrant and arrest while inside **jurisdiction** despite prior rank. Cleared only by acquittal or **loyalty pardon**; distinct from **Rout**.
_Avoid_: Auto-ban, perma-kick, enemy team

**Loyalty recovery**:
Restoring **loyalty tier** after a **political offence** without a pardon. One tier per configured number of in-game days without further offence, up to **Faithful**. **Traitor** cannot recover by time alone.
_Avoid_: Play time reward, login streak, good boy points

**Loyalty pardon**:
The monarch restoring a subject's political loyalty at court. Returns tier to **Faithful**, or to **Doubtful** after **Traitor** if the crown chooses partial restoration. Required to clear **Traitor** without acquittal.
_Avoid_: Unban command, forgive keystroke, amnesty button

**Loyalty penalty**:
The civil effects of **loyalty tier** beyond office bars. **Doubtful** subjects are flagged for constable scrutiny; **Disloyal** subjects become warrant-eligible on further **political offences**; **Traitor** subjects may be arrested on sight inside **jurisdiction** when a warrant is active or on fresh treason report. No noble **loyalty immunity**—automatic tier drops apply to all fealty subjects; monarch **warrant immunity** under police law is separate.
_Avoid_: Chat mute, glow red, wanted stars UI

**Political offence**:
An act that lowers **loyalty tier** by weighted severity: **Act breach** → **Doubtful**; repeat or severe breach → **Disloyal**; **treason** on conviction → **Traitor**. May parallel **morale breach** when the same act spans both tracks.
_Avoid_: Warning message, admin slap, chat rule break

**Act breach**:
Violating a **conduct provision** in any enacted Act—build bans, curfews, war limits, or similar behaviour rules embedded in fiscal, war, or supply Acts. PvP restrictions are not used under **open PvP**. Fiscal rate changes alone are not breaches; forbidden conduct is. A **political offence** when detected.
_Avoid_: Tax underpayment, missing budget, low treasury

**Conduct provision**:
A behaviour rule embedded in a bill or enacted Act, separate from fiscal rate fields. Kinds include build ban, curfew, and war limit. Fiscal-only Acts may carry none. PvP restrictions are not used under **open PvP**.
_Avoid_: Tax rate, budget line, mint cost, chat filter

**Open PvP**:
Current kingdom policy: player-versus-player damage is not cancelled by Acts, occupation rules, or war state. **War combat**, **friendly fire**, and **siege neutral** damage gating are deferred. **Battlefield treason** may still be detected when kingdoms are **at war** under **open PvP**. There is no **trial arena**.
_Avoid_: PvP always off, safe zone plugin, faction combat tag

**Military morale**:
How willingly a subject fights for the kingdom in wartime, expressed as a **morale tier**: Steadfast, Shaken, Breaking, or Rout. Governs combat-era penalties and NPC squad reliability; separate from parliamentary or court standing.
_Avoid_: Political loyalty, PvP skill, combat level

**Morale tier**:
The military morale ladder for fealty subjects on levy. **Steadfast** is default after oath or muster; **Shaken**, **Breaking**, and **Rout** follow as **morale breaches** accumulate. Tier recovers via **morale recovery** or **morale pardon**.
_Avoid_: Buff level, combat tag, team colour

**Steadfast**:
Full willingness to fight. Normal muster compliance; the officer's **squads** follow orders reliably.
_Avoid_: Ready buff, full health, eager flag

**Shaken**:
Morale after a minor **morale breach**—typically refusing muster once. Minor combat debuffs; the officer's **squads** occasionally hesitate.
_Avoid_: Slowness potion, weak debuff

**Breaking**:
Morale after repeated breaches or leaving an active siege without release. Stronger debuffs; the officer's **squads** may scatter from command.
_Avoid_: Fear effect, flee AI always

**Rout**:
Morale collapsed—the lowest **morale tier**. Subject is unfit for levy duty until restored; still absent from muster counts as **desertion**. The officer's **squads** break and flee; fighting for the enemy may trigger **treason** review.
_Avoid_: Combat log ban, auto-kick, instant traitor

**Fealty subject**:
A player tracked for loyalty—either a kingdom member or a **sworn outsider**. Members gain **political loyalty** on join; **military morale** begins after **oath of service** or when they answer a **muster**—no separate oath required if they answer muster. Sworn outsiders gain both tracks via oath without membership.
_Avoid_: Citizen, member, ally tag

**Oath of service**:
A ceremony pledging military obligation to a kingdom—at a court lectern, throne checkpoint, or muster point. Early voluntary bind for members; required entry for **sworn outsiders**. Answering a **muster** binds military morale for members without a prior oath.
_Avoid_: Join command, team accept, contract sign GUI

**Sworn outsider**:
A non-member who pledges fealty to a kingdom for a bounded purpose—typically wartime service as a mercenary or allied fighter. Begins at **Faithful** on **oath of service** for political loyalty and **Steadfast** for military morale; never gains office or Commons vote regardless of tier.
_Avoid_: Mercenary rank, temp citizen, guest fighter

**Army**:
A kingdom's military strength in two layers: a **standing force** always on roster and a **levy** raised per campaign when war is declared. Player officers command both; NPC **rank-and-file** fill enlisted slots under cap.
_Avoid_: Militia tag, PvP team, mob horde

**Standing force**:
The permanent military core—typically knights and appointed officers—maintained on an explicit **standing roster** between wars. Small capped roster; not demobbed when peace returns. Rostered members are **auto-on-duty** at **Steadfast** on **war bill** enactment with **hardened service** rules. A knight title alone does not imply roster membership.
_Avoid_: Garrison plugin, permanent army tag, royal guard

**Standing roster**:
The named list of kingdom **members** in the **standing force**, appointed and removed by the King or Queen. **Sworn outsiders** are never rostered; they serve through oath and levy only. Only rostered members receive **auto-on-duty** mobilisation; other members—including knights not rostered—follow levy **muster** rules. Roster size is capped in configuration.
_Avoid_: Knight permission node, title auto-enlist, OP list

**Auto-on-duty**:
The automatic military mobilisation of the **standing force** when a **war bill** is enacted. Opens or refreshes the military morale track at **Steadfast** without a **muster** response. **Hardened service** applies from enactment until **demobilisation**.
_Avoid_: Auto-teleport front, kit on join war, PvP flag all

**Hardened service**:
Morale rules for the **standing force** after **auto-on-duty**: same **morale tier** ladder as the levy, but stricter on **siege release**—brief departures with release are lawful; absence from an active siege without release for more than one in-game day counts as a **morale breach** to **Breaking**. **Fighting for the enemy** applies as for levy; levy **muster** rules do not.
_Avoid_: VIP soldier, knight immunity, elite bypass

**Levy**:
Soldiers raised for one war from kingdom members and sworn outsiders who answer the muster. Demobbed when that war ends; military morale during levy matters for desertion.
_Avoid_: Draft list, temp team, war roster

**Muster**:
The act of calling the levy to arms after war is declared. For members without prior **oath of service**, answering the muster opens the military morale track at **Steadfast**; refusing drops to **Shaken**. Sworn outsiders must already be bound by oath. A member who does not answer by the **muster deadline** suffers **ignored muster**.
_Avoid_: Teleport summon, kit command, rally point

**Muster deadline**:
The end of the muster window named in the enacted **war bill**. Members who neither answer nor refuse the **muster** by this deadline suffer **ignored muster**.
_Avoid_: War timer, grace period, AFK check

**Ignored muster**:
Failing to answer a **muster** by the **muster deadline**. Opens the military morale track at **Shaken** and lowers political loyalty to **Doubtful** for ignoring crown war summons.
_Avoid_: AFK farmer, offline excuse, mute button

**Civilian member**:
A kingdom member with political loyalty but no active military morale track—neither mustered nor sworn by **oath of service**. May enter a **siege** freely; taking hostile action there auto-binds the military track at **Shaken** without prior **muster** compliance.
_Avoid_: Non-combat tag, spectator mode, tourist visa

**Siege**:
The phase of war fought inside the defender's linked territory, where **chunk capture** is active. The attacker sieges; the defender **recapture**s only—invading the attacker's homeland requires a separate **war bill** tabled by the defender. Open-field **battle** occurs outside enemy linked territory.
_Avoid_: Raid, PvP flag, war zone

**Counter-war**:
A new **war bill** tabled by a kingdom that was the defender in an earlier war, authorising **siege** in the former attacker's linked territory. Distinct from **recapture** during the original war.
_Avoid_: Revenge raid, auto counterattack, mutual war flag

**Siege release**:
Permission for a fealty subject to leave an active **siege** without a **morale breach**. Granted by the subject's commanding officer in the field or by the crown or a knight at a muster point. Unreleased departures count as desertion.
_Avoid_: Teleport home, leave war zone command, unsiege button

**Battle**:
Combat during war that is not a **siege**—fights in neutral ground, wilderness, or home territory before the front advances into enemy linked territory. **War combat** rules apply.
_Avoid_: Skirmish plugin, duel, brawl

**War combat**:
*(Deferred under **open PvP**.)* The planned PvP permission model during an active 1v1 war: only **military participants** may damage enemy **military participants** in **siege** or **battle** zones, with **friendly fire** disabled and **siege neutral** bystanders protected. Not enforced until open PvP is lifted.
_Avoid_: Faction PvP on, team damage, war flag all

**Friendly fire**:
*(Deferred under **open PvP**.)* Planned rule: same-kingdom damage between liege **military participants** during war is disabled; liege damage may count as **battlefield treason** (zone-agnostic once **at war**).
_Avoid_: Team kill on, guild friendly fire, duel override

**Siege neutral**:
*(Deferred under **open PvP**.)* Planned rule: players not **military participants** for either belligerent are outside **war combat** and should not take war-sanctioned damage.
_Avoid_: Safe zone, spectator mode, world spawn protection

**Chunk capture**:
Contested control of map chunks inside a **siege**. Progress when attacker **military participants** outnumber defender **military participants** in the chunk over a configured tick window. On flip, the chunk becomes **captured**—attacker **occupation** rules apply immediately. Enough **captured** chunks satisfy a **territory threshold** **war aim** or feed **annexation** at **decisive victory**.
_Avoid_: Claim plugin, land grab, faction power

**Captured chunk**:
A chunk inside enemy linked territory that has flipped to attacker control during **siege**. **Occupation** rules apply immediately; the linked WorldGuard region boundary is unchanged until **region merge**. Defenders may **recapture** the chunk using the same **chunk capture** presence rules.
_Avoid_: World edit paste, instant border, faction claim

**Recapture**:
A defender **chunk capture** that returns a **captured chunk** to defender **occupation** or home control during an active war. Uses the same presence rules as the initial flip; removes the chunk from the attacker's war tally.
_Avoid_: Undo command, rollback, admin restore

**Occupation**:
The control state of a **captured chunk** during an active war. Attacker military participants gain configured build/PvP rights; defender civilians keep political rights but not military presence credit. Ends on **peace bill**, **decisive victory** **region merge**, or chunk recapture by defenders.
_Avoid_: Raid mode, grief permit, temp trust

**Region merge**:
The WorldGuard operation that redraws linked territory after **decisive victory** with an **annexation** outcome—or when a **territory threshold** aim completes—folding **captured** chunks into the attacker's region. Deferred during war in favour of per-chunk **occupation**.
_Avoid_: //expand, manual redraw, faction merge command

**Military participant**:
A fealty subject whose military morale track is active for a side in the current war—**standing roster** **auto-on-duty**, levy who answered **muster**, **sworn outsiders** under oath, or a member bound by **civilian member** hostile action in **siege**. Counts toward **chunk capture** presence only while inside the contested chunk.
_Avoid_: Everyone online, all citizens, tab-list team

**Desertion**:
A military offence by a fealty subject on levy: refusing the **muster**, leaving an active **siege** without **siege release**, or **fighting for the enemy**. Each class is a **morale breach** with a weighted tier drop; defection and severe battlefield treason may trigger **treason** on the political track.
_Avoid_: Combat log, queue dodge, going AWOL (casual)

**Morale breach**:
A military offence that lowers **morale tier** by weighted severity: refusing the **muster** → **Shaken**; leaving an active **siege** without **siege release** → **Breaking**; **fighting for the enemy** → **Rout** and treason review. Recovery is by honourable siege service or **morale pardon**.
_Avoid_: Warning strike, admin note, chat slap

**Fighting for the enemy**:
Aiding the opposing kingdom in war in two forms: **battlefield treason**—dealing damage to liege soldiers while on levy anywhere the kingdoms are **at war**—and **defection**—accepting an enemy **oath of service** or muster. Both force **Rout**; **defection** always opens treason review; battlefield treason may open it.
_Avoid_: Friendly fire toggle, team kill, accidental hit

**Battlefield treason**:
Damaging a liege **military participant** while the actor's kingdom is **at war** with the victim's kingdom and the actor remains on the liege's levy roster for that **active war**. No **siege** or **battle** zone is required—the offence may occur anywhere on the map. A form of **fighting for the enemy**; forces **Rout** and may trigger treason review. Not possible during peace or after **demobilisation**. Under **open PvP**, damage is not prevented, but the offence may still be recorded.
_Avoid_: PvP accident, duel, training damage, siege-only treason

**Defection**:
Leaving the liege's levy during an **active war** to accept an enemy **oath of service** or muster. A form of **fighting for the enemy**; forces **Rout** and always opens treason review. Requires the actor's kingdom to be **at war** with the kingdom they join.
_Avoid_: Kingdom switch, alt account, spy role

**Morale recovery**:
Restoring **morale tier** after a breach without ending the war. Honourable service in an active **siege** slowly raises tier one step per in-game day without further breach, up to **Steadfast**. **Rout** requires **morale pardon** before the subject may muster again.
_Avoid_: Sleep to heal, eat food buff, passive regen

**Morale pardon**:
The crown or an appointed knight restoring a fealty subject's military morale at a muster point or court. Returns tier to **Steadfast**; required to clear **Rout** before the next levy duty.
_Avoid_: Admin unban, debuff clear command, forgive button

**Morale penalty**:
The in-game effects of **morale tier** on a fealty subject. **Shaken** and **Breaking** apply scaling potion debuffs and limit **squad** command; **Rout** applies severe debuffs, blocks levy duty, and disables squad command until **morale pardon**.
_Avoid_: Custom weakness mod, damage multiplier UI, hunger punishment

**Treason**:
A political offence against the crown—swearing fealty then aiding an enemy kingdom, defying enacted Acts, or desertion so severe it breaches oath. Handled through the police and court pipeline, not morale debuffs alone.
_Avoid_: Ban reason, griefer tag, karma hit

**Declaration of war**:
A formal proposal by the monarch to make war on another kingdom. Hostilities do not begin until a **war bill** passes the Commons and receives **royal assent**; assent alone before Commons passage is insufficient.
_Avoid_: PvP toggle, faction war command, admin war

**War bill**:
A parliamentary bill authorising one kingdom's war against one named defender kingdom. Tabled by the monarch; requires Commons division and **royal assent** like other Acts. Names the sole target, **war aim**, victory **outcome**, and **muster deadline** duration. On enactment, creates an **active war** and both kingdoms are **at war**; the **levy** may be called and **siege** may begin in the target's linked territory.
_Avoid_: War vote command, raid permission, hostility flag

**Active war**:
The bilateral hostilities between exactly two kingdoms from enactment of a **war bill** until **peace bill** enactment or **decisive victory** **demobilisation**. Records attacker, defender, **war aim**, outcome, **muster deadline**, and war clock. At most one **active war** per kingdom pair at a time.
_Avoid_: Raid timer, faction war id, PvP flag

**At war**:
A kingdom wartime state while it is a **belligerent** in an **active war**. Begins when a **war bill** naming that kingdom as attacker or defender receives **royal assent**; ends when that **active war** closes. While **at war**, **battlefield treason**, **muster**, **siege**, and levy rules apply.
_Avoid_: PvP enabled tag, combat mode, war team

**At peace**:
A kingdom state with no **active war** involving it. The default outside wartime; shown on kingdom info as peace rather than an enemy and **war aim**.
_Avoid_: Neutral flag, PvP off tag, ceasefire mode

**Belligerent**:
Either kingdom party to an **active war**—the attacking realm that enacted the **war bill** and the named defender. Both are **at war** from enactment until the war ends.
_Avoid_: Ally, coalition member, neutral observer

**Rank-and-file**:
Enlisted NPC soldiers under player officers, drawn from two pools: **pressed villagers** conscripted from territory population and **crown squads** bought from treasury. Both are capped per kingdom and demobbed when the war ends.
_Avoid_: Mob army, minion pack, hired golem

**Squad**:
A capped group of **rank-and-file** NPCs assigned to one player officer on levy. Behaviour inherits the officer's **morale tier**: hesitate at Shaken, scatter at Breaking, **rout** at Rout.
_Avoid_: Mob stack, pet army, wolf pack

**Squad rout**:
When an officer reaches **Rout**, assigned **squads** break. **Pressed villagers** flee toward home territory and re-enter the villager economy if they survive; **crown squads** scatter and are lost—they do not return after **demobilisation**.
_Avoid_: Mob despawn all, villager delete, respawn same squad

**Pressed villager**:
A productive territory villager conscripted into the levy as rank-and-file. Removed from normal villager economy while pressed; returned on demobilisation if still alive.
_Avoid_: Villager MP, soldier villager profession, NPC knight

**Crown squad**:
Treasury-funded spawned soldiers—vanilla mobs under kingdom command—raised to supplement pressed villagers. Counts against army cap; costs Corona from approved war spending.
_Avoid_: Summoned horde, iron golem army, spawn egg troop

**Peace bill**:
A parliamentary bill ending an active war without **decisive victory**. Requires Commons division and royal assent like other Acts. On enactment, hostilities cease, all **captured** chunks **revert** to defender control, the **levy** is demobbed, and no **region merge** occurs. **Annexation** requires victory, not negotiated peace.
_Avoid_: Truce command, PvP off, white flag

**Revert**:
Returning a **captured chunk** to defender home control on **peace bill** enactment. Clears attacker **occupation**; does not change WorldGuard region boundaries because no **region merge** occurred.
_Avoid_: Rollback plugin, undo capture, admin heal

**Decisive victory**:
An automatic war end when a configured war aim is met—without a peace bill. Triggers demobilisation and applies conquest outcomes (border shifts, tribute, or annexation per enacted war aims).
_Avoid_: Score cap, last team standing, admin ceasefire

**Demobilisation**:
The end of an **active war** by **peace bill** or **decisive victory**. Both **belligerents** cease to be **at war**. **Levy** soldiers are released: military morale track closes and tiers reset—**Rout** still needs **morale pardon** before the next muster. **Standing roster** members remain on duty roster; their morale tiers persist until **morale recovery** or **morale pardon**. Pressed villagers and crown squads are released per **squad rout** rules. **Battlefield treason** is no longer possible after demobilisation.
_Avoid_: Kit remove, team untag, mob despawn command

**War aim**:
The victory condition named in a **war bill**: a **territory threshold** (percentage of enemy linked chunks **captured**) or **capital fall**. One aim per war; meeting it triggers **decisive victory**.
_Avoid_: Win condition config, capture the flag, admin win

**Capital fall**:
A **war aim** requiring **captured** chunks in the defender's **capital** subregion. The **war bill** names whether **majority** or **total** capture of that subregion satisfies the aim.
_Avoid_: Kill the king, throne break, monarch offline

**Capital**:
A kingdom's designated seat of government—a WorldGuard subregion the monarch sets within linked territory. Used for **capital fall** war aims; capture progress counts only chunks inside this subregion.
_Avoid_: Spawn point, home set, throne plugin

**Territory threshold**:
A **war aim** requiring the attacker to **capture** a named percentage of the defender's linked territory chunks during **siege**. Named in the **war bill**; satisfaction triggers **decisive victory** and may authorise **region merge** under an **annexation** outcome.
_Avoid_: Score limit, percent bar UI, faction power

**Annexation**:
A **decisive victory** outcome named in the war bill: captured enemy chunks are merged into the attacker's linked WorldGuard territory. Requires the war aim to be met first.
_Avoid_: Land claim, region steal, faction absorb

**War tribute**:
A **decisive victory** outcome named in the **war bill**: the defeated kingdom pays a configured Corona sum to the victor's treasury. Available balance transfers immediately on victory; any shortfall becomes **war debt** until cleared.
_Avoid_: Reparations fine, loot pool, war reparations command

**War debt**:
The unpaid remainder of an enacted **war tribute** after **decisive victory**. Owed by the defeated kingdom's treasury to the victor; persists across peace until paid or superseded by a later Act.
_Avoid_: Loan plugin, interest tick, credit score
