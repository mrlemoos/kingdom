# Villager Speaker owns Commons procedure

Commons procedure needed a player Speaker to open and close a division, so a house with any seated player MP and no player Speaker could never divide; the only escape hatch, realm-handled division, applied solely when the Commons was fully villager-seated. We decided the Commons always has a Speaker: when no player holds the Speakership, a **villager Speaker** occupies the Speaker's Chair and runs procedure in every house, mixed or fully villager-seated.

## Consequences

- **Realm-handled division is retired.** Its work — opening, applying villager MP votes, closing — is now the villager Speaker's, and it is the same code path in a mixed house. Two mechanisms for one job would have drifted apart.
- **The Premier villager's casting vote is retired with it.** The casting vote belongs to the Chair, not the government.
- **Tied divisions now fail rather than pass.** The villager Speaker follows Denison's rule and casts nay, leaving the status quo standing. Under realm-handled division the Premier villager cast aye, so existing fully villager-seated kingdoms change behaviour: a tie that used to send a bill to the Lords now kills it. This was chosen deliberately — an unelected Chair should not carry a bill over an evenly divided House.
- **Election ties no longer wait on a player Speaker.** With no player Speaker seated, the realm resolves an MP-seat or Premier-election tie in the Speaker's name by earliest nomination, and `AWAITING_SPEAKER_TIE` is never entered. The villager Speaker cannot cast that vote itself: it is spawned at election close, after the count that produced the tie.
