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

**Public work**:
An authorised treasury spend that places one estate block—beacon, conduit, or lodestone—inside linked territory. The Premier prepares a site and type, tables a spend bill, and on royal assent the realm places the block (no survival materials) and debits the treasury by that type's Estate Corona worth, counting against the approved treasury budget. Realm wealth still counts the block only through the ordinary Estate scan; the spend does not register an estate by hand.
_Avoid_: Public works programme, estate commission, manual estate, government plot

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

**Villager strike**:
The visible pressure state on a frozen villager wallet after a configured strike threshold of in-game days, which must be shorter than villager wallet escheatment. While on strike the ordinary territory villager's nametag reads **[on strike]** and player Corona merchant trades and emerald villager trade taxation are refused; background villager trades are already idle under the freeze. The strike clears when the villager becomes productive again. Seated MP economic participants and Treasury Lords do not enter a strike.
_Avoid_: Labour dispute, walkout, frozen nametag, unproductive tag

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

**Tariff**:
An extra commerce surcharge, enacted as a FISCAL rate, applied on top of villager commerce tax when a player who is not a member of the territory kingdom completes a Corona merchant trade or emerald villager trade taxation there. Taken from the same gross as villager commerce tax—treasury takes commerce tax plus tariff; the traded villager's net falls accordingly; the player's trade price is unchanged. Same exclusions as villager commerce tax (Treasury Lords and seated MPs). Background villager-wallet settlements are out of scope. Member trades pay villager commerce tax only.
_Avoid_: Foreign commerce tax, visitor duty, import tax, customs

**Villager income tax**:
The kingdom base tax rate applied when villager GDP is credited to a villager wallet. Routed to the kingdom treasury. Noble rank discounts do not apply to villager income.
_Avoid_: Villager levy, GDP tax, profession tithe

**Villager wallet interest**:
A signed daily rate on productive villager wallets (including seated MP economic participants), enacted as a FISCAL rate. Positive credits from the kingdom treasury; negative charges the wallet into the treasury. Interest credits are not subject to villager income tax. Applied to each eligible wallet's balance after that day's villager GDP and villager trades have settled, and before villager wallet escheatment. When the treasury cannot cover the day's positive interest in full, each eligible wallet receives a pro-rata share of what remains. Negative interest cannot reduce a wallet below zero. Frozen villager wallets and player wallets are out of scope.
_Avoid_: Bank of Corona, bank interest, savings rate, storage fee

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
The noble who proposes a kingdom's fiscal rates: tax (base, per-rank modifiers, foreign-income surcharge), Corona transfer fees, villager wallet interest, and tariff. Rates take effect only after the King or Queen approves. One Premier slot per kingdom. Elected by seated player MPs after a general election; if no player MPs are seated, the monarch may appoint a Premier.
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
The first bill in an inaugural fiscal package. Proposes fiscal rates adjusted from the kingdom's current enacted rates: each rate field—base tax, per-rank modifiers, foreign-income surcharge, transfer fees, villager wallet interest, and tariff—moves by exactly one percentage point according to the seated Premier villager's **profession vote bias** on FISCAL bills: aye lowers, nay raises, abstain leaves every field unchanged.
_Avoid_: Policy bill, rate shuffle, default fiscal

**Full villager parliament**:
A Commons where every seated MP is a villager and no player MPs hold seats. The Premier role passes to a Premier villager and fiscal bills are submitted on their behalf; divisions run as in any other House, presided over by the Speaker, and close at once because no player MP is waiting to vote. Bills that pass Commons still require the King or Queen to grant royal assent manually in the Lords; the realm does not assent on the monarch's behalf.
_Avoid_: NPC parliament, all-villager chamber, automated legislature

**Tax proposal**:
A pending set of fiscal rates submitted by the Premier: tax rates, transfer fees, villager wallet interest, and tariff. Inactive until approved by the King or Queen.
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

**Kingdom flag**:
The banner that identifies the kingdom, flown as the Royal Standard one block east of the House of Lords. Set when the monarch runs `/kingdom parliament set lords` while holding a finished banner (consumed); otherwise the first raise defaults to Crown gold, and later empty-hand re-sets keep the stored design. Persisted with base colour and loom patterns; break is temporary until the next raise or Lords set; moving Lords clears the old banner block if it still holds a banner.
_Avoid_: Faction banner, custom map art, coat of arms GUI

**Act**:
A bill that has passed the Commons and received royal assent. Enacts fiscal rates, a budget cap, or an authorised treasury spend. Recorded as a written book in the registrar.
_Avoid_: Law, statute, decree

**Bill**:
A formal proposal before Parliament: fiscal rates, treasury budget, or treasury spend (including mint placement, stipend, and public work). Only one bill may be in progress per kingdom at a time. Under a full villager parliament, fiscal bills may be submitted on the Premier villager's behalf; they still table in the Commons and pass through division, but royal assent in the Lords is unchanged—the monarch must be present and grant or withhold assent manually. No bill becomes an Act without that step, and there is no automatic or timed assent for villager-submitted bills.
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
The archive where assented Acts and **Hansard** volumes are stored as written books on chiseled bookshelves. The monarch sets the anchor bookshelf; the registrar is that shelf and every face-adjacent chiseled bookshelf reachable from it (no diagonals). Further volumes fill slots and extend along that contiguous run.
_Avoid_: Archive, library, record office, Hansard shelf (the shelf is the Registrar; Hansard is what is shelved)

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
The ceremony in which the Crown opens a new session once a government has formed after a general election. The Crown summons the realm to the House of Lords — members of the kingdom together with Parliament's villager members, the Speaker and the seated profession MPs among them — then declares Parliament open from within the chamber. The realm forms up in ranks before the throne, facing it, rather than crowding around the Crown. The Speaker then crosses to the Bar of the House and reads the return of the Commons a member at a time, each sentence hanging above them as it is spoken; the House rises and everyone is returned whence they came once the reading ends. Until it happens, Parliament remains prorogued.
_Avoid_: Inauguration, swearing-in, coronation

