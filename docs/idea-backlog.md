# Kingdom — idea backlog

**Status:** Brainstorm (unscheduled)
**Date:** 2026-08-11

Loose ideas for player-facing features. Not a plan — nothing here is committed, sized, or
sequenced against [`docs/build-order.md`](build-order.md). Ordered cheap-first within each
group: everything listed rides subsystems that already exist (Parliament, Corona economy,
villager wallets, Police, WorldGuard territory). Glossary terms live in `CONTEXT.md`.

War and chunk capture are deliberately absent — that is Phase 6 in the build order and gated
on the WorldGuard capture spike.

## Politics — reuses Parliament

> **Designed and scheduled.** All six are now Phase 7 in [`docs/build-order.md`](build-order.md),
> with agreed terms in `CONTEXT.md` `## Parliament`. The table below is the original sketch.

| Idea | Sketch | Rides on |
|---|---|---|
| **Manifestos** | Candidate writes a one-line pledge at nomination; shown in the election GUI and in the broadcast. No mechanical effect. | Election nomination, existing GUIs |
| **Party whips** | MPs pick a party colour at nomination; division tally shows the party split. | Division tally, nametag/prefix colours |
| **Vote of no confidence** | Any three seated MPs table it → division → passes → Premier leaves office and a Premier by-election runs. | `tableBill`, divisions, resignation/by-election flow |
| **Hansard** | Every division result appended to a book on the registrar bookshelf. | Registrar Act writing |
| **Referendum bill** | Bill kind where every online kingdom member votes, not only seated MPs. | Bill kinds, division voting |
| **Prime Minister's Questions** | Villager Speaker broadcasts a scheduled question window. Ceremony only, no mechanics. | `ElectionTask` sweep, State Opening ceremony pattern |

## Economy — reuses villager wallets and the treasury

| Idea | Sketch | Rides on |
|---|---|---|
| **Bank of Corona interest** | ~~Daily interest rate paid on (or charged against) villager wallets, set by a FISCAL bill.~~ **Done** — `FiscalRates.villagerWalletInterest`, applied after GDP/trades and before escheat. | Villager wallet daily settlement, fiscal rates |
| **Tariffs** | ~~Separate commerce tax rate for trades involving non-members.~~ **Done** — `FiscalRates.tariff` surcharge on Corona merchant / emerald trade taxation for non-members. | Commerce tax, membership check |
| **Bounty board** | ~~Post Corona on a player's head at the court lectern; the arresting constable claims it.~~ **Done** — **arrest reward** (not bounty): member wallet escrow on active warrant; constable paid on arrest; golem/cancel refunds poster. `/kingdom police reward`. | Court lectern, wallet escrow, arrest flow |
| **Public works** | ~~Premier spends from the treasury to place beacon/lodestone estate blocks, which then count toward realm wealth.~~ **Done** — `SPEND_PUBLIC_WORK` prepare → table → assent places the estate block and debits Estate Corona worth against the treasury budget; wealth still via ordinary Estate scan. | Budget bills, realm wealth valuation |
| **Villager strikes** | ~~A wallet frozen too long stops the villager trading and shows an `[on strike]` nametag.~~ **Done** — `frozen-wallet-strike-mc-days` (default 7, must be shorter than escheat); nametag + player trade/taxation refuse; clears when productive. | Wallet freeze/escheat, villager nametag reconciliation |

## Law and Police

| Idea | Sketch | Rides on |
|---|---|---|
| **Wanted nametag** | Active warrant + inside jurisdiction → red `[WANTED]` replaces other prefixes. | Warrant issue, nametag pipeline |
| **Trial jury** | Auto-seat on arrest **or patrol detain** when no Judge; secret ballot GUI anywhere; `/kingdom police jury` reopen; timeout→realm-handled. Accused free until sentenced. | Court GUI, judge trial, patrol golems |
| ~~**Exile**~~ | ~~Whitelist removal.~~ **Rejected** — no sentence touches the server whitelist. Harden **prison sentence**: cell TP + spawn, all-TP ban (incl. ceremonies), Parliament bar while sentenced, elected offices vacated at once, appointed nobles/sworn suspended then restored; **villager warrants** for territory villagers (Speaker immune). | Cells, resignation/by-election, teleport/ceremony gates, villager MP release |

## Spectacle — cheap, no domain risk

| Idea | Sketch | Rides on |
|---|---|---|
| **Coronation** | ~~Same ceremony machinery as the State Opening, fired when a King or Queen is first assigned.~~ **Done** — `CoronationDecision` fires only on a vacant throne via `/kingdom title`; `CoronationCeremony` summons, crowns, proclaims, auto-returns; no Lords site → proclamation only, never blocks the title. | State Opening ceremony, `SafeChamberLanding` |
| **Royal Standard** | ~~Banner auto-placed at the Lords in the kingdom's colour.~~ **Done** — kingdom flag beside Lords; hold banner on `parliament set lords` to set design (else Crown gold / keep stored); persist patterns in `data.yml`. | Lords site, kingdom flag |
| **Titles in death messages** | ~~"LORD Bob was slain" — one listener.~~ **Done** — `DeathMessageTitleListener` splices the full coloured prefix into the vanilla message; citizens unaffected. | Rank prefixes |

