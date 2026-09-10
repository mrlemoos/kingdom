package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureTally;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Dev-only, in-memory capture-plan probe. It never changes a WorldGuard region. */
public final class WorldGuardCaptureSpikeCommand {

    private static final String KINGDOM_PREFIX = "spike-";
    private static final String REGION_PREFIX = "kingdom-spike-";

    @FunctionalInterface
    interface RegionLookup {
        List<String> regionsAt(Location location);
    }

    private final KingdomService kingdoms;
    private final RegionLookup regions;
    private final Map<String, ChunkCaptureTally> tallies = new HashMap<>();

    public WorldGuardCaptureSpikeCommand(KingdomService kingdoms) {
        this(kingdoms, location -> WorldGuardBridge.regionsAt(
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    WorldGuardCaptureSpikeCommand(KingdomService kingdoms, RegionLookup regions) {
        this.kingdoms = kingdoms;
        this.regions = regions;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(c("&cOperators only."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("&cRun this probe in-game."));
            return;
        }
        if (args.length != 3 || (!"capture".equals(args[0]) && !"plan".equals(args[0]))) {
            sender.sendMessage(c("&cUsage: /kingdom-spike <capture|plan> <spike-attacker> <spike-defender>"));
            return;
        }

        Optional<SandboxPair> pair = sandboxPair(args[1], args[2]);
        if (pair.isEmpty()) {
            sender.sendMessage(c("&cBoth kingdoms must be linked spike-* kingdoms with kingdom-spike-* regions in one world."));
            return;
        }
        if (!isInside(player.getLocation(), pair.get().defenderRegion())) {
            sender.sendMessage(c("&cStand inside defender's kingdom-spike-* region."));
            return;
        }

        if ("capture".equals(args[0])) {
            capture(player, pair.get());
        } else {
            plan(sender, pair.get());
        }
    }

    private void capture(Player player, SandboxPair pair) {
        ChunkCoord chunk = new ChunkCoord(player.getWorld().getName(), player.getChunk().getX(), player.getChunk().getZ());
        tally(pair).tickPresence(chunk, pair.attackerId(), pair.defenderId(), 1, 0);
        senderMessage(player, "Captured " + chunk.chunkX() + "," + chunk.chunkZ() + ". Mark three chunks, then run plan.");
    }

    private void plan(CommandSender sender, SandboxPair pair) {
        Set<ChunkCoord> captured = tally(pair).capturedBy(pair.attackerId());
        if (captured.size() < 3) {
            senderMessage(sender, "Need three captured chunks. Have " + captured.size() + ".");
            return;
        }
        RegionMergePlan plan = RegionMergePlan.fromCapturedChunks(pair.attackerId(), pair.defenderId(), captured);
        senderMessage(sender, "Plan " + plan.proposedVertices() + ". No WorldGuard region changed.");
    }

    private Optional<SandboxPair> sandboxPair(String attackerId, String defenderId) {
        Optional<Kingdom> attacker = kingdoms.getKingdom(attackerId);
        Optional<Kingdom> defender = kingdoms.getKingdom(defenderId);
        if (attacker.isEmpty() || defender.isEmpty()
                || !attackerId.startsWith(KINGDOM_PREFIX) || !defenderId.startsWith(KINGDOM_PREFIX)) {
            return Optional.empty();
        }
        String attackerRegion = attacker.get().getWorldGuardRegions().stream().findFirst().orElse(null);
        String defenderRegion = defender.get().getWorldGuardRegions().stream().findFirst().orElse(null);
        String attackerWorld = attacker.get().getWorldName();
        String defenderWorld = defender.get().getWorldName();
        if (attackerRegion == null || defenderRegion == null || attackerWorld == null || defenderWorld == null
                || !attackerRegion.startsWith(REGION_PREFIX) || !defenderRegion.startsWith(REGION_PREFIX)
                || !attackerWorld.equals(defenderWorld)) {
            return Optional.empty();
        }
        return Optional.of(new SandboxPair(attackerId, defenderId, defenderRegion));
    }

    private boolean isInside(Location location, String regionId) {
        return regions.regionsAt(location).stream().map(id -> id.toLowerCase(Locale.ROOT))
                .anyMatch(regionId.toLowerCase(Locale.ROOT)::equals);
    }

    private ChunkCaptureTally tally(SandboxPair pair) {
        return tallies.computeIfAbsent(pair.attackerId() + '\u0000' + pair.defenderId(), ignored -> new ChunkCaptureTally(1));
    }

    private static void senderMessage(CommandSender sender, String message) {
        sender.sendMessage(c("&7[WG spike] " + message));
    }

    private record SandboxPair(String attackerId, String defenderId, String defenderRegion) {}
}