**Speech from the Throne**:
The summons delivered to the King or Queen — or to the heir acting as regent when no monarch is seated — when a session awaits opening. Right-clicking it opens the State Opening interface: summon the realm, then declare Parliament open. Redelivered on login if missing, and removed once the session opens.
_Avoid_: Royal decree, king's speech scroll, opening address book

**Bar of the House**:
Where the Speaker stands to address the Crown with the return of the Commons, set by the monarch with `/kingdom parliament set bar`. The Speaker crosses to it when the reading begins and resumes their place when it ends. Where none is set, the Speaker reads from wherever they stand.
_Avoid_: Podium, lectern, stage

**Return of the Commons**:
The Speaker's roll-call of who was elected to each bench and by what margin, read to the realm's members at every State Opening — including one opened by royal commission. Player MPs are ranked by their votes and profession MPs by the size of their constituency, but the two are never ranked against one another: a vote and a villager are not the same unit. A by-election is followed by the return of that single seat.
_Avoid_: Election results, vote report, results announcement

**Constituency size**:
The number of territory villagers of a profession at the general election that returned its MP. It is the profession MP's counterpart to a player MP's vote tally, and is what the Speaker reads out for a villager bench.
_Avoid_: Villager votes, profession votes

**Unopposed return**:
A seat returned without any count behind it: a Citizen backfilling an empty bench, or a seat filled before returns were recorded. The Speaker names the member but reads no number, since nobody contested the seat.
_Avoid_: Zero votes, uncontested vote

**Royal commission**:
The fallback that opens a session without ceremony: used when the kingdom has no House of Lords set, or when neither monarch nor regent has opened Parliament within three in-game days of the government forming. Parliament opens by announcement alone — no summons, no teleport — so the realm's business is never frozen by an absent Crown.
_Avoid_: Auto-open, timeout open, forced opening

**Motion**:
Business before the House that settles a question rather than making law. A motion is tabled, divided upon, and decided in the Commons alone: it never travels to the House of Lords and never becomes an Act. Motions share the order paper with bills, so a kingdom may have only one piece of business — motion or bill — before the House at a time.
_Avoid_: Non-binding bill, resolution, proposal

**Motion of no confidence**:
A motion asking the Commons whether it still supports the Premier. Tabled by a seated player MP and requiring a **seconder** before the division opens; neither may be the Premier. Carrying the motion removes the Premier at once — a villager Premier is dismissed and released to the territory as any villager MP is — and opens a Premier election. Parliament is not prorogued and no State Opening follows: the government changes, the session continues. A motion that fails begins the **confidence cooldown**.
_Avoid_: Impeachment, vote of censure, recall

**Seconder**:
The second seated player MP who must confirm a motion of no confidence before it reaches division. One member's grievance is not the House's question. Where only one player MP is seated the motion is unavailable, there being no confidence question a single-member House can put.
_Avoid_: Co-signer, backer, sponsor

**Confidence cooldown**:
The period in in-game days after a failed motion of no confidence during which no further such motion may be tabled in that kingdom. It binds the whole House, not only those who signed the failed motion, so that a rotating handful of members cannot hold the order paper hostage.
_Avoid_: Motion timeout, no-confidence lockout, spam guard

**Referendum**:
A question put to every member of the realm rather than to the eight seats of the Commons. Called by the Premier or by the Crown, and advisory: the realm's answer is proclaimed and recorded in **Hansard**, but enacts nothing by itself. A Premier who disregards the realm's answer answers to the Commons, not to the plugin. Members vote wherever they stand — a referendum is not a division and does not sit in the chamber.
_Avoid_: Plebiscite, poll, binding vote, division

**Polling window**:
The span in in-game days during which a referendum accepts ballots. Members are prompted on login while it remains open, and the Premier may close polling early. The referendum holds the order paper for its whole window: no other business comes before the House until it closes.
_Avoid_: Voting period, election window, ballot timer

**Turnout**:
The count of members who voted in a referendum set against the number entitled to. Proclaimed with every result, so the realm may weigh a thin answer for itself. There is no quorum: a referendum is never void for want of voters.
_Avoid_: Participation rate, quorum, validity threshold

**Manifesto**:
A single line a candidate writes on nomination, shown beside their name in the election and read out with the result. It is a promise made to the realm, not a term the plugin enforces: nothing checks a manifesto against how its author later votes.
_Avoid_: Pledge, platform, campaign promise (as a tracked commitment)

**Party**:
A name and colour a candidate chooses on nomination, under which divisions are tallied. Parties are declared, not registered: any candidate may stand under any name, and members coordinate their own. Villager MPs stand not under a party but under their **profession bloc**.
_Avoid_: Faction, caucus, registered party

**Profession bloc**:
How villager MPs are grouped in a division tally — by the profession that returned them rather than by any party, which they neither choose nor join. A player cannot stand for a profession bloc, nor a villager for a party.
_Avoid_: Villager party, profession party

**Hansard**:
The bound record of a parliamentary session: every division, its tally by party and profession bloc, and every referendum result. Written to the registrar as a book at prorogation, one volume per Parliament, from records kept as each division closes.
_Avoid_: Log, minutes, transcript

**Questions to the Premier**:
A window the villager Speaker calls at intervals through a session, inviting the realm to put questions to the Premier. Ceremony alone: the Speaker announces it, the House speaks, and nothing is recorded or tallied. Called only while a Premier is seated and the session is open.
_Avoid_: PMQs (in player-facing text), question time, debate session

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
A constable's formal application to pursue a named suspect—player or territory villager. Inactive until the Crown approves it; until then patrol golems and constables may not act on it. The villager Speaker cannot be named.
_Avoid_: Bounty, hit list, detention order

