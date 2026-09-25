# Kingdom

Kingdom is a Paper plugin for player-run kingdoms, parliamentary government, law enforcement, war, and a simulated Corona economy. Kingdoms are created and configured by server operators; players join one kingdom and hold titles within it.

## Ranks and responsibilities

Titles are assigned by an operator with `/kingdom title`, except MP, which is filled through Commons elections. Slots are per kingdom: one King, one Queen, two Princes/Princesses, one Premier, one Speaker, two Dukes/Duchesses, two Lords/Ladies, four Counts/Countesses, eight MPs, and unlimited Knights/Dames. Feminine title style changes the displayed title; it does not change authority. Permissions below are kingdom-specific and apply while the holder is in office.

| Rank | Role and permissions |
| --- | --- |
| **King / Queen** | The Crown. Holds all rank-delegated powers: appoints sworn constables, judges and clerics; configures court, prison and other sites; sets the capital; issues build permits; posts to the Gazette; manages mints; commands the army and police golems; tables war, peace and treaties; and pays war debt. Grants or refuses royal assent to bills. Resolves resignation offers and appoints titles. |
| **Prince / Princess** | Royal heir, second in precedence. May resolve resignation offers when no monarch is seated. No general Crown powers solely by virtue of this title. |
| **Premier** | Leads the government and proposes fiscal rates and budgets; tables government bills and conducts parliamentary business. Elected by seated player MPs after general elections. When no player MPs are seated, a villager Premier may be selected. |
| **Speaker** | Presides over the Commons, opens and closes divisions, and casts the deciding vote on a tie. Does not hold an MP seat or vote in ordinary divisions. A villager Speaker serves when no player holds the rank. |
| **Duke / Duchess** | May issue and revoke build permits and publish announcements or decrees to the Gazette. |
| **Lord / Lady** | May place and despawn kingdom mints. |
| **Count / Countess** | May issue and revoke build permits. |
| **MP** | Elected Commons member. Tables and seconds motions, votes in Commons divisions, and conducts other business available to seated MPs. MPs may offer to resign. |
| **Knight / Dame** | May command rank-and-file squads, press territory villagers into wartime service, deploy patrol and guard golems, and grant morale pardons at court. |
| **Citizen** | No noble title or rank powers. Players may join a kingdom as members without a title. |

Rank permissions are explicit delegations, not cumulative: for example, a Count can issue permits but cannot publish Gazette notices. Some commands also have operator-only setup paths. Open PvP remains enabled.

## Main systems

- **Kingdoms and territory:** operator-created kingdoms, one kingdom per player, optional WorldGuard region links, named checkpoints, capital and city hall.
- **Parliament:** Commons elections, bills, divisions, royal assent, State Opening, Hansard, no-confidence motions and advisory referendums.
- **Police and courts:** sworn Constables and Judges, warrants, arrests, trials, jury ballots, prison sentences, fines, and wanted indicators.
- **City and building:** a Lord Mayor issues free kingdom-wide build permits to members. Building inside linked territory requires a permit; unclaimed land is unrestricted.
- **War and army:** kingdom war and peace, treaties, squads, standing forces and wartime service, subject to the server's enabled features.
- **Corona economy:** player wallets, villager GDP and trade, taxes, treasury, budgets, mints, tariffs and transfers.
- **Persistence:** kingdom and political state in `plugins/Kingdom/data.yml`; economy state in `plugins/Kingdom/economy.yml`.

## Requirements

- Java 21
- Paper 26.x (currently tested on MC 26.2; compiles against the Paper 26.1.2 API)
- WorldGuard is optional; it enables region-linked territory features.

## Build

```bash
mvn test package
```

The plugin JAR is `target/kingdom-0.1.0-SNAPSHOT.jar`. The package build also syncs it to the deploy directories under `deploy/`.

## Quick start

1. Start the server once so the plugin can initialise.
2. As an operator, create a kingdom: `/kingdom create northmarch Northmarch`.
3. Set its world if it is not named `world`: `/kingdom setworld northmarch <world>`.
4. Link a WorldGuard region if used: `/kingdom setregion northmarch <region>`.
5. Players join with `/kingdom join northmarch`; inspect the realm with `/kingdom info` and `/kingdom list`.
6. Assign the initial monarch with `/kingdom title <player> king` or `queen`. Other noble titles can be assigned with `/kingdom title <player> <rank>`; MPs are elected.

Use `/kingdom help` for available commands. Detailed domain rules and terminology are in [`CONTEXT.md`](CONTEXT.md), and the implementation sequence is in [`docs/build-order.md`](docs/build-order.md).
