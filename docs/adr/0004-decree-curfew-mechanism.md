# Decrees set curfew through the Gazette, not a generic effect framework

Parliament already had `CurfewEvaluator` and a plugin-wide `enforcement.curfew` window, but nothing called them. The Crown needed a way to announce binding law without inventing a second legislation track. We decided a **decree** is a Crown act published at the **Town Crier**: it enters **Hansard**, sounds royal assent to the realm, and may carry exactly one mechanical effect — a per-kingdom **curfew**. Announcements share the same board but stay informational.

The binding from decree to effect is **narrow and explicit**. Curfew is the only mechanical decree. A generic decree-effect framework is deliberately not built; every future decree type will want one of its own, and that is a decision for whoever adds the second type.

## Consequences

- **No capital, no Gazette.** The Town Crier stands at the capital with the Lord Mayor. A kingdom with no capital has no board and cannot publish. Live realms keep behaving as before until a monarch sites a capital.
- **Announcements are capped; decrees are permanent.** Twenty announcements, oldest dropped; decrees never count against the cap and are never dropped by it. Retention lives in the city state, not in a second store.
- **Plugin config is the fallback.** `enforcement.curfew.*` applies when no decree has set a window. A *Lift curfew* decree writes a disabled window so enforcement stays off until another decree sets one. *No curfew* on a decree leaves the existing window untouched.
- **Presets only.** The compose GUI offers Dusk–Dawn (13000–23000), Nightfall–Midnight (13000–18000), Lift, and No curfew. Free-form tick entry is rejected; typed numbers fight the signed-book instrument and invite bad windows.
- **Signed book is the instrument.** Title is the headline, pages are the body; the book is consumed on success, matching resignation letters and the Speech from the Throne.
- **Breach files a warrant, nothing else.** Offenders inside linked territory (members and visitors) get a warrant application through `MechanicalJusticeService`. Movement is not blocked and nobody is teleported home — that would fight the open-PvP stance. Filings are throttled so one night is not ten applications.
- **Royal immunity, not OP.** King, Queen and Prince of the jurisdiction are immune via the existing warrant rule. Operators are not exempt, matching the build-permit gate rather than the Act-ban gate.
- **Reuse `CurfewEvaluator`.** The evaluator treats the configured window as the restricted hours (inside → breach). A synthetic assented Act with `ConductKind.CURFEW` and bill id `decree-curfew` feeds it; no second enforcement primitive.

## Deliberately not decided

Generic decree effects beyond curfew, auto-generated Gazette headlines for assent or arrests, anvil/sign/chat composition, movement refusal or teleport-home enforcement, and per-player curfew exemptions beyond the Crown are all out of scope.