**Royal warrant approval**:
The Crown's acceptance or rejection of a warrant application, using the same paper-and-review workflow as a resignation letter. On approval the warrant becomes active; on rejection it does not. Does not apply to a **flagrant warrant**.
_Avoid_: Instant warrant, auto-approve, chat approval

**Flagrant warrant**:
An **active warrant** opened at once for witnessed **assault on the Crown**, without **royal warrant approval**. The ordinary constable application still needs Crown paper.
_Avoid_: Auto-approve every warrant, skip trial

**Active warrant**:
An approved warrant, or a **flagrant warrant**, that authorises constables and patrol golems to arrest the named suspect inside kingdom territory.
_Avoid_: Open case, manhunt tag

**Wanted nametag**:
A red `[WANTED]` nametag prefix shown on a player while they have an active warrant and are physically inside that kingdom's linked territory (jurisdiction). It replaces noble and sworn-role prefixes for that display; those return when the mark clears or the player leaves the territory. Hidden outside that territory; restored on re-entry. Cleared when the warrant is served, cancelled, or rejected. Not applied to villagers.
_Avoid_: Wanted flag, wanted stars, glow red, manhunt tag, server-wide shame tag, stacked wanted-plus-rank prefix, villager wanted nametag

**Arrest reward**:
A Corona purse attached to an active warrant, posted at the court from a kingdom member's player wallet into escrow (the poster may top up the same purse), and paid to the arresting constable's wallet on arrest. A patrol-golem arrest refunds the purse to the poster. Cancelling or rejecting the warrant path that leaves no arrest also refunds the poster. Distinct from the warrant itself and from a fine sentence. Not funded from the kingdom treasury.
_Avoid_: Bounty, bounty board, hit purse, warrant prize

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
The King, Queen, and Prince cannot be subject to a warrant or arrest under kingdom police law. The villager Speaker likewise cannot be warranted or arrested.
_Avoid_: Royal exemption, crown privilege, diplomatic immunity

**Arrest**:
Taking a suspect with an active warrant into custody and opening a pending trial. Suspects may be players or territory villagers (claimed economy villagers and seated MP or Premier villagers). Constables arrest manually; patrol golems detain automatically inside territory, including immediately after a **flagrant warrant** for **assault on the Crown**. A player judge is chosen at random from online judges, excluding the accused when the accused is a player, the arresting constable, and the Crown who approved the warrant; if none qualify, a trial jury is seated when possible, otherwise the villager judge hears the case.
_Avoid_: Ban, kick, instant jail

**Pending trial**:
The state between arrest and verdict. A player judge, trial jury, or realm-handled villager judge must adjudicate before a sentence takes effect. The accused is not confined for being pending alone—prison hardening begins only on a prison sentence.
_Avoid_: Pre-trial hold, limbo, cooldown, custody-as-guilt

**Trial**:
The hearing before a player judge, trial jury, or villager judge. Verdict options: guilty with a prison sentence, guilty with a fine paid to the treasury, a formal warning (record only), or not guilty.
_Avoid_: Hearing, prosecution, court session

**Prison sentence**:
A guilty verdict that confines the convict to an assigned numbered cell for a configured real-world duration (presets of five, fifteen, thirty, or sixty minutes). On sentence a player convict is teleported to the cell, their spawn is set to that cell, and all teleports are barred for the duration—including kingdom checkpoints, `/tp`, and ceremony summons (State Opening and like). On release, the prior bed or spawn saved at sentence start is restored; if none, world spawn. A villager convict is moved to the cell for the duration and does not trade or earn GDP while confined; on release they return to productive territory life. Anyone under an active prison sentence is ineligible for Parliament. Elected offices (player or villager MP, Premier, player Speaker) are vacated immediately and trigger the normal Premier election or Commons by-election—no resignation-letter approval step. Appointed noble titles and sworn roles on a player are suspended for the duration and restored exactly on release. Hard confinement also returns a player convict to the cell if they move more than eight blocks away. No sentence removes a player from the server whitelist. Crown and Speaker **immunity** still block warrant and arrest of King, Queen, Prince, and the villager Speaker.
_Avoid_: Temp ban, mute sentence, soft jail, whitelist exile, exile kick, permanent attainder by prison alone, seat held through prison, spawn left on cell after release, warrantable Speaker

**Fine sentence**:
A guilty verdict that levies Corona from the convicted player to the kingdom treasury without imprisonment.
_Avoid_: Bounty payment, damages, restitution order

**Warning**:
A guilty verdict that records the offence without prison time or a treasury fine.
_Avoid_: Caution, slap on the wrist, strike

**Realm-handled trial**:
A trial conducted by the villager judge at the court when no eligible player judge is online and a trial jury cannot be seated (fewer than three eligible online members). The villager judge returns a weighted random verdict from the standard options.
_Avoid_: Auto-conviction, script trial, NPC prosecution

**Trial jury**:
Three randomly chosen online kingdom members who vote guilty or not guilty when no eligible player judge is available and at least three eligible members are online. Seated automatically on arrest or patrol detain when those conditions hold; otherwise the case falls straight to a realm-handled trial. Jurors vote from anywhere via a secret ballot GUI opened on seating—no court presence required; the ballot shows the accused and time remaining, not how others voted. A seated juror may reopen their ballot with `/kingdom police jury` while the window is open. On seating, each juror receives a private prompt and the kingdom hears a short notice that a jury is seated; on close, the kingdom hears the outcome and sentence (or that the window timed out into a realm-handled trial), never individual ballots. The pool excludes the accused, the arresting constable, the Crown who approved the warrant, and sworn Judges. Replaces the realm-handled trial for that case; does not replace a player judge hearing. Once seated, the jury runs to completion or timeout even if an eligible player Judge later comes online. Each juror must vote; majority (two of three) decides. Not guilty yields acquittal; guilty draws the sentence from the same weighted realm-handled sentence table the villager judge uses. If fewer than three eligible members are online, or the jury window expires without a full set of votes (including when a seated juror disconnects and never votes), the case falls back to a realm-handled trial. Jury seating is not persisted. Open police cases are also memory-only today, so a full restart clears pending trials; when pending trials later survive reload, any still-open case with no live jury is routed through hearing resolution again (await Judge, re-seat a jury, or realm-handle).
_Avoid_: Peer court, mob vote, public poll, jury sentencing panel, hung jury softlock, lectern-gated jury seating, chamber-required jury vote, live jury tally, mid-window juror replacement, jury roll-call, judge seizure of a live jury, persisted in-flight jury ballots

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
Restoring **loyalty tier** after a **political offence** without a pardon. One tier per configured number of in-game days without further offence, up to **Faithful**. **Service credit** may shorten that wait but never skips a tier. **Traitor** cannot recover by time alone.
_Avoid_: Play time reward, login streak, good boy points

