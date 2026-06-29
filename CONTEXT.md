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
A contest among seated player MPs to fill the Premier seat after a general election closes. MPs nominate and vote; the Speaker breaks ties for Premier as for MP seats.
_Avoid_: Leadership vote, prime-minister ballot, executive election

**Premier villager**:
When a general election seats no player MPs—the full villager parliament—the Premier role passes to a villager drawn from seated profession MPs only; **[MP] Citizen** backfill seats are not eligible. The villager from the profession with the highest count in the election scan is chosen; seat order breaks ties. The seated Premier villager displays a **[Premier]** nametag prefix in place of **[MP]** on that villager only, using the same prefix colour as a player Premier, followed by the profession label (e.g. **[Premier] Farmer**). On appointment after such a full villager general election, the realm automatically tables an **inaugural fiscal package** on the Premier villager's behalf. All bills follow the usual Westminster path: tabled in the Commons, realm-handled division (villager MP votes; no player Speaker), then royal assent in the Lords. Realm-handling covers Commons procedure only; once a bill passes division, the monarch must still grant or withhold assent manually in the Lords—there is no automatic or timed assent shortcut.
_Avoid_: Villager chancellor, NPC premier, profession premier

**Inaugural fiscal package**:
The pair of bills tabled automatically when a Premier villager is appointed after a full villager general election. Submitted in sequence on the Premier villager's behalf: first an **inaugural FISCAL bill**, then a BUDGET bill setting the approved spending cap to 50% of the kingdom's current treasury balance, rounded down.
_Avoid_: Starter budget, auto-fiscal, opening bills

**Inaugural FISCAL bill**:
The first bill in an inaugural fiscal package. Proposes fiscal rates adjusted from the kingdom's current enacted rates: each rate field—base tax, per-rank modifiers, foreign-income surcharge, and transfer fees—moves by exactly one percentage point according to the seated Premier villager's **profession vote bias** on FISCAL bills: aye lowers, nay raises, abstain leaves every field unchanged.
_Avoid_: Policy bill, rate shuffle, default fiscal

**Full villager parliament**:
A Commons where every seated MP is a villager and no player MPs hold seats. The Premier role passes to a Premier villager; fiscal bills are submitted on their behalf and proceed through realm-handled Commons divisions without a player Speaker. Bills that pass Commons still require the King or Queen to grant royal assent manually in the Lords; the realm does not assent on the monarch's behalf.
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
The kingdom legislature. Fiscal rates, treasury budget caps, and treasury spending pass as Acts through the House of Commons and receive royal assent in the House of Lords. When Commons is fully villager-seated, divisions are realm-handled without a player Speaker.
_Avoid_: Congress, senate, government

**House of Commons**:
The elected chamber where the Premier tables bills and Members of Parliament vote. Debate and divisions take place within the chamber bounds set by the monarch.
_Avoid_: Lower house (in player-facing text), assembly

**House of Lords**:
The chamber where the King or Queen grants or withholds royal assent on bills passed by the Commons. Assent and rejection both require the monarch to be present in the Lords.
_Avoid_: Upper house (in player-facing text), senate

**Act**:
A bill that has passed the Commons and received royal assent. Enacts fiscal rates, a budget cap, or an authorised treasury spend. Recorded as a written book in the registrar.
_Avoid_: Law, statute, decree

**Bill**:
A formal proposal before Parliament: fiscal rates, treasury budget, or treasury spend (including mint placement). Only one bill may be in progress per kingdom at a time. Under a full villager parliament, fiscal bills may be submitted on the Premier villager's behalf; they still table in the Commons and pass through realm-handled division, but royal assent in the Lords is unchanged—the monarch must be present and grant or withhold assent manually. No bill becomes an Act without that step, and there is no automatic or timed assent for villager-submitted bills.
_Avoid_: Proposal, motion, decree

**Division**:
A Commons vote on the bill before the House. The Speaker opens and closes the division; MPs vote aye, nay, or abstain while the division is open. When the Commons is fully villager-seated, the realm conducts division without a player Speaker—see **realm-handled division**. If aye and nay tie in a realm-handled division, the Premier villager breaks the tie in favour of passage via the government casting vote—see **casting vote**.
_Avoid_: Poll, ballot, referendum

**Realm-handled division**:
When Commons is fully villager-seated and no player MPs hold seats, division procedure is conducted by the realm without a player Speaker. Bills tabled on the Premier villager's behalf proceed through Commons: the division opens, villager MP votes apply, and the division closes once the vote is complete. If aye and nay tie, the Premier villager casts the government casting vote in favour of passage—the same tie-breaking role the Speaker holds when a player Speaker is present. Realm-handling does not extend to the Lords; a bill that passes division still awaits manual royal assent from the monarch.
_Avoid_: Auto-pass, script vote, NPC Speaker

**Casting vote**:
The tie-breaking vote when aye and nay are equal in a Commons division. When a player Speaker is present, the Speaker casts it—required before a tied division can pass or fail. In a realm-handled division with no player Speaker, the Premier villager casts the government casting vote in favour of passage.
_Avoid_: Tie-breaker, deciding vote

**Royal assent**:
The monarch's approval of a bill passed by the Commons, given in the House of Lords. Withholding assent rejects the bill without enacting it. Assent and rejection both require the King or Queen to be present in the Lords and to act manually; this applies equally to bills from a full villager parliament, including those tabled on the Premier villager's behalf after realm-handled division. There is no automatic assent, timed assent, or realm-handled assent shortcut—only the monarch may grant or withhold assent.
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
The Speaker's tie-breaking choice when two or more citizen candidates tie for the last available player MP seat in an election. Required before that election can close.
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
