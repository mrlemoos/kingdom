# Server deploy bundle

## Plugin versions (Minecraft 26.2 / Paper build 111)

| Plugin | File | MC versions |
|--------|------|-------------|
| WorldEdit | `worldedit-bukkit-7.4.4.jar` | 1.21.10 – 26.2 |
| WorldGuard | `worldguard-bukkit-7.0.18.jar` | 26.1 – 26.2 |
| Kingdom | `kingdom-0.1.0-SNAPSHOT.jar` | 1.21.x / 26.x (Paper) |

**Install WorldEdit first**, then WorldGuard, then Kingdom. Server runtime is **Paper** (not Spigot).

`deploy/plugins-26.1.2/` keeps the older 26.1.2 bundle (WorldEdit 7.4.3, WorldGuard 7.0.17).

## Server jar

`deploy/server/paperclip.jar` is Paper **26.2 build 111** (sha256 `3ec81e3ea50cc6090b94aab024491846a202702e8a874308a5d7510f6b3aa012`). The server expects the file to be named `paperclip.jar`.

Re-download with:

```bash
curl -o deploy/server/paperclip.jar \
  "$(curl -s https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/111 \
     | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['server:default']['url'])")"
```

## Upload to server

**Stop the server first** — overwriting a running jar corrupts the live instance.

```bash
scp -P 2224 /Users/leo/Developer/kingdom/deploy/server/paperclip.jar \
  admin@srv1753557.hstgr.cloud:/paperclip.jar

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