**Service credit**:
An **act of service** shortening the current **recovery clock** by moving its marked start day back a configured number of in-game days. It never grants a tier directly, never stacks past the next tick, and never applies to **Traitor** or **Rout**, whose clocks do not run. The only earn-back lever a subject holds; the crown's lever remains the **loyalty pardon**.
_Avoid_: Loyalty points, XP grind, redeemable tokens, daily quest

**Act of service**:
The qualifying deed that yields **service credit**: on the political track, paying **income tax** while below **Faithful**; on the military track, answering a **muster** and serving it out without a **morale breach**. One per track — the obligation the subject broke is the one that mends it.
_Avoid_: Fetch quest, chore list, errand, side mission

**Recovery clock**:
The persisted per-subject mark — tier plus the in-game day the wait began — driving **loyalty recovery** and **morale recovery**. Restarts whenever the marked tier no longer matches the subject's current tier, so any fresh offence resets the wait. Survives restart; visible to the subject in the **loyalty ledger**.
_Avoid_: Cooldown bar, timer buff, respawn timer

**Loyalty ledger**:
The subject's own view of both tracks — current tier, what lowered it, in-game days to the next recovery tick, **service credit** applied, and the one act that would help next. Read-only: it reports the domain, it never changes it.
_Avoid_: Reputation screen, karma meter, loyalty stars UI, scoreboard

**Realm board**:
The scoreboard at a subject's right hand while they stand on a kingdom's linked territory: whose land it is, the season, and their purse. On **home soil** it also shows one standing line whose heading follows the track — **Loyalty** and the **loyalty tier** while the **military morale** track is closed, **Morale** and the **morale tier** once that track is open. Off home soil the board stays up truncated: land, season, purse, no standing. Peace does not invent a third meter; a **civilian member** still has no military track.
_Avoid_: Sidebar, tab list, boss bar, State of the Realm, kingdom morale average

**Home soil**:
Linked territory of the kingdom that holds the player's **fealty** — membership, or a **sworn outsider** bind. A guest with neither sees no standing line.
_Avoid_: Home chunk, spawn, capital, claimed land

**Loyalty pardon**:
The monarch restoring a subject's political loyalty at court. Returns tier to **Faithful**, or to **Doubtful** after **Traitor** if the crown chooses partial restoration. Required to clear **Traitor** without acquittal.
_Avoid_: Unban command, forgive keystroke, amnesty button

**Loyalty penalty**:
The civil effects of **loyalty tier** beyond office bars. **Doubtful** subjects are flagged for constable scrutiny; **Disloyal** subjects become warrant-eligible on further **political offences**; **Traitor** subjects may be arrested on sight inside **jurisdiction** when a warrant is active or on fresh treason report. No noble **loyalty immunity**—automatic tier drops apply to all fealty subjects; monarch **warrant immunity** under police law is separate.
_Avoid_: Chat mute, glow effect, loyalty stars UI

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
Restoring **morale tier** after a breach without ending the war. Honourable service in an active **siege** slowly raises tier one step per in-game day without further breach, up to **Steadfast**; **service credit** may shorten that wait. **Rout** requires **morale pardon** before the subject may muster again.
_Avoid_: Sleep to heal, eat food buff, passive regen

**Morale pardon**:
The crown or an appointed knight restoring a fealty subject's military morale at a muster point or court. Returns tier to **Steadfast**; required to clear **Rout** before the next levy duty.
_Avoid_: Admin unban, debuff clear command, forgive button

**Morale penalty**:
The in-game effects of **morale tier** on a fealty subject. **Shaken** and **Breaking** apply scaling potion debuffs and limit **squad** command; **Rout** applies severe debuffs, blocks levy duty, and disables squad command until **morale pardon**.
_Avoid_: Custom weakness mod, damage multiplier UI, hunger punishment

**Treason**:
A political offence against the crown—swearing fealty then aiding an enemy kingdom, defying enacted Acts, desertion so severe it breaches oath, or **assault on the Crown**. Handled through the police and court pipeline, not morale debuffs alone. Loyalty **Traitor** applies on **conviction**, not on the hit.
_Avoid_: Ban reason, griefer tag, karma hit

**Assault on the Crown**:
Player-attributed damage (melee, arrow, trident, potion, or pet) against that kingdom's King, Queen, Prince, or Princess inside **jurisdiction**, witnessed by a **patrol golem** within thirty-two blocks of the victim. Opens a **flagrant warrant** and an immediate patrol **arrest** into **pending trial**. Unwitnessed hits, hits outside jurisdiction, hits when police infrastructure is not ready, and hits by a King, Queen, or Prince are not a police offence. Vanilla damage is not cancelled under **open PvP**.
_Avoid_: Safe zone, PvP off, monarch god mode, guard-golem witness

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

## Calendar

**Realm calendar**:
The realm's own reckoning of time: twelve months of thirty days, three hundred and sixty days to the year, derived from the world clock rather than kept as a separate counter. Read with `/kingdom date` and bound into the **almanac**.
_Avoid_: Real-world date, server uptime, Minecraft day number

