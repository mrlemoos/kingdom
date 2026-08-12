# Build permits are issued by a Lord Mayor at the capital

Territory ownership meant nothing to a pickaxe: any player could place and break blocks anywhere inside any kingdom's linked region, and the only brake was a build-ban Act covering the whole jurisdiction. We decided building inside a kingdom is a **licensed** activity. Every player except the monarch and the princes needs a **build permit**, and permits are issued by a **Lord Mayor** — a realm NPC standing at the kingdom's **capital**, which is also its **City Hall**.

The permit is free and granted on the spot. It is not a paywall and not a queue; it is a revocable licence, which is the whole point — a licence can be taken away, and a prison sentence takes it away.

## Consequences

- **Opt-in per kingdom.** A kingdom with no capital set has no Lord Mayor, issues no permits, and is not gated at all. Every kingdom that exists today keeps behaving exactly as it does now until its monarch runs `/kingdom capital set`. The alternative — hard-failing every unset kingdom — would have broken live realms on upgrade.
- **Unclaimed land stays free.** The gate only fires where `KingdomTerritoryResolver` resolves an owning kingdom. Wilderness, and any world or region no kingdom has linked, is unlicensed building as before.
- **Unlicensed building is not a crime.** The block event is cancelled and the player is told why; no warrant, no loyalty drop. Act-ban breaches remain criminal and keep their `ActBreach` pipeline. Two refusal régimes sit on the same two events and mean different things: an Act ban says *nobody may build here*, a missing permit says *you personally may not build here yet*.
- **The refusal message is throttled.** Roughly one message per player per 30 seconds, held in memory. A player mining into a hillside would otherwise produce a wall of identical refusals.
- **Prison revokes; release does not restore.** `PrisonOfficePolicy` suspends and restores appointed titles because a prisoner cannot reappoint themselves. A permit they can reclaim in one click, so there is no suspension state — the sentence revokes it outright and the released player walks to City Hall and applies again.
- **OP does not bypass this gate**, deliberately diverging from `BuildConductEnforcer`, which does let OP through an Act ban. Only the monarch and the princes are exempt, and only inside their own kingdom: a Prince of one realm is a foreigner in another, and foreigners cannot hold a permit at all.
- **The Lord Mayor is a sitting wolf, not a player NPC.** A real player-shaped NPC needs NMS `ServerPlayer` plus player-info packets, a packet library, or Citizens as a soft-dep. We compile against the Paper 26.1.2 API while running 26.2, which is precisely where NMS breaks, and a soft-dep NPC silently fails to exist on a server without the plugin. A tamed, sitting, invulnerable, AI-less wolf with a `Lord Mayor` nametag is native API, survives version bumps, and cannot be killed by a wandering skeleton — killing a mob should not shut down a kingdom's permit office.
- **The Mayor has no economy.** It is not a territory villager, holds no wallet, trades with nobody, is never claimed as an MP, and takes no part in GDP, tax, interest, or escheat.
- **Permits and capitals persist** in `data.yml` alongside the rest of kingdom state. Restarts must not send an entire realm back to City Hall.
- **Manual revocation is the monarch's lever.** `/kingdom permit grant|revoke <player>` works offline and gives the crown a political instrument; without it the permit would be nothing but a side-effect of imprisonment. Leaving the kingdom revokes automatically.
- **Prison teleport confinement already holds.** `TpCommand.teleportPlayer` refuses when `PoliceTrialService.isKingdomTeleportBlocked` is true for the *target*, so a third party cannot `/tp` a prisoner out, and ceremony summons are barred by the same check. No new work was needed here; the permit revocation simply rides the existing sentence lifecycle.

## Deliberately not decided

Container access, doors, buttons, farmland, buckets, and ignition are untouched — the gate covers `BlockBreakEvent` and `BlockPlaceEvent` only. Sub-city districts, per-region permits, permit fees, expiring permits, and permits for foreigners under a treaty are all out of scope; the capital is one point and the permit is kingdom-wide.
