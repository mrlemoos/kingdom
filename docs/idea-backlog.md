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
| **Trial jury** | When no player judge: 3 eligible members vote guilty/not guilty (majority); else villager judge. | Court GUI, judge trial |
| ~~**Exile**~~ | ~~Whitelist removal.~~ **Rejected** — no sentence touches the server whitelist. Harden **prison sentence**: cell TP + spawn, all-TP ban (incl. ceremonies), Parliament bar while sentenced, elected offices vacated at once, appointed nobles/sworn suspended then restored; **villager warrants** for territory villagers (Speaker immune). | Cells, resignation/by-election, teleport/ceremony gates, villager MP release |

## Spectacle — cheap, no domain risk

| Idea | Sketch | Rides on |
|---|---|---|
| **Coronation** | Same ceremony machinery as the State Opening, fired when a King or Queen is first assigned. | State Opening ceremony, `SafeChamberLanding` |
| **Royal Standard** | Banner auto-placed at the Lords in the kingdom's colour. | Lords site, rank colours |
| **Titles in death messages** | "LORD Bob was slain" — one listener. | Rank prefixes |

## Suggested first three

**Manifestos**, **vote of no confidence**, **wanted nametag**. All three ride existing rails,
touch no new persistence beyond a field or two, and are visible to players inside one session.