**Realm day**:
The canonical unit of realm time: the in-game day counted from the **calendar epoch**. Never runs backwards, so records keep their order even where an operator sets the world clock back.
_Avoid_: Tick, in-game day, world full time

**Calendar epoch**:
The world day the realm calendar was first pinned, stored once in `data.yml`. Every **realm day** is the world day less this epoch.
_Avoid_: Server start date, world seed date

**Realm Year**:
The server-wide year of the realm calendar, counted from the **calendar epoch** and shared by every kingdom. All scheduling and all sorting of records run on it.
_Avoid_: Regnal year, season, calendar year

**Month**:
One twelfth of the realm year, thirty days long. The twelve are Frostwane, Thawtide, Seedfall, Blossoming, Highmead, Sunwake, Harvest, Goldfall, Emberwane, Hallowtide, Longnight and Yulewatch. Fixed realm vocabulary, never configurable.
_Avoid_: Real-world month names, week, fortnight

**Regnal year**:
The ceremonial year of a monarch's reign, beginning at their coronation and turning on each **accession anniversary**. Runs out of phase with the **Realm Year**, which alone governs scheduling.
_Avoid_: Realm Year, term of office, session

**Accession anniversary**:
The realm day three hundred and sixty days after a monarch's accession, and every three hundred and sixty days thereafter. The realm is told that the kingdom enters the next **regnal year** of that reign.
_Avoid_: Coronation day, jubilee, birthday

**Reign record**:
One monarch's entry in a kingdom's **roll of monarchs**: who reigned, under what style and ordinal, and between which realm days. At most one reign is left open at a time.
_Avoid_: Title assignment, membership record

**Roll of monarchs**:
A kingdom's ordered reign records, oldest first. The source of a monarch's regnal ordinal — the second King Leo of that kingdom reigns as King Leo II.
_Avoid_: Member list, noble roster

**Interregnum**:
Any stretch of realm days with no monarch seated in a kingdom. Dates fall back to the **Realm Year** alone and the regnal style is suspended; the gap is left unattributed rather than assigned to the late monarch.
_Avoid_: Regency, vacancy, caretaker reign

**Polling day**:
The realm date each realm year on which the writ for a general election is issued. Set in config by month and day; the writ still issues on the first day after it should the server have been down when it came round, but never twice in one realm year.
_Avoid_: Election interval, term length, dissolution date

**Almanac**:
A written book drawn up by `/kingdom almanac` recording the realm date, the twelve months and the **season** each belongs to, the season now in force and what it asks of the realm, the next **polling day** and the kingdom's **roll of monarchs**.
_Avoid_: Hansard, order paper, register

## Season

**Season**:
One quarter of the realm year: three whole **months**, server-wide, the same for every kingdom. Spring is Frostwane, Thawtide and Seedfall; summer is Blossoming, Highmead and Sunwake; autumn is Harvest, Goldfall and Emberwane; winter is Hallowtide, Longnight and Yulewatch. Derived from the **realm day** and never stored.
_Avoid_: Weather, biome, Realm Year, quarter

**Season profile**:
What a season asks of the realm and grants it, held as one set of figures the whole plugin reads: how fast crops come on, what the fields and workshops yield, what the levy costs to keep, how thickly the hostile dark spawns, whether hearths must burn, how slowly a soldier's **morale** mends and how fast it wears away in the field. Tunable in config; the seasons themselves are not.
_Avoid_: Modifier table, multiplier, difficulty setting

**Season turn**:
The first day of a season, on which the realm is told what has come upon it. Announced once, whatever the hour, and hung on every kingdom's **Gazette** so the **town crier** cries it thereafter for those who were not about when the word first went out. Authored by the realm, not by the Crown.
_Avoid_: Solstice, equinox, weather change

**Outdoor profession**:
A villager trade worked in the open, whose yield swings with the **season** — richest in summer and autumn, poorest in winter. Set against the indoor trades, which are worked under a roof and yield the same the year round but still need a **hearth** to keep warm.
_Avoid_: Farmer, tier, profession rate

**Hearth**:
A lit campfire inside a kingdom's territory with a container set against it — face to face, never on the diagonal — which burns a day's ration of coal or logs out of that container each winter day to keep the villagers within reach of it warm. It burns the whole ration or none: a container short of it keeps what it has and warms nobody. Not a placed or crafted thing of the plugin's own: any campfire so furnished is a hearth, found by sweeping the territory rather than recorded.
_Avoid_: Furnace, brazier, heater, warmth block

**Cold**:
The state of a villager left through a winter day beyond the reach of a burning **hearth**. Cold bites by degrees: from the first day it costs the villager part of its yield, and by the third it goes on **strike** as any unpaid villager does, told the same way on its nametag. A day's warmth wipes the slate. Seated villager MPs, Lords of the Treasury and the Town Crier never strike, cold or not. The run of cold days behind each villager is the only thing the hearths leave on disk.
_Avoid_: Freezing, frostbite, damage

**Levy upkeep**:
The daily charge on the treasury for keeping men under arms: a standing rate for the **standing roster** and a dearer one for those who answered a **muster**, both moved by the **season**. Winter is dearest, summer cheapest.
_Avoid_: Stipend, wage, tax, budget

**Arrears**:
Levy upkeep a treasury could not meet. The realm is warned the day the levy first goes unpaid; while arrears stand, the unpaid lose **morale**, and those whose morale sinks far enough **desert**.
_Avoid_: Debt, deficit, overdraft

**Desertion**:
The loss of a soldier from the **standing roster** for want of pay. Not a punishment and not reversible by paying up: the deserter must be raised again.
_Avoid_: Discharge, demobilisation, dismissal

