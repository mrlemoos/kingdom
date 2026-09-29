# Crown finance: national debt, gilts, ground rent

Status: accepted. Decisions below were confirmed after the design interview.

## National debt

- National debt equals the treasury overdraft plus outstanding gilt principal. War debt remains a separate obligation and is reported separately.
- Approved budget spending may take the treasury below zero. Overdraft interest may increase that negative balance.
- Parliament sets the daily overdraft interest rate through a FISCAL bill. It starts at 0.1% per realm day and may range from 0% to 1%; negative rates are forbidden.
- An inaugural FISCAL bill from a villager Premier moves that rate by 0.1 percentage point: aye lowers it, nay raises it, abstention leaves it unchanged. Clamp the result to 0–1%.
- Charge overdraft interest once per realm day on the closing negative treasury balance, after that day's income, rent, levy upkeep, and gilt coupons. Revenue that clears the overdraft before closing incurs no interest that day.
- Realm wealth subtracts outstanding gilt principal. It already includes the signed treasury balance, so overdraft is not subtracted a second time.
- State of the Realm shows national debt total, overdraft, gilt principal, and change since the previous realm day. The Gazette and issuing mint publish the same figures to members and foreign buyers.
- Keep fractional ledger amounts for interest and coupons, including amounts below 0.01 Corona; display enough decimal places to make small amounts visible rather than rounding daily obligations away.
- Settle the full daily economy for each elapsed realm day exactly once, including GDP, tax, rent, and debt obligations. Include days skipped by sleep or time changes. Preserve existing daily stages and the agreed payment priorities; overdraft interest closes the day. Server downtime adds no realm days because the world clock stops. Catch-up may be spread across ticks but must not discard days.
- Levy upkeep, gilt coupons, war tribute, voluntary war-debt payments, and cash withdrawals cannot overdraw the treasury. Existing levy arrears and gilt default therefore remain possible.

## Gilts

- A BUDGET Act may include one optional gilt issue with face value, coupon rate, and maturity. Parliament approves those terms; the Crown opens the sale after royal assent. An ordinary BUDGET Act issues no gilts.
- Gilt face value cannot exceed the spending allowance approved by that same BUDGET Act.
- The Crown must open an authorised sale within seven realm days of BUDGET assent. Unused gilt authority then lapses; the approved spending budget remains.
- A later BUDGET Act replaces any earlier gilt authority that the Crown has not opened. An already open sale keeps its original terms.
- A kingdom may have one open sale at a time. Existing gilt issues may remain outstanding while Parliament authorises later issues.
- Gilt holdings are not transferable. The buyer or gifted monarch keeps the holding through redemption, even after a membership change.
- Crown opens the sale from the Treasury section of the Realm Hub GUI or at that kingdom's Lord of the Treasury. A sited mint is required to open it; buyers, including foreigners, use that Lord of the Treasury. No mint means no sale and no Crown gift.
- The issuing mint shows its open sale, the visitor's holdings, next coupon, maturity, and overdue principal. The Gazette announces sales and defaults; the Treasury Hub directs members to the mint.
- A BUDGET Act's gilt face value and each purchase use whole Corona. A player may buy any whole amount up to remaining face value and wallet balance; daily coupons may be fractional.
- Any player, including foreigners and the Crown, may buy with wallet Corona. Purchases credit the issuing treasury.
- The sale closes seven realm days after the Crown opens it, or sooner when sold out. Coupons and the maturity term begin at close; all buyers in that issue share the same dates.
- Each issue's coupon rate is fixed by its BUDGET Act at 0–1% per realm day for its full life. Later FISCAL bills do not change existing coupons.
- At sale close, unsold face value becomes a full gilt gifted to the Crown. No purchase money enters the treasury for it, but its coupons and principal are owed and count towards national debt. For example, a 100 Corona issue with 60 bought creates 100 Corona of gilt principal: 60 purchased, 40 gifted. See [ADR 0010](adr/0010-unsold-gilts-go-to-issuing-monarch.md).
- The gifted holding belongs permanently to the player who opened the sale as King or Queen. Abdication or succession does not transfer it.
- A BUDGET Act chooses maturity from 7 to 90 realm days after sale closes. Every holder in the issue has the same maturity day.
- Each day, pay available coupon Corona pro rata across holders. Add unpaid coupon shares to their gilt principal; these shares earn future coupons, count towards national debt, and become due immediately. Any coupon shortfall is a default.
- At maturity, repay available principal automatically and pro rata, including offline holders. Unpaid principal remains owed at the original coupon rate and counts as default until repaid.
- Retry overdue principal automatically each realm day after levy upkeep and coupons, pro rata across eligible holders. The Crown cannot delay repayment while cash is available.
- Cash priority is levy upkeep, then current gilt coupons, then due principal (capitalised missed coupons and matured issue principal). Within each gilt payment tier, split scarce Corona pro rata across issues and holders.
- Gilt default lowers every kingdom member's political loyalty. It is a grievance, not an automatic no-confidence motion; seated player MPs must still table and second the motion under existing rules.
- One loyalty drop applies when the realm first enters default across all gilt issues. A continuing default causes no further drop; after all due principal clears, a later default can trigger a new drop.

