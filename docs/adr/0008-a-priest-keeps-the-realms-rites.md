# A priest keeps the realm's rites

A realm that marries, buries and crowns its people ought to have someone to do it. The **church** is a place the Crown sites inside its own territory, one to a kingdom, and it does nothing at all until it has been **consecrated**. Everything the system offers hangs off that point: no church, no rites, and a kingdom that sites none runs exactly as it runs today.

Religion here is ceremony, not arithmetic. There is no faith score, no piety meter, no congregation to keep sweet — those would be a second loyalty track measuring the same thing twice. What there is instead is five rites a **priest** performs in person: **marriage**, **funeral**, **blessing**, **coronation** and **consecration**. Each is a small ceremony with a small consequence, and the consequence is what stops the whole thing from being a hat.

The priest is a sworn office on the pattern of the **constable** and the **judge** — one to a kingdom, sworn and unsworn by the King or Queen, exclusive of the police roles because a realm should not have the same hand blessing and arresting, suspended by a prison sentence and restored on release. When the seat stands empty, or its holder is in a cell, a **cleric** villager presides in his place: spawned fresh at the church as the villager Speaker is spawned fresh, never claimed off the territory, with no wallet and no part in the economy, and despatched the moment a player is sworn. A realm is never left unable to bury its dead because nobody wanted the job.

Marriage is between two members of the same kingdom, both of them present and both consenting at the church, one spouse each. It buys a shared respawn point and a teleport to one's spouse — reason enough to hold the ceremony without making the unmarried second-class. It is undone by a rite in the same place, with both consenting, or by an annulment the Crown grants when one will not.

A funeral is worth holding because death costs something that can be given back. When a member dies inside linked territory the experience they dropped is **held** for three in-game days; the rite, with the deceased standing there, returns half of it. One record to a player, overwritten by the next death, so nobody farms his own corpse into a bank. A productive villager's death is the other half of the rite: its wallet freezes **awaiting rites**, and a funeral inside the window sends the balance to the treasury with a **tithe** off the top to the priest, while silence lets it escheat to the treasury whole, as an abandoned wallet does today. That tithe is the priesthood's living, and it is why the office is worth holding.

The **blessing** is the one perk that is simply given: the priest lays hands on a member at the church for a couple of minutes of regeneration and resistance, free, once a day to a man. Charging for it would make the priest a potion shop, which is the thing this design was chosen instead of.

**Coronation** is the rite with teeth. Titles remain the operator's to assign — that does not change — but a monarch who has not been crowned may not do the ceremonial half of the job: no royal assent, no honours, no swearing of roles or granting of titles. The everyday powers, permits and whitelist and `/kingdom` administration, are untouched, and the gate only bites at all in a kingdom that has a consecrated church to be crowned in. A realm cannot be bricked by having no priest; it can only be left with an uncrowned king who cannot yet sign a bill. The priest crowns the rightful holder and no one else — he consecrates the succession, he does not choose it.

Only what cannot be re-derived is written to `data.yml`: the church point and whether it is consecrated, who is sworn priest, the marriages and the annulled ones, the held funeral records with their expiry, the villagers awaiting rites, and whether the monarch has been crowned.

## Considered options

- **A faith or piety stat** on players and villagers driving GDP and loyalty — rejected; a second morale track measuring what loyalty already measures, and it makes the priest a buff generator with a service on a timer.
- **Rival religions with conversion and voting blocs** — rejected for now; a whole political subsystem on top of one that already exists, and it needs a schism to be interesting.
- **Blessings sold for Corona** — rejected; the priest becomes a potion vending machine and the rite stops being a rite.
- **A church coffer on the ledger** — rejected; a new account type, new commands and an audit GUI where the priest's own wallet does the job.
- **A tithe off villager GDP** paid automatically — rejected; passive income needs no ceremony, so it would quietly replace the reason to attend one.
- **Villagers attending services**, pathing to the church for a yield nudge — rejected; pathfinding, a new state and a sweep for flavour that nobody can see happening.
- **Marrying villagers** — rejected; cute, and a breeding mechanic in disguise.
- **Cross-kingdom marriage** — rejected for now; it drags loyalty and war into a rite that has no answer for whose side a spouse is on.
- **No divorce** — rejected; a permanent shared respawn with someone who has left the realm is a bug with a ribbon on it.
- **Coronation conferring the title**, priest as kingmaker — rejected; kingdoms are operator-defined, and a priest and a friend should not be able to take one.
- **A hard coronation gate** on every royal power — rejected; an uncrowned king with no priest is a dead realm.
- **The cleric claiming a territory cleric villager**, as villager MPs claim theirs — rejected; wallet, strike and release bookkeeping for an office that needs none of it.
- **Consecrating estates or burial grounds** — rejected for now; the church is the only building whose blessing changes anything today.
- **Funerals in absentia** — rejected; the point of the rite is that people come.