**Field morale decay**:
The steady loss of **morale** by men a kingdom keeps under arms in a war through a hard **season**, one step every few realm days whatever else befalls them. Winter alone bites by default. It falls on the same ladder as **arrears** do, so an army both in the field and unpaid loses heart from both causes at once.
_Avoid_: Attrition damage, siege timer, exhaustion

**Campaign season**:
Summer and autumn together, when the levy is cheap, the fields are full and morale mends quickly — the fit time to take the field. Set against winter, when a war fought is a war the treasury and the harvest both pay for.
_Avoid_: War window, muster period, ceasefire

**Winter censure**:
The political price a **Premier** pays for taking the realm to war, or sending it to the country, in winter: their own political loyalty falls a step, the grievance is entered in Hansard, and the realm is told. It lands on the Premier alone and never on their subjects, and it tables nothing — a **motion of no confidence** still needs two seated MPs to choose it between them.
_Avoid_: Automatic no confidence, winter penalty, censure motion

## City

**Member**:
A player who belongs to one kingdom. Created by the **oath of allegiance**, or by an operator moving them. Relative to that kingdom they are not a **foreigner**.
_Avoid_: Citizen, subject, resident, national

**Oath of allegiance**:
The civil ceremony, administered by the **lord mayor** at **city hall**, by which a player who belongs to no kingdom pledges loyalty to **the Crown** of that kingdom and becomes a **member**. They affirm a written oath that names the seated **King or Queen** when one holds the throne; if the throne is vacant it is sworn to the Crown as office. The monarch need not be present. Completing the oath is announced to online members of the kingdom and marked at city hall by a brief spectacle. A kingdom with no **capital** has no hall and cannot administer the oath. Distinct from the **oath of service**, which binds military obligation without granting membership.
_Avoid_: Naturalisation, citizen enrolment, join command, loyalty oath, oath of service

**Oath book**:
The signed written book given to a new **member** when they complete the **oath of allegiance**. A keepsake of the oath they affirmed; it grants no office, permit, or further right.
_Avoid_: Citizenship papers, membership card, naturalisation certificate

**Capital**:
The single point a monarch designates as the seat of their kingdom, set with `/kingdom capital set` from inside the kingdom's linked territory and removed with `/kingdom capital clear`. Setting it again moves the seat. The capital is the **city hall** and the place the **lord mayor** stands; a kingdom with no capital issues no **build permits** and is not gated at all.
_Avoid_: Home, spawn, capital city, seat of government

**City hall**:
The civic office at the **capital** where the **oath of allegiance** is sworn and **build permits** are applied for. Not a building the plugin places—it is the capital point itself, and the monarch is free to raise a hall around it.
_Avoid_: Town hall, mayor's office, civic centre, guild hall

**Lord Mayor**:
The realm NPC that administers the **oath of allegiance** and issues **build permits**, standing at the **city hall**: a tamed wolf, seated, invulnerable, without AI, showing a **[Lord Mayor]** nametag. Spawned by `/kingdom capital set`, removed by `/kingdom capital clear`, and respawned by the periodic sweep if it goes missing. Holds no wallet, trades with nobody, is never claimed as an **MP**, and takes no part in the villager economy.
_Avoid_: Mayor NPC, town crier, clerk, magistrate

**Build permit**:
The licence a player must hold to place or break blocks inside a kingdom's linked territory. Free, granted on the spot by the **lord mayor**, kingdom-wide, and persisted in `data.yml`. Held only by members of that kingdom; **foreigners** cannot obtain one. Revoked by a **prison sentence**, by leaving the kingdom, and by the monarch through `/kingdom permit revoke`.
_Avoid_: Build rights, build flag, planning permission, land claim

**Horse permit**:
The licence a **horse** stands on, entered on the realm's register the moment a member puts a saddle on it and shown beside their **build permit** in the permit register. It names one horse and one holder; saddling a horse afresh moves the permit to whoever saddled it last. Members only — a player in no kingdom claims nothing. Struck off when the horse dies, when the holder leaves the kingdom, and when the Crown revokes the holder's build permit in the register.
_Avoid_: Horse deed, mount licence, stable claim, horse tag

**Horse gate**:
Who may ride a permitted horse, open its saddlebags or lead it: its holder, and the **King, Queen or Prince** of the holder's realm. Everybody else is refused, operators and foreign royalty included; the refusal is throttled to roughly one message every thirty seconds. A horse on no register is anybody's, as vanilla has it.
_Avoid_: Horse lock, mount protection, anti-theft

**Permit exemption**:
The standing right of the **King or Queen** and the **Princes or Princesses** to build inside **their own** kingdom without a **build permit**. It does not travel: royalty in another realm is a **foreigner** and cannot build there at all. Operators are not exempt from this gate, unlike the **build-ban Act** gate.
_Avoid_: Admin bypass, OP override, royal decree

**Foreigner**:
Relative to a kingdom, a player who is not one of its members. A foreigner may not hold that kingdom's **build permit** and so may not build in its territory, whatever rank they hold at home.
_Avoid_: Outsider, guest, visitor, non-citizen

**Permit register**:
The paginated roll of a kingdom's **build permit** holders, opened by the **King or Queen** or a **Prince or Princess** right-clicking the **lord mayor**. Each holder appears as a player head; selecting one offers revocation.
_Avoid_: Permit list, whitelist, roster

**Unlicensed building**:
An attempt to place or break a block inside linked territory without a **build permit**. The action is refused and the player told why, at most once in a short interval. It is not a crime: no **warrant** is opened and no **political loyalty** is lost, unlike a **build-ban Act** breach.
_Avoid_: Griefing, trespass, illegal build, build offence

