# Server deploy bundle

## Plugin versions (Minecraft 1.21 – 1.21.1)

| Plugin | File | MC versions |
|--------|------|-------------|
| WorldEdit | `worldedit-bukkit-7.3.9.jar` | 1.21 – 1.21.3 |
| WorldGuard | `worldguard-bukkit-7.0.12-dist.jar` | 1.21 – 1.21.1 |
| Kingdom | `kingdom-0.1.0-SNAPSHOT.jar` | 1.21.x (Spigot API) |

**Install WorldEdit first**, then WorldGuard, then Kingdom.

If your server runs **1.21.4+**, use WorldGuard **7.0.13-dist** and WorldEdit **7.3.14+** from [Modrinth WorldGuard](https://modrinth.com/plugin/worldguard) / [Modrinth WorldEdit](https://modrinth.com/plugin/worldedit) instead.

## Upload to server

```bash
scp -P 2224 /Users/leo/Developer/kingdom/deploy/plugins/*.jar \
  admin@srv1753557.hstgr.cloud:/plugins/
```

Then **restart the server** (full restart, not `/reload`).

Check console for:

```
[WorldEdit] ... enabled
[WorldGuard] ... enabled
[Kingdom] Kingdom enabled.
```
