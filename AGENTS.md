## Learned User Preferences

- British spelling in user-facing plugin messages and documentation.
- Use TDD for domain logic: write failing tests first, then implement until `mvn test` passes.
- Run `mvn package` and provide the JAR path when the user intends to deploy to the game server.
- Use grill-me or grill-with-docs for major feature design; ask one question at a time with a recommended answer.
- Do not create git commits or pushes unless explicitly requested.
- Prefer tracer-bullet vertical slices over large single implementations.
- Minimise diff scope and match existing Java/Maven conventions in the repo.
- Use caveman mode (terse responses) when the user invokes `/caveman`; resume only after they say "stop caveman" or "normal mode".
- Capture agreed domain terms in `CONTEXT.md` during grill-with-docs sessions (glossary only, no implementation detail).
- Do not implement anti-AFK detection for life events.

## Learned Workspace Facts

- Private Spigot plugin targeting Spigot 1.21.x and Java 21; build with `mvn test package`.
- Deploy artefact: `target/kingdom-0.1.0-SNAPSHOT.jar` to the server `plugins/` folder; MC 26.1.2 bundle in `deploy/plugins-26.1.2/` (WorldEdit 7.4.3, WorldGuard 7.0.17).
- GitHub repo: `https://github.com/mrlemoos/kingdom`; upload JARs via `scp -P 2224 … admin@srv1753557.hstgr.cloud:/plugins/`.
- Admin-defined kingdoms; players join once via `/kingdom join`; only OP can move members.
- Noble titles are admin-assigned with fixed slots; Knight has unlimited slots; hierarchy sort in `/kingdom info`; title ladder: King/Queen, Prince/Princess (2 slots, yellow prefix, −3% default tax), Premier, Speaker, Duke/Duchess, Lord/Lady, Count/Countess, MP, Knight/Dame. **MP seats are elected** (up to 4 citizens + villager profession backfill to 8 total); OP cannot assign MP.
- Noble chat/tab/nametag prefixes are bold uppercase with per-rank colours; citizens have no prefix.
- Kingdom persistence in `plugins/Kingdom/data.yml` (members, teleports, parliament, elections); economy state in `plugins/Kingdom/economy.yml`.
- Full Corona economy and Westminster-style Parliament (Commons divisions, Lords assent, registrar Acts on bookshelves); domain glossary in `CONTEXT.md`.
- Plugin `/tp` overrides vanilla player/coordinate teleport (`~` supported); per-kingdom named checkpoints in `data.yml` (OP `checkpoint create`, members `/tp <name>`).
- WorldGuard is a soft dependency via `WorldGuardBridge` (reflection); territory linked with `/kingdom setregion` and `/kingdom setworld`; wilderness income chiefly applies in Nether/End.
- OP `/kingdom treasury credit <kingdom> <amount>` for testing treasury balance.
- No block, PvP, or build enforcement yet; domain logic in testable Java services, Bukkit layer verified manually on server.
