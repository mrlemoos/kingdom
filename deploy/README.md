# Server deploy bundle

## Plugin versions (Minecraft 1.21 – 1.21.1)

| Plugin | File | MC versions |
|--------|------|-------------|
| WorldEdit | `worldedit-bukkit-7.3.9.jar` | 1.21 – 1.21.3 |
| WorldGuard | `worldguard-bukkit-7.0.12-dist.jar` | 1.21 – 1.21.1 |
| Kingdom | `kingdom-0.1.0-SNAPSHOT.jar` | 1.21.x / 26.x (Paper) |

**Install WorldEdit first**, then WorldGuard, then Kingdom. Server runtime is **Paper** (not Spigot).

If your server runs **1.21.4+**, use WorldGuard **7.0.13-dist** and WorldEdit **7.3.14+** from [Modrinth WorldGuard](https://modrinth.com/plugin/worldguard) / [Modrinth WorldEdit](https://modrinth.com/plugin/worldedit) instead.

## Upload to server

```bash
scp -P 2224 /Users/leo/Developer/kingdom/deploy/plugins/*.jar \
  admin@srv1753557.hstgr.cloud:/plugins/
```

Then **restart the server** (full restart, not `/reload`).

## Local enable test (Paper)

```bash
mvn package
./scripts/paper-test-server.sh
```

Downloads Paper **1.21.11** (closest public build to MC **26.x** / Paper **26.1.2**), copies `target/kingdom-0.1.0-SNAPSHOT.jar`, starts a headless server, and exits once Kingdom enables successfully. Override with `PAPER_VERSION` / `PAPER_BUILD` if needed.

Check console for:

```
[WorldEdit] ... enabled
[WorldGuard] ... enabled
[Kingdom] Kingdom enabled.
```
