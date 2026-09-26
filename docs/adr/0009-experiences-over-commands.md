# Experiences over commands

A realm that is run by typing is a realm run from a console. The Crown sites a church by standing on a spot and typing `/kingdom church set`; a priest is sworn by name in chat; a constable arrests by typing the suspect's name. Every one of those acts has a place and a person in the world it belongs to, and the command skips both. The standing principle — prefer a right-click on something in the world to a new subcommand — has governed new work for a while (the **oath of allegiance** at the **lord mayor**, the **coronation window** at the **cleric**, **honours** by the golden sword, the **resignation letter**), but the older powers were never brought across. This decision brings them across.

**Siting is done by laying a stone.** Every point the Crown sets — **capital**, **church**, **Town Crier**, **mint**, the **House of Commons**, **House of Lords**, the Speaker's Chair, the bar, the **MP seats**, the **registrar**, the **court** and the **cell**s, and the **granary** — is sited by a **foundation stone**. The Crown asks the **Realm Hub** for the stone of the site it means to raise and is handed a tagged item; placing it inside its own territory is the act of siting. The ordinary siting rules decide whether it holds: a stone refused goes back into the hand that laid it. A stone that holds is consumed — the site is marked by what already stands there, its NPC or its chamber, not by a block that would need its own grief protection. Where the site already *is* a block, the stone is that block: the registrar's stone is a chiseled bookshelf and the court's a lectern, and each stays where it is put; the Lords' is a banner in the Crown's own design, spent to become the **kingdom flag** raised beside the House. Sites with many points number themselves: an MP seat stone fills the next empty seat, a cell stone the next free cell. Sites that are WorldGuard regions rather than points take the smallest region around the stone. Clearing a site is done from the same Hub entry, behind a confirmation.

**Rites are asked for at the cleric.** Right-clicking the cleric already opens the coronation window and the oath of service; it now opens a **rites window** offering every rite the clicker may ask for — consecration, marriage, divorce, annulment, funeral — and nothing they may not. Marriage consent is given in a window, not typed into chat.

**Appointments are made with the sword.** The golden sword that confers **honours** confers the **sworn role**s too: constable, judge and priest are sworn and unsworn in the same window, under the same rules — constable and judge exclusive, the **coronation gate** in force.

**Police golems are built, not summoned.** An iron golem built in the vanilla way inside linked territory by the King, Queen or a Knight takes the oath as a **patrol golem**; its orders window sets it to guard or stands it down. Four iron blocks is the price of an officer, which makes deploying one a choice.

**Elections are carried on a poll card.** The Crown starts an election and the Premier calls or closes a referendum from the **Parliament hub**. Every member is handed a **poll card** when polls open, and redelivered it on joining; right-clicking it offers nomination, then the ballot, then, when the count is tied, the Speaker's casting vote.

**The smaller powers go where they are exercised.** Deposit sits beside withdrawal at the **Lord of the Treasury**. A constable arrests by striking a **wanted** player with an iron sword. The arrest reward is posted at the court; the Crown cancels warrants from a **warrant register** in the Hub; tribute is paid from the Hub.

**The Hub is the manual, and the one door.** `/kingdom` opens the **Realm Hub** and nothing else does — no hub item, no hub NPC, no hub block. The Hub is split into **hub section**s — City, Church, Parliament, Police, War, Treasury — behind a front page that shows the reader's standing, whatever business is live, and a door to each section. A section lays out its places, then its powers, then its experiences. Every experience has an entry, including those that begin elsewhere — the rites window at the cleric, the sword, a built golem, the poll card — and every entry says three things: where things stand, how to do it in one or two plain steps, and who may. Entries that are not the reader's stay greyed and name who may, as before.

**Every experience answers back the same way.** The action bar gives instructions while an item is in hand and gives refusals. A title marks a milestone for those it concerns, and only coronation, the founding of a capital and the opening of polls are titled to the whole realm. A boss bar counts down anything with a window — consent to a marriage, polls, a referendum, a jury, mass. Chat keeps the record, with a Gazette post where the realm cares. Every success is heard and seen at the spot it happened; a refusal is heard, not seen.

**The commands stay, for operators.** Every siting command becomes operator-only: an escape hatch for an NPC stuck in a wall or a test server, not a second road for the Crown. Commands that are server administration rather than realm business — `create`, `move`, `setregion`, `setworld`, `treasury credit`, the whitelist, the King or Queen title — stay commands.

## Considered options

- **Site at your feet from a Hub button** — rejected; it is the command with fewer keystrokes, and the Crown still never puts anything in the world.
- **Detect a built structure** (an altar pattern for the church, a ring of seats for the Commons) — rejected; a pattern matcher per site, and the Crown has to learn the recipes.
- **A stone that stays as a marker block**, broken to clear the site — rejected; it can be griefed in war and by foreign builders, and needs protection, break handling and reconcile for what is decoration.
- **Clearing a site by right-clicking its NPC** — rejected; cells, seats and the bar have none.
- **Choosing a slot before taking the stone** — rejected; more clicks for a choice the next empty slot makes correctly.
- **Deleting the commands** — rejected; no recovery when an NPC is lost in a wall.
- **Keeping the commands for the Crown** — rejected; two roads make the experience optional, and optional ceremonies are skipped.
- **Appointing each role at its own site** (judge at the court, priest at the cleric) — rejected; three flows, each needing the appointee to stand in the right place.
- **A free commission item for police golems** — rejected; free officers are spammed up to the cap.
- **Voting from the Parliament hub alone** — rejected; members have to find the hub, and a card in hand is its own reminder.
- **A hub item or hub NPC** — rejected; `/kingdom` is already the door, and a second one is a thing to lose or to reconcile.
- **Keeping one flat Hub list, sorted into blocks** — rejected; more topics only means more pages of one long list.
- **Feedback chosen per feature** — rejected; players learn one vocabulary, not forty.