## Ground rent

- The member who placed an estate block owes rent for that block. Record the placer's identity at placement so unpaid rent can revoke the correct member's build permit.
- Kings, Queens, Princes, and Princesses are exempt from ground rent on their estates.
- Royal exemption follows the owner's current rank at each daily settlement. Taking a royal rank pauses future rent; leaving it resumes rent on still-owned estate blocks. Earlier arrears remain owed.
- Crown-funded public works and estate blocks already present when this feature launches have no player owner and owe no rent. They still contribute to realm wealth. Breaking and replacing an estate block assigns the new placer.
- Daily rent for each owned estate block equals the ground-rent rate times that block type's configured Corona value.
- Parliament sets the ground-rent rate through FISCAL bills.
- The ground-rent rate starts at 0.1% per realm day and may range from 0% to 1%. At the default estate values, a lodestone owes 0.05 Corona/day, a conduit 0.25, and a beacon 0.5.
- Ground-rent payments credit the treasury and are reported separately from income-tax revenue and GDP in State of the Realm.
- A debtor sees exact arrears, estate locations, and the payment route in the Realm Hub and at the Lord Mayor. The Crown sees named arrears in the permit register; the Gazette publishes totals only.
- An inaugural FISCAL bill from a villager Premier moves the ground-rent rate by 0.1 percentage point in its profession vote bias direction, clamped to 0–1%; abstention leaves it unchanged.
- Collect what the owner's player wallet can pay. Any shortfall becomes ground-rent arrears and revokes that member's build permit. The Lord Mayor cannot reissue the permit until the arrears clear. Player income-tax shortfalls remain debt-free.
- Rent keeps accruing on an owned estate while its block remains, even if the owner's build permit has been revoked.
- A member whose permit was revoked may break their own recorded estate blocks to stop future rent. This does not clear existing arrears or allow any other unlicensed building.
- Daily settlement automatically takes available wallet Corona towards arrears. A debtor may also pay at the Lord Mayor immediately, then reapply for the free build permit.
- Daily player income-tax share is collected before ground-rent arrears and current rent from the same wallet.
- When an owner leaves the kingdom, their estate blocks become unowned and stop future rent. Existing arrears remain owed to the former kingdom.
- Once a debtor leaves, the former kingdom stops automatic wallet deductions. The arrears remain payable voluntarily at its Lord Mayor and block a build permit if the player later rejoins.
- When territory passes to another kingdom, estate ownership ends and future rent stops. The new kingdom counts the blocks in realm wealth; arrears already owed to the original kingdom remain there.
- New ground rent pauses while a kingdom has no capital, matching build-permit enforcement. Ownership and existing arrears remain; automatic wallet collection of arrears continues. Direct payment and permit reapplication resume when the capital returns.

## Player access and interaction