**Town Crier**:
The realm NPC that keeps the **Gazette**: a nitwit villager, invulnerable, without AI, showing a **Town Crier** nametag. Spawned when the **capital** is set (defaulting to the capital block), may be stood elsewhere in the kingdom's territory with `/kingdom crier set`, returned to the capital with `/kingdom crier clear`, removed when the capital is cleared, and respawned by the periodic sweep if it goes missing. Holds no wallet, trades with nobody, is never claimed as an **MP**, and takes no part in the villager economy. A text display above its head cycles the newest Crown posts when a player is nearby.
_Avoid_: Herald, messenger, news NPC, bulletin board

**Gazette**:
The kingdom's notice board, read by right-clicking the **Town Crier**. It shows Crown-authored **announcements** and **decrees** (persisted) together with live realm state computed when the board is opened — open bills, the next election, the wanted list, permit count, and the treasury — never stored.
_Avoid_: Newspaper, news feed, event log, bulletin

**Announcement**:
A Crown post to the **Gazette** that informs the realm. Cap of twenty; the oldest drops when a new one is pinned. It does not enter **Hansard**, does not broadcast, and carries no mechanics.
_Avoid_: Notice, news item, proclamation (prefer **decree** for binding Crown acts)

**Decree**:
A binding Crown act published to the **Gazette**, entered in **Hansard**, and announced once to online members. A decree may set, change, or lift the kingdom's **curfew**; no other mechanical effects travel with it.
_Avoid_: Proclamation, edict, executive order, Act of Parliament

**Curfew**:
The hours, in Minecraft day ticks, during which being abroad in a kingdom's linked territory is an offence. Set per kingdom by **decree** (with named presets), falling back to the plugin's `enforcement.curfew` defaults when no decree is in force. A lift decree turns enforcement off. Breach opens a **warrant** application through the ordinary Crown-approval pipeline. The **King or Queen** and **Princes or Princesses** of that kingdom are immune; operators are not.
_Avoid_: Lockdown, night ban, movement ban, teleport home

**Sitting day**:
An even **realm day** on which the Commons sits. Divisions open only on sitting days. Ordinary **villager MPs** attend Parliament; the **Premier** villager and **villager Speaker** attend every day Parliament is in session.
_Avoid_: Session day, sitting week, parliamentary day

**Recess**:
An odd **realm day** when the Commons does not sit. Ordinary **villager MPs** return to their professions (keeping the **[MP]** nametag and territory despawn protection). Bills tabled in recess wait on the order paper until the next sitting day. While Parliament is **prorogued** there are no sitting days: villager MPs work every day until State Opening.
_Avoid_: Holiday, adjournment, vacation, break

**Villager jury**:
A trial jury of three territory villagers claimed when fewer than three eligible players are online. They are released to their stored origins when the trial ends. The verdict still comes from the realm-handled sentence table; the spectacle is bodies in seats, sound, and the trial bar. Excludes the accused (if a villager), seated MPs and the Premier, the Speaker, Treasury Lords, the villager judge, the **Town Crier**, and striking villagers. Fewer than three eligible villagers falls to the villager judge alone.
_Avoid_: NPC jury, fake jury, auto jury

## Granary

**Granary**:
The place a kingdom keeps its grain against the winter: a **granary region** the Crown links inside its own territory, which fills with hay through the growing year and is drawn down through winter. One to a kingdom. A kingdom that has sited none is fed exactly as one whose granary stands empty.
_Avoid_: Silo, barn, warehouse, food store, stockpile

**Granary region**:
The WorldGuard region a **granary** occupies, linked by the **King or Queen** with `/kingdom granary setregion` and released with `/kingdom granary clear`. It must lie within the kingdom's linked territory. Nothing of the region is kept but its name.
_Avoid_: Granary block, granary plot, claim, zone

**Stock**:
The hay standing in a **granary region**, counted off the blocks whenever it is asked for and never written down. New bales are laid on the lowest course first and drawn from the top, so the store visibly rises through the year and sinks through winter.
_Avoid_: Balance, inventory, reserve, count

**Capacity**:
Whatever air the builders left inside the **granary region**. A granary with no room takes no more grain and the realm is told; a realm that wants to store more builds more.
_Avoid_: Limit, cap, max stock, tier

**Harvest tally**:
The day's grain off the realm's fields: a count of the farmer-profession villagers in territory, each credited wheat at a rate moved by the **season profile**'s outdoor yield — richest in Harvest, nothing in winter. Every nine wheat lays one bale of hay in the **granary**; the remainder under nine waits on disk for the next day. Hay placed by hand and loose wheat left in the region count the same.
_Avoid_: Farm output, crop yield, production, GDP

**Winter ration**:
The bales drawn out of the **granary** on each of the ninety winter days: one for every so many territory villagers, rounded up. Drawn in winter alone. Players are never fed from it — they keep vanilla hunger.
_Avoid_: Upkeep, food cost, consumption, feeding

**Shortfall**:
How many bales a kingdom stands short of seeing the winter through at its present head-count. Cried by the **town crier** and hung on the **Gazette** on the **season turn** into Harvest and again on the last day of autumn, so a realm is warned twice before it starves.
_Avoid_: Deficit, arrears, debt, warning

**Hunger**:
The state of a villager on a winter day whose kingdom could not draw its **winter ration**. It bites by degrees, on a ledger of its own kept beside **cold**: the first day costs part of its yield, the third sends it on **strike**, and from the seventh it may **starve**. A fed day wipes the slate. Cold and hunger both bite at once — their yield cuts multiply, either alone calls a strike — and the nametag shows the worse of the two.
_Avoid_: Famine, malnutrition, food debuff, saturation

**Starvation**:
The death of one villager a day, chosen at random from those left **hungry** seven days or more. Seated **villager MPs**, the **Premier** villager, the **villager Speaker**, **Lords of the Treasury** and the **Town Crier** are spared, as they are spared the strike.
_Avoid_: Culling, despawn, population decay

**Famine grievance**:
The political price of leaving the realm unfed: while villagers starve, subject loyalty falls a step, the grievance is entered in **Hansard** and the realm is told. It tables nothing — a **motion of no confidence** still needs two seated MPs to choose it — on the pattern of **winter censure**.
_Avoid_: Famine penalty, unrest, riot, automatic no confidence

