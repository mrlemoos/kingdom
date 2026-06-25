package dev.leo.kingdom.service;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TitleStyle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KingdomService {

    public static final String DEFAULT_WORLD = "world";

    private final Map<String, Kingdom> kingdoms = new HashMap<>();
    private final Map<UUID, PlayerMembership> memberships = new HashMap<>();

    public KingdomResult createKingdom(String id, String displayName) {
        String normalised = Kingdom.normaliseId(id);
        if (kingdoms.containsKey(normalised)) {
            return KingdomResult.fail("A kingdom with that id already exists.");
        }
        Kingdom kingdom = new Kingdom(normalised, displayName);
        kingdom.setWorldName(DEFAULT_WORLD);
        kingdoms.put(normalised, kingdom);
        return KingdomResult.ok("Created kingdom " + normalised + ".");
    }

    public Optional<Kingdom> getKingdom(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kingdoms.get(Kingdom.normaliseId(id)));
    }

    public Collection<Kingdom> listKingdoms() {
        return kingdoms.values();
    }

    public Optional<PlayerMembership> getMembership(UUID playerId) {
        return Optional.ofNullable(memberships.get(playerId));
    }

    public KingdomResult joinKingdom(UUID playerId, String kingdomId) {
        if (memberships.containsKey(playerId)) {
            return KingdomResult.fail("You already belong to a kingdom. Ask an operator to move you.");
        }
        Optional<Kingdom> kingdom = getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return KingdomResult.fail("Unknown kingdom. Use /kingdom list.");
        }
        memberships.put(playerId, new PlayerMembership(playerId, kingdom.get().getId()));
        return KingdomResult.ok("You joined " + kingdom.get().getDisplayName() + ".");
    }

    public KingdomResult movePlayer(UUID playerId, String kingdomId) {
        Optional<Kingdom> kingdom = getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return KingdomResult.fail("Unknown kingdom.");
        }
        PlayerMembership membership = memberships.computeIfAbsent(
                playerId,
                id -> new PlayerMembership(id, kingdom.get().getId()));
        membership.setKingdomId(kingdom.get().getId());
        return KingdomResult.ok("Moved player to " + kingdom.get().getDisplayName() + ".");
    }

    public KingdomResult assignTitle(UUID playerId, NobleRank rank, TitleStyle style) {
        if (rank == NobleRank.MP) {
            return KingdomResult.fail("MP seats are filled by election. Use /kingdom election.");
        }
        PlayerMembership membership = memberships.get(playerId);
        if (membership == null) {
            return KingdomResult.fail("That player is not in a kingdom.");
        }
        if (isSlotTakenByAnother(membership, rank, playerId)) {
            return KingdomResult.fail("All " + rank.name().toLowerCase() + " slots are filled in that kingdom.");
        }
        membership.assignTitle(rank, style);
        return KingdomResult.ok("Assigned " + rank.displayTitle(style != null ? style : TitleStyle.MASCULINE) + ".");
    }

    public KingdomResult assignTitleFromElection(UUID playerId, TitleStyle style) {
        PlayerMembership membership = memberships.get(playerId);
        if (membership == null) {
            return KingdomResult.fail("That player is not in a kingdom.");
        }
        NobleRank rank = NobleRank.MP;
        if (isSlotTakenByAnother(membership, rank, playerId)) {
            return KingdomResult.fail("All MP slots are filled in that kingdom.");
        }
        membership.assignTitle(rank, style != null ? style : TitleStyle.MASCULINE);
        return KingdomResult.ok("Elected as MP.");
    }

    public KingdomResult clearTitle(UUID playerId) {
        PlayerMembership membership = memberships.get(playerId);
        if (membership == null) {
            return KingdomResult.fail("That player is not in a kingdom.");
        }
        membership.clearTitle();
        return KingdomResult.ok("Cleared noble title.");
    }

    public String nobleChatPrefix(UUID playerId) {
        return getMembership(playerId).map(PlayerMembership::chatPrefix).orElse("");
    }

    public String coloredNobleChatPrefix(UUID playerId) {
        return getMembership(playerId).map(PlayerMembership::coloredChatPrefix).orElse("");
    }

    public String resolveWorldName(Kingdom kingdom) {
        String worldName = kingdom.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            return DEFAULT_WORLD;
        }
        return worldName;
    }

    public Optional<String> territoryLabel(Kingdom kingdom) {
        if (kingdom.getWorldGuardRegion() == null || kingdom.getWorldGuardRegion().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(kingdom.getWorldGuardRegion() + " (" + resolveWorldName(kingdom) + ")");
    }

    public KingdomResult setKingdomRegion(String kingdomId, String regionName) {
        Optional<Kingdom> kingdom = getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return KingdomResult.fail("Unknown kingdom.");
        }
        kingdom.get().setWorldGuardRegion(regionName);
        return KingdomResult.ok("Linked region " + regionName + " to " + kingdom.get().getDisplayName() + ".");
    }

    public KingdomResult setKingdomWorld(String kingdomId, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return KingdomResult.fail("World name cannot be empty.");
        }
        Optional<Kingdom> kingdom = getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return KingdomResult.fail("Unknown kingdom.");
        }
        kingdom.get().setWorldName(worldName);
        return KingdomResult.ok("Set linked world to " + worldName + " for " + kingdom.get().getDisplayName() + ".");
    }

    public Map<String, Kingdom> getKingdomsView() {
        return Map.copyOf(kingdoms);
    }

    public Map<UUID, PlayerMembership> getMembershipsView() {
        return Map.copyOf(memberships);
    }

    public void replaceState(Map<String, Kingdom> loadedKingdoms, Map<UUID, PlayerMembership> loadedMemberships) {
        kingdoms.clear();
        kingdoms.putAll(loadedKingdoms);
        memberships.clear();
        memberships.putAll(loadedMemberships);
    }

    private boolean isSlotTakenByAnother(PlayerMembership membership, NobleRank rank, UUID playerId) {
        if (!rank.hasSlotLimit()) {
            return false;
        }
        String kingdomId = membership.getKingdomId();
        long occupied = memberships.values().stream()
                .filter(other -> kingdomId.equals(other.getKingdomId()))
                .filter(PlayerMembership::hasNobleTitle)
                .filter(other -> other.getRank() == rank)
                .filter(other -> !other.getPlayerId().equals(playerId))
                .count();
        return occupied >= rank.getMaxPerKingdom();
    }
}