Every finance rule has a player-facing explanation. Players set rates, authorise gilts, open and buy sales, pay arrears, and reapply for permits through GUI or guided chat. No player-facing finance subcommand, including existing FISCAL or BUDGET command routes, may execute these actions or serve as a required fallback. `/kingdom` remains an entry to the existing Realm Hub GUI; it carries no finance action or value. Each action also has an in-world GUI route at its Parliament, mint, or city hall site.

| Need | Where players see it and act |
|---|---|
| Understand debt | Right-click the Town Crier for Gazette, or a Lord of the Treasury for the mint GUI. Both show current treasury, overdraft, gilt principal, total national debt, previous-day change, active rates, and default status; war debt has its own line. The Treasury Hub and Crown's State of the Realm show the same detail and name the next available action. |
| Set rates or authorise gilts | Premier uses the Commons Parliament GUI to table FISCAL or BUDGET business; guided chat prompts gather the selected rate or gilt face value, coupon, and maturity. The GUI previews current and proposed figures, bounds, cost, and who may act. Commons division and royal assent stay in their existing GUIs. |
| Open or buy a gilt | Crown opens the authorised sale in the Lord of the Treasury GUI, with a confirmation showing the closing day and that unsold principal becomes the issuing monarch's personal holding. The Treasury Hub GUI links to the same action. Any player at that mint sees the issue's terms, remaining face value, and own wallet; a GUI amount choice or chat prompt confirms a purchase. The same GUI shows personal holdings, next coupon, maturity, payments, and overdue principal, including for foreign holders. |
| Understand or pay ground rent | Right-click the Lord Mayor for owned estate locations, value, daily charge, arrears, permit status, and a pay/reapply GUI. The City section of the Realm Hub shows the same status and points to the Mayor. A debtor with no permit sees that breaking their own estate stops future rent. Crown's permit register shows named arrears; Gazette shows rent totals only. |

Daily settlement gives each affected player a private chat summary with amount paid, amount still owed, and exact next step. Summaries for offline players arrive on next login; catch-up days are combined into one readable result. Gazette posts announce new gilt sales, maturity, default, and default clearance. Disabled or unavailable GUI actions say why and where to go next, including missing mint or capital and rank restrictions. No automatic payment requires the holder to be online.

## Existing rules to preserve

- War tribute pays available treasury Corona on victory; any remainder becomes war debt owed to the victor.
- An approved treasury budget still limits discretionary spending.
- An estate is a beacon, conduit, or lodestone in linked territory. Current wealth counts have no owner or builder attribution.

## Acceptance examples

- Treasury 0, approved budget 100, approved spend 100: treasury becomes -100 and national debt 100. At 0.1% daily interest with no revenue, closing debt becomes 100.1.
- Gilt issue 100, players buy 60, sale closes: treasury received 60, purchased principal is 60, issuing monarch receives 40 gifted principal. Total gilt principal and national debt rise by 100, before any overdraft.
- A 1-Corona holding at 0.1% earns 0.001 Corona daily. That amount remains on the ledger.
- Beacon worth 500, owner has 0.2 Corona after income tax, rent rate 0.1%: 0.5 is due, 0.2 reaches treasury, 0.3 becomes arrears, permit is revoked. Breaking the owned beacon stops later rent but leaves the 0.3 owed.
- A clock jump over two realm days runs two complete daily settlements. A restart or repeated task call does not charge either day again.
- A foreign player can inspect debt and gilt terms at the issuing mint and buy there without joining the kingdom or typing a command. An owner can inspect and pay rent at the Lord Mayor without typing a command.

## Implementation constraints

- Only approved budget spending and overdraft interest may create a negative treasury. Cash-only treasury debits must treat a negative balance as zero available; current `EconomyService.debitTreasury` does not do this.
- Daily settlement must persist the last settled realm day and run for kingdoms without linked territory too; current `VillagerGdpTask` skips those kingdoms and does not catch up elapsed days.
- Estate wealth counts alone cannot identify rent debtors. Ownership must survive restart and be cleared when a block disappears, territory changes kingdom, or its owner leaves.