## Suggested first three

**Manifestos**, **vote of no confidence**, **wanted nametag**. All three ride existing rails,
touch no new persistence beyond a field or two, and are visible to players inside one session.

---

# Wave 2

**Status:** Brainstorm (unscheduled)
**Date:** 2026-09-10

Second sweep, after Parliament, Police, War, the Corona economy, City, Granary, Hearth,
Church, the realm Calendar, and the Gazette/Town Crier all landed. Same rules as wave 1:
nothing here is sized or sequenced, and everything listed rides a subsystem that already
exists. Ordered cheap-first within each group.

Deliberately absent: chunk capture and the siege phases (Phase 6 in
[`docs/build-order.md`](build-order.md)), and anything already shipped — Gazette, Town
Crier, coronation, royal standard, marriage and funeral rites, arrest rewards, public works.

## Diplomacy

Treaty bills, trade pacts, and non-aggression treaties are shipped. Alliance and the envoy remain ideas.

| Idea | Sketch | Rides on |
|---|---|---|
| ~~**Treaty bill**~~ | Shipped: `BillType.TREATY` offers non-aggression or a trade pact. Both Crowns assent; either may repeal. | `BillType`, division, royal assent, `data.yml` |
| ~~**Trade pact**~~ | Shipped: waives tariff both ways while commerce tax remains due. | `FiscalRates.tariff`, merchant settlement |
| ~~**Non-aggression**~~ | Shipped: war bill validation rejects an active treaty target. | `WarService` war-bill validation |
| **Alliance** | An ally's muster call reaches your rostered members; answering credits service, ignoring it costs morale as usual. | `MusterService`, `StandingRosterService` |
| **Envoy** | A villager at the capital; right-click opens a paginated treaty register with the same confirm/revoke shape as the permit register. | `LordMayorService` / `TownCrierService` NPC pattern, permit register GUI |

## Crown finance — reuses the treasury and the daily processor

| Idea | Sketch | Rides on |
|---|---|---|
| ~~**Per-player tax hook**~~ | Shipped: daily member share, personal feedback, and service credit on positive payment. | Daily GDP + income tax settlement, `RealmFeedback` |
| **National debt** | The treasury may go negative; daily interest accrues against it; the balance and its trend show in **State of the Realm**. Nothing is blocked — bills simply get expensive. | Treasury, daily processor order, State of the Realm item |
| **Gilts** | The Crown issues bonds against a BUDGET line; players buy with Corona, take a daily coupon from the treasury, redeem at maturity. An unpayable coupon is a **default**: loyalty drop, and the Commons may table no confidence. | Budget bills, wallet transfers, daily processor, `NO_CONFIDENCE` |
| **Ground rent** | Estates inside linked territory owe the Crown a daily rent per valued block, collected with the other daily lines. Non-payment revokes the build permit before it revokes anything else. | Estate/realm-wealth scan, build permits |

## Law — reuses the warrant → trial → sentence pipeline

| Idea | Sketch | Rides on |
|---|---|---|
| **Statute of limitations** | An active warrant nobody serves expires after N sittings and clears the `[WANTED]` nametag. | Police sweep, sitting calendar |
| **Appeal to the Crown** | A sentenced player petitions; a paper reaches the monarch; the Crown upholds, commutes, or pardons from the review GUI. | Existing pardon, resignation-letter paper flow, prison sentence |
| **Prison labour** | Work a block inside the cell to shorten the sentence clock; idling does nothing. | Prison sentence clock, cell bounds |
| **Case persistence** | Open cases and in-flight jury seating are memory-only and die with the server. Persisting them is debt, not a feature, but it belongs on this list. | `data.yml` police section |

## Parliament — reuses divisions and the order paper

| Idea | Sketch | Rides on |
|---|---|---|
| **Prime Minister's Questions** | Carried over from wave 1, still unbuilt. The villager Speaker broadcasts a scheduled question window. Ceremony only. | `ElectionTask` sweep, State Opening pattern |
| **Private member's bill** | A seated MP who is not the Premier may table, given a second from another MP. | `tableBill`, `NO_CONFIDENCE` seconding rule |
| **Amendments** | Before the division opens, an MP may amend a tabled bill's numeric fields; the amendment itself is divided on first. | Bill model, division |
| **Select committee** | Three MPs are summoned to report on the treasury before a BUDGET division may open; the bill holds the order paper until they report. | Trial jury seating (same shape), order paper hold used by referendums |

## World — cheap spectacle, no new persistence

| Idea | Sketch | Rides on |
|---|---|---|
| **Market day** | One day a season where commerce tax drops and villager trade limits reset. Broadcast by the Crier. | `SeasonTurn`, `FiscalRates`, Gazette |
| **Bandit raid** | A hostile wave on a capital with no standing squads or patrol golems present; losing it costs treasury. | War squads, patrol golems, capital |
| **Plague** | A season event with the same day-ramp shape as hunger and cold, cured at the church by the Celebrant. | `HungerDayService` / `HearthDayService` ramp, `ClericService` |

Plague is listed last on purpose: it is a third survival ramp on top of two, and the marginal
fun per line of code is the worst on this page.

## Suggested first three

Treaty bill and per-player tax hook are shipped. Alliance, envoy, and appeal to the Crown remain.
