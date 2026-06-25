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
A kingdom's total economic standing: treasury Corona plus the valued worth of material reserves and estates in linked territory. Informational and for comparison; does not change what the treasury can spend.
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
Passive Corona income a kingdom treasury earns each in-game day from productive villagers. A villager counts when its bed and workstation lie inside the kingdom's territory; income scales by profession and soft-caps at higher populations.
_Avoid_: Villager tax, population income, NPC revenue

**Productive villager**:
A villager whose bed and workstation are both inside a kingdom's territory. Only productive villagers contribute to villager GDP.
_Avoid_: Working villager, employed villager, citizen

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
The noble who proposes a kingdom's fiscal rates: tax (base, per-rank modifiers, foreign-income surcharge) and Corona transfer fees. Rates take effect only after the King or Queen approves. One Premier slot per kingdom.
_Avoid_: Chancellor, treasurer, finance minister

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
The elected chamber where the Premier tables bills and Members of Parliament vote. Debate and divisions take place within the chamber bounds set by the monarch.
_Avoid_: Lower house (in player-facing text), assembly

**House of Lords**:
The chamber where the King or Queen grants or withholds royal assent on bills passed by the Commons. Assent and rejection both require the monarch to be present in the Lords.
_Avoid_: Upper house (in player-facing text), senate

**Act**:
A bill that has passed the Commons and received royal assent. Enacts fiscal rates, a budget cap, or an authorised treasury spend. Recorded as a written book in the registrar.
_Avoid_: Law, statute, decree

**Bill**:
A formal proposal before Parliament: fiscal rates, treasury budget, or treasury spend (including mint placement). Only one bill may be in progress per kingdom at a time.
_Avoid_: Proposal, motion, decree

**Division**:
A Commons vote on the bill before the House. The Speaker opens and closes the division; MPs vote aye, nay, or abstain while the division is open.
_Avoid_: Poll, ballot, referendum

**Casting vote**:
The Speaker's tie-breaking vote when aye and nay are equal. Required before a tied division can pass or fail.
_Avoid_: Tie-breaker, deciding vote

**Royal assent**:
The monarch's approval of a bill passed by the Commons, given in the House of Lords. Withholding assent rejects the bill without enacting it.
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
A villager MP representing one of the kingdom's largest productive-villager professions. Stands in the Commons as a persistent villager with an MP prefix; division votes are cast automatically from profession leanings on each bill type.
_Avoid_: NPC delegate, villager representative, profession delegate

**Election casting vote**:
The Speaker's tie-breaking choice when two or more citizen candidates tie for the last available player MP seat in an election. Required before that election can close.
_Avoid_: Tie-breaker, deciding ballot, runoff vote
