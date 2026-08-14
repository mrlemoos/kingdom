package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.service.KingdomResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Capitals, the oath of allegiance, and build permits. A kingdom with no capital set is not gated
 * at all; once a capital exists, new members swear at city hall, and only permit holders and the
 * kingdom's own monarch or princes may build in its territory.
 */
public final class CityService {

    private final KingdomService kingdomService;
    private PrisonStatusPort prisonStatusPort;

    public CityService(KingdomService kingdomService) {
        this(kingdomService, PrisonStatusPort.none());
    }

    public CityService(KingdomService kingdomService, PrisonStatusPort prisonStatusPort) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.prisonStatusPort = Objects.requireNonNull(prisonStatusPort, "prisonStatusPort");
    }

    public void setPrisonStatusPort(PrisonStatusPort port) {
        this.prisonStatusPort = Objects.requireNonNull(port, "prisonStatusPort");
    }

    /** Whether the city office considers this player to be serving a prison sentence. */
    public boolean isUnderPrisonSentence(UUID playerId) {
        return playerId != null && prisonStatusPort.isUnderPrisonSentence(playerId);
    }

    /** True when the King, Queen or a Prince/Princess of their own kingdom holds this rank. */
    public static boolean isRoyalExempt(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }

    // --- capital ---------------------------------------------------------

    public Optional<CapitalLocation> capital(String kingdomId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        if (city.isEmpty()) {
            return Optional.empty();
        }
        return city.get().capital();
    }

    /** Permit enforcement only applies to a kingdom that has designated a capital. */
    public boolean enforcementActive(String kingdomId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        return city.isPresent() && city.get().hasCapital();
    }

    public CityResult setCapital(String kingdomId, NobleRank actorRank, CapitalLocation location) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!isRoyalExempt(actorRank)) {
            return CityResult.fail("Only the King, Queen or a Prince may set the capital.");
        }
        if (location == null) {
            return CityResult.fail("A capital needs a location.");
        }
        KingdomCityState city = kingdom.get().getCityState();
        boolean moved = city.hasCapital();
        city.setCapital(location);
        return CityResult.ok(moved ? "The capital has been moved." : "The capital has been established.");
    }

    public CityResult clearCapital(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!isRoyalExempt(actorRank)) {
            return CityResult.fail("Only the King, Queen or a Prince may clear the capital.");
        }
        KingdomCityState city = kingdom.get().getCityState();
        if (!city.hasCapital()) {
            return CityResult.fail("This kingdom has no capital.");
        }
        city.clearCapital();
        return CityResult.ok("The capital has been dissolved. Build permits are no longer required.");
    }

    // --- oath of allegiance ----------------------------------------------

    /** True when this kingdom has a city hall and so will not take {@code /kingdom join}. */
    public boolean requiresOathAtHall(String kingdomId) {
        return enforcementActive(kingdomId);
    }

    /** The hall-join refusal for this kingdom, when a capital stands. */
    public Optional<String> hallJoinRefusal(String kingdomId) {
        if (!requiresOathAtHall(kingdomId)) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AllegianceOath.hallJoinRefusal(kingdom.get().getDisplayName()));
    }

    /**
     * The civil oath at city hall: unaffiliated players become members. Does not grant a build
     * permit.
     */
    public CityResult swearAllegiance(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!kingdom.get().getCityState().hasCapital()) {
            return CityResult.fail("This kingdom has no city hall.");
        }
        KingdomResult joined = kingdomService.joinKingdom(playerId, kingdom.get().getId());
        if (joined instanceof KingdomResult.Failure failure) {
            return CityResult.fail(failure.message());
        }
        return CityResult.ok("You have sworn allegiance and are a member of "
                + kingdom.get().getDisplayName()
                + ".");
    }

    // --- town crier stand ------------------------------------------------

    /** Where the Town Crier should stand: separate site if set, otherwise the capital. */
    public Optional<CapitalLocation> crierStand(String kingdomId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        if (city.isEmpty()) {
            return Optional.empty();
        }
        return city.get().crierStand();
    }

    public boolean hasSeparateCrierStand(String kingdomId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        return city.isPresent() && city.get().hasSeparateCrierStand();
    }

    public CityResult setTownCrierStand(String kingdomId, NobleRank actorRank, CapitalLocation location) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!isRoyalExempt(actorRank)) {
            return CityResult.fail("Only the King, Queen or a Prince may site the Town Crier.");
        }
        KingdomCityState city = kingdom.get().getCityState();
        if (!city.hasCapital()) {
            return CityResult.fail("Site a capital before placing the Town Crier.");
        }
        if (location == null) {
            return CityResult.fail("The Town Crier needs a place to stand.");
        }
        boolean moved = city.hasSeparateCrierStand();
        city.setTownCrierStand(location);
        return CityResult.ok(moved
                ? "The Town Crier has been moved."
                : "The Town Crier has been stood apart from the city hall.");
    }

    public CityResult clearTownCrierStand(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!isRoyalExempt(actorRank)) {
            return CityResult.fail("Only the King, Queen or a Prince may clear the Town Crier's stand.");
        }
        KingdomCityState city = kingdom.get().getCityState();
        if (!city.hasCapital()) {
            return CityResult.fail("This kingdom has no capital.");
        }
        if (city.crierStand().isEmpty()) {
            return CityResult.fail("This kingdom has no Town Crier.");
        }
        city.clearTownCrierStand();
        return CityResult.ok("The Town Crier has been dismissed.");
    }

    // --- permits ---------------------------------------------------------

    public boolean hasPermit(String kingdomId, UUID playerId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        return city.isPresent() && city.get().hasPermit(playerId);
    }

    public Map<UUID, Long> permitsView(String kingdomId) {
        Optional<KingdomCityState> city = cityState(kingdomId);
        if (city.isEmpty()) {
            return Map.of();
        }
        return city.get().permitsView();
    }

    /** Free, immediate and idempotent. Members only; never a foreigner, never a prisoner. */
    public CityResult grantPermit(String kingdomId, UUID playerId) {
        return grantPermit(kingdomId, playerId, System.currentTimeMillis());
    }

    public CityResult grantPermit(String kingdomId, UUID playerId, long grantedAtMs) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (playerId == null) {
            return CityResult.fail("Unknown player.");
        }
        if (!isMember(kingdom.get().getId(), playerId)) {
            return CityResult.fail("Foreigners may not hold a build permit in this kingdom.");
        }
        if (prisonStatusPort.isUnderPrisonSentence(playerId)) {
            return CityResult.fail("A prisoner may not be issued a build permit.");
        }
        KingdomCityState city = kingdom.get().getCityState();
        if (!city.grantPermit(playerId, grantedAtMs)) {
            return CityResult.ok("That player already holds a build permit.");
        }
        RealmFeedback.permitGranted(playerId);
        return CityResult.ok("Build permit granted.");
    }

    /** Entry point for manual revocation, imprisonment and leaving the kingdom. */
    public CityResult revokePermit(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (!kingdom.get().getCityState().revokePermit(playerId)) {
            return CityResult.fail("That player holds no build permit.");
        }
        RealmFeedback.permitRevoked(playerId);
        return CityResult.ok("Build permit revoked.");
    }

    /** Revokes a player's permit in every kingdom that holds one; returns the number revoked. */
    public int revokeAllPermits(UUID playerId) {
        int revoked = 0;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getCityState().revokePermit(playerId)) {
                revoked++;
            }
        }
        return revoked;
    }

    // --- the gate --------------------------------------------------------

    /** Resolves membership and rank from the roll, then applies {@link #mayBuild}. */
    public boolean mayBuild(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return true;
        }
        String resolvedId = kingdom.get().getId();
        Optional<PlayerMembership> membership =
                playerId == null ? Optional.empty() : kingdomService.getMembership(playerId);
        boolean member = membership.isPresent() && resolvedId.equals(membership.get().getKingdomId());
        NobleRank rank = member ? membership.get().getRank() : null;
        return mayBuild(resolvedId, playerId, rank, member, false);
    }

    /**
     * @param rank the player's rank in {@code kingdomId}, or null when untitled or a foreigner
     * @param member whether the player belongs to {@code kingdomId}
     * @param operator ignored: operators are deliberately not exempt from this gate
     */
    public boolean mayBuild(String kingdomId, UUID playerId, NobleRank rank, boolean member, boolean operator) {
        if (!enforcementActive(kingdomId)) {
            return true;
        }
        if (!member) {
            return false;
        }
        if (rank != null && isRoyalExempt(rank)) {
            return true;
        }
        return hasPermit(kingdomId, playerId);
    }

    private boolean isMember(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent() && kingdomId.equals(membership.get().getKingdomId());
    }

    private Optional<KingdomCityState> cityState(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(kingdom.get().getCityState());
    }
}
