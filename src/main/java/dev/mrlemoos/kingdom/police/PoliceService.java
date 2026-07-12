package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class PoliceService {

    public static final String CONSTABLE_CHAT_COLOR = c("&1");
    public static final String JUDGE_CHAT_COLOR = c("&5");
    public static final String GUARD_CHAT_COLOR = c("&3");

    private final KingdomService kingdomService;
    private final PoliceConfig config;

    public PoliceService(KingdomService kingdomService, PoliceConfig config) {
        this.kingdomService = kingdomService;
        this.config = config;
    }

    public PoliceConfig config() {
        return config;
    }

    public PoliceResult appointConstable(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return PoliceResult.fail("Only the King or Queen may appoint a constable.");
        }
        if (!kingdomService.getMembership(playerId)
                .filter(m -> kingdom.get().getId().equals(m.getKingdomId()))
                .isPresent()) {
            return PoliceResult.fail("That player is not a member of this kingdom.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (police.isJudge(playerId)) {
            return PoliceResult.fail("A judge cannot also serve as constable.");
        }
        if (police.isConstable(playerId)) {
            return PoliceResult.fail("That player is already a constable.");
        }
        police.appointConstable(playerId);
        return PoliceResult.ok("Constable appointed.");
    }

    public PoliceResult dismissConstable(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return PoliceResult.fail("Only the King or Queen may dismiss a constable.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (!police.isConstable(playerId)) {
            return PoliceResult.fail("That player is not a constable.");
        }
        police.dismissConstable(playerId);
        return PoliceResult.ok("Constable dismissed.");
    }

    public PoliceResult appointJudge(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return PoliceResult.fail("Only the King or Queen may appoint a judge.");
        }
        if (!kingdomService.getMembership(playerId)
                .filter(m -> kingdom.get().getId().equals(m.getKingdomId()))
                .isPresent()) {
            return PoliceResult.fail("That player is not a member of this kingdom.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (police.isConstable(playerId)) {
            return PoliceResult.fail("A constable cannot also serve as judge.");
        }
        if (police.isJudge(playerId)) {
            return PoliceResult.fail("That player is already a judge.");
        }
        police.appointJudge(playerId);
        return PoliceResult.ok("Judge appointed.");
    }

    public PoliceResult dismissJudge(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return PoliceResult.fail("Only the King or Queen may dismiss a judge.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (!police.isJudge(playerId)) {
            return PoliceResult.fail("That player is not a judge.");
        }
        police.dismissJudge(playerId);
        return PoliceResult.ok("Judge dismissed.");
    }

    public PoliceResult setCell(
            String kingdomId, NobleRank actorRank, boolean operator, int slot, PrisonCellLocation location) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canConfigureSites(actorRank, operator)) {
            return PoliceResult.fail("Only the King, Queen, or an operator may configure prison cells.");
        }
        if (slot < 1) {
            return PoliceResult.fail("Cell slot must be 1 or greater.");
        }
        if (location == null || location.worldName().isBlank()) {
            return PoliceResult.fail("A valid cell location is required.");
        }
        kingdom.get().getPoliceState().setCell(slot, location);
        return PoliceResult.ok("Prison cell " + slot + " configured.");
    }

    public PoliceResult clearCell(String kingdomId, NobleRank actorRank, boolean operator, int slot) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canConfigureSites(actorRank, operator)) {
            return PoliceResult.fail("Only the King, Queen, or an operator may clear prison cells.");
        }
        if (slot < 1) {
            return PoliceResult.fail("Cell slot must be 1 or greater.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (police.cell(slot).isEmpty()) {
            return PoliceResult.fail("That prison cell is not configured.");
        }
        police.clearCell(slot);
        return PoliceResult.ok("Prison cell " + slot + " cleared.");
    }

    public Optional<PrisonCellLocation> cell(String kingdomId, int slot) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return kingdom.get().getPoliceState().cell(slot);
    }

    public OptionalInt lowestFreeCellSlot(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return OptionalInt.empty();
        }
        return kingdom.get().getPoliceState().lowestFreeCellSlot();
    }

    public PoliceResult setCourt(
            String kingdomId, NobleRank actorRank, boolean operator, CourtLocation courtLocation) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canConfigureSites(actorRank, operator)) {
            return PoliceResult.fail("Only the King, Queen, or an operator may set the court site.");
        }
        if (courtLocation == null || courtLocation.worldName().isBlank()) {
            return PoliceResult.fail("A valid court location is required.");
        }
        kingdom.get().getPoliceState().setCourt(courtLocation);
        return PoliceResult.ok("Court site configured.");
    }

    public boolean hasCourt(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return kingdom.get().getPoliceState().hasCourt();
    }

    public boolean isPoliceReady(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        return police.configuredCellCount() >= 1 && police.hasCourt();
    }

    public PoliceResult registerPatrolGolem(String kingdomId, UUID entityUuid) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (police.isPatrolGolem(entityUuid)) {
            return PoliceResult.fail("That patrol golem is already registered.");
        }
        if (police.patrolGolemCount() >= config.maxPatrolGolems()) {
            return PoliceResult.fail("Patrol golem cap reached (" + config.maxPatrolGolems() + ").");
        }
        police.registerPatrolGolem(entityUuid);
        return PoliceResult.ok("Patrol golem registered.");
    }

    public PoliceResult registerGuardGolem(String kingdomId, UUID entityUuid) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (police.isGuardGolem(entityUuid)) {
            return PoliceResult.fail("That guard golem is already registered.");
        }
        if (police.guardGolemCount() >= config.maxGuardGolems()) {
            return PoliceResult.fail("Guard golem cap reached (" + config.maxGuardGolems() + ").");
        }
        police.registerGuardGolem(entityUuid);
        return PoliceResult.ok("Guard golem registered.");
    }

    public PoliceResult deregisterGolem(String kingdomId, UUID entityUuid) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        if (!police.deregisterGolem(entityUuid)) {
            return PoliceResult.fail("That golem is not registered with the police.");
        }
        return PoliceResult.ok("Golem deregistered.");
    }

    public boolean isConstable(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return kingdom.get().getPoliceState().isConstable(playerId);
    }

    public boolean isJudge(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return kingdom.get().getPoliceState().isJudge(playerId);
    }

    public boolean isRegisteredGolem(String kingdomId, UUID entityUuid) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return kingdom.get().getPoliceState().isRegisteredGolem(entityUuid);
    }

    public String swornChatPrefix(String kingdomId, UUID playerId) {
        if (isConstable(kingdomId, playerId)) {
            return "[Constable] ";
        }
        if (isJudge(kingdomId, playerId)) {
            return "[Judge] ";
        }
        return "";
    }

    public String colouredSwornChatPrefix(String kingdomId, UUID playerId) {
        if (isConstable(kingdomId, playerId)) {
            return CONSTABLE_CHAT_COLOR + "[Constable] ";
        }
        if (isJudge(kingdomId, playerId)) {
            return JUDGE_CHAT_COLOR + "[Judge] ";
        }
        return "";
    }

    public Optional<String> findKingdomForRegisteredGolem(UUID entityUuid) {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getPoliceState().isRegisteredGolem(entityUuid)) {
                return Optional.of(kingdom.getId());
            }
        }
        return Optional.empty();
    }

    public void pruneStalePatrolGolems(String kingdomId, Set<UUID> presentIds) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        Set<UUID> stale = new java.util.HashSet<>(police.patrolGolemsView());
        stale.removeAll(presentIds);
        for (UUID id : stale) {
            police.deregisterGolem(id);
        }
    }

    public void pruneStaleGuardGolems(String kingdomId, Set<UUID> presentIds) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        KingdomPoliceState police = kingdom.get().getPoliceState();
        Set<UUID> stale = new java.util.HashSet<>(police.guardGolemsView());
        stale.removeAll(presentIds);
        for (UUID id : stale) {
            police.deregisterGolem(id);
        }
    }

    public void clearJudgeEntityId(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isPresent()) {
            kingdom.get().getPoliceState().clearJudgeEntityId();
        }
    }

    public void setJudgeEntityId(String kingdomId, UUID entityId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isPresent()) {
            kingdom.get().getPoliceState().setJudgeEntityId(entityId);
        }
    }

    public Optional<UUID> judgeEntityId(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return kingdom.get().getPoliceState().judgeEntityId();
    }

    public Optional<CourtLocation> court(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return kingdom.get().getPoliceState().court();
    }

    public KingdomPoliceState policeState(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return null;
        }
        return kingdom.get().getPoliceState();
    }
}