**Grain theft**:
Breaking hay out of a **granary region** by any hand but the **King or Queen** or a **Prince or Princess** of that kingdom. Members, holders of a **build permit** and operators alike commit it. Seen by the plugin itself, so no witness is wanted; it opens an ordinary **warrant** through the Crown-approval pipeline and is never **flagrant**.
_Avoid_: Looting, plunder, raiding, stealing food

## Church

**Church**:
The place a kingdom holds its **rite**s: a point the **King or Queen** sets inside linked territory with `/kingdom church set` and clears with `/kingdom church clear`. One to a kingdom. It does nothing until **consecrated**, and a kingdom that has sited none has no religion at all — no rites, no **coronation gate**.
_Avoid_: Temple, cathedral, shrine, chapel, altar

**Consecration**:
The **rite** that brings a **church** into use. Performed once at the church point by the **priest** or the **cleric**. An unconsecrated church refuses every other rite. Setting a new church point unconsecrates it.
_Avoid_: Activation, dedication, blessing the church, enabling

**Priest**:
A sworn office of the realm, one to a kingdom, sworn and unsworn by the **King or Queen**. Exclusive of **constable** and **judge**. Suspended by a prison sentence and restored on release. Wears a bold `[Priest]` prefix in chat, tab and nametag, and is listed in `/kingdom info` beside the police. Performs every **rite** and keeps the **tithe**.
_Avoid_: Bishop, cleric (the villager), chaplain, monk

**Cleric**:
The villager who presides at the **church** while the **priest**'s seat is empty or its holder is in a cell. Spawned fresh at the church point on the pattern of the **villager Speaker**, cleric profession, `[Cleric]` nametag, never claimed off the territory and never given a **villager wallet**. Reconciled on startup and by the 60-second territory sweep, and despatched when a player is sworn priest. Its **tithe** goes to the **kingdom treasury**.
_Avoid_: Villager priest, NPC priest, acolyte

**Rite**:
A ceremony the **priest** or **cleric** performs in person at a **consecrated** **church**: **mass**, **marriage**, **divorce**, **funeral**, **coronation** or **consecration**. There is no faith score behind them — a rite is worth holding for what it does, not for what it scores.
_Avoid_: Ritual, spell, service, sacrament, prayer

**Marriage**:
The **rite** binding two consenting members of the same kingdom, both present at the **church**, one spouse each. Married players share a respawn point and may `/tp` to one another.
_Avoid_: Wedding, partnership, bond, pairing

**Divorce**:
The **rite** undoing a **marriage**, with both spouses consenting at the **church**. Where one will not consent, the **King or Queen** may grant an **annulment** instead. Either way the shared respawn and spouse teleport end.
_Avoid_: Separation, unmarry, split

**Held experience**:
The experience a member dropped dying inside linked territory, kept against a **funeral** for three in-game days. One record to a player — a later death overwrites the earlier one — and half of it is returned by the rite.
_Avoid_: XP bank, soul, escrow, death record

**Funeral**:
The **rite** for the dead. Held over a member standing at the **church**, it returns half their **held experience**. Held over a dead **productive villager** **awaiting rites**, it releases the frozen **villager wallet** to the **kingdom treasury** less the **tithe**.
_Avoid_: Burial, memorial, wake, last rites

**Awaiting rites**:
The state of a dead **productive villager**'s **villager wallet**: frozen, and released to the **kingdom treasury** less the **tithe** if a **funeral** is held inside the window. Left alone, it escheats to the treasury whole, as **villager wallet escheatment** does.
_Avoid_: Pending funeral, unburied, limbo, dead wallet

**Tithe**:
The share of a **funeral**'s released **villager wallet** kept by the **priest**, paid straight to his player wallet. Where a **cleric** presides it goes to the **kingdom treasury** instead. The church holds no purse of its own.
_Avoid_: Fee, commission, church funds, offering, donation

**Mass**:
The weekly **rite** the whole realm is called to. It falls due every seventh in-game day, is called at the **church** by the **priest** or **cleric** the moment it does, and stands open for the rest of that day. The bell tolls over the realm, the celebrant speaks the liturgy above his own head, and every member who comes to the altar while it sits takes the **blessing** — once to a mass. No **church**, no **consecration** or nobody to celebrate it means no mass that week; the clock simply waits.
_Avoid_: Service, sermon, prayer meeting, church event

**Congregation**:
The realm's own villagers, called in to the **mass**. While a mass sits, every ordinary villager standing inside the kingdom's linked territory and within earshot of the altar (48 blocks) walks to the church and turns to face it; plugin office-holders — villager MPs, the Speaker, the cleric, Treasury Lords, the Town Crier, the magistrate — stay at their posts. When the mass closes they go back to their fields. Villagers take no **blessing**; only members do.
_Avoid_: Flock, worshippers, crowd, parishioners

**Blessing**:
What attending **mass** confers: regeneration and resistance for a couple of minutes, free, once to a mass. It is not asked for and cannot be commanded — the only way to it is to be there.
_Avoid_: Buff, potion, boon, charm

**Coronation**:
The **rite** crowning the rightful **King or Queen** at the **church**. Titles stay operator-assigned — the priest crowns the holder, he never chooses one. Until crowned, a monarch is subject to the **coronation gate**.
_Avoid_: Investiture, enthronement, appointment, election

**Coronation gate**:
The bar on an uncrowned monarch's ceremonial powers: no royal assent, no **honours**, no swearing of roles and no granting of titles. Everyday powers — **build permit**s, the whitelist, `/kingdom` administration — are untouched, and the gate applies only in a kingdom that has a **consecrated** **church** to be crowned in. A realm without one cannot be locked out.
_Avoid_: Regency, interregnum, lockout, suspension
