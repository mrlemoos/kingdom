package dev.mrlemoos.kingdom.resignation;

import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.election.PendingResignation;
import dev.mrlemoos.kingdom.model.election.ResignationSubject;
import dev.mrlemoos.kingdom.model.election.ResignationSubjectKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Supplier;

public final class ResignationService {

    private final KingdomService kingdomService;
    private final ElectionService electionService;
    private final Supplier<Long> clockMs;

    public ResignationService(KingdomService kingdomService, ElectionService electionService) {
        this(kingdomService, electionService, System::currentTimeMillis);
    }

    ResignationService(
            KingdomService kingdomService, ElectionService electionService, Supplier<Long> clockMs) {
        this.kingdomService = kingdomService;
        this.electionService = electionService;
        this.clockMs = clockMs;
    }

    public boolean isElectionBlockingPremier(String kingdomId) {
        return kingdomService
                .getKingdom(kingdomId)
                .map(k -> k.getElectionState().election().isActive())
                .orElse(false);
    }

    public ResignationResult offerResignation(
            String kingdomId, UUID offeredBy, OptionalInt targetedVillagerSeatIndex) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return fail("Unknown kingdom.");
        }
        PlayerMembership membership = kingdomService.getMembership(offeredBy).orElse(null);
        if (membership == null || !kingdomId.equals(membership.getKingdomId())) {
            return fail("You are not in that kingdom.");
        }

        KingdomElectionState electionState = kingdom.get().getElectionState();
        if (electionState.pendingResignation().isPresent()) {
            return fail("A resignation is already awaiting royal approval.");
        }

        Optional<ResignationSubject> subject = resolveSubject(membership, electionState, targetedVillagerSeatIndex);
        if (subject.isEmpty()) {
            return fail("You hold no seat to resign.");
        }

        electionState.setPendingResignation(
                new PendingResignation(subject.get(), offeredBy, clockMs.get()));
        return ok("Your resignation has been offered to the Crown. You remain in office until it is accepted.");
    }

    public ResignationResult acceptResignation(String kingdomId, NobleRank approverRank) {
        if (!ResignationAuthority.canResolveResignation(kingdomId, kingdomService, approverRank)) {
            return fail("Only the monarch, or a Prince when no King or Queen is seated, may accept a resignation.");
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return fail("Unknown kingdom.");
        }

        KingdomElectionState electionState = kingdom.get().getElectionState();
        PendingResignation pending = electionState.pendingResignation().orElse(null);
        if (pending == null) {
            return fail("No resignation awaits royal approval.");
        }

        ResignationSubject subject = pending.subject();
        electionState.clearPendingResignation();

        return switch (subject.kind()) {
            case PLAYER_PREMIER -> completePlayerPremierResignation(kingdomId, subject);
            case PLAYER_MP -> completePlayerMpResignation(kingdomId, electionState, subject);
            case VILLAGER_PREMIER -> completeVillagerPremierResignation(kingdomId, electionState, subject);
            case VILLAGER_MP -> completeVillagerMpResignation(kingdomId, electionState, subject);
        };
    }

    public ResignationResult rejectResignation(String kingdomId, NobleRank approverRank) {
        if (!ResignationAuthority.canResolveResignation(kingdomId, kingdomService, approverRank)) {
            return fail("Only the monarch, or a Prince when no King or Queen is seated, may reject a resignation.");
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return fail("Unknown kingdom.");
        }
        if (kingdom.get().getElectionState().pendingResignation().isEmpty()) {
            return fail("No resignation awaits royal approval.");
        }
        kingdom.get().getElectionState().clearPendingResignation();
        return ok("Resignation rejected. The office-holder remains in post.");
    }

    public Optional<PendingResignation> pendingResignation(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).flatMap(k -> k.getElectionState().pendingResignation());
    }

    private ResignationResult completePlayerPremierResignation(String kingdomId, ResignationSubject subject) {
        UUID premierId = subject.playerId().orElseThrow();
        clearPremierTitle(kingdomId);
        ElectionResult election = electionService.startPremierElection(kingdomId);
        if (election instanceof ElectionResult.Success) {
            return ok("Resignation accepted. A Premier election has been called.");
        }
        return ok("Resignation accepted. The Premier seat is now vacant.");
    }

    private ResignationResult completePlayerMpResignation(
            String kingdomId, KingdomElectionState electionState, ResignationSubject subject) {
        UUID mpId = subject.playerId().orElseThrow();
        int seatIndex = subject.seatIndex().orElseThrow();
        vacatePlayerMpSeat(electionState, mpId, seatIndex);
        kingdomService.clearTitle(mpId);
        ElectionResult election = electionService.startByElection(kingdomId, seatIndex);
        if (election instanceof ElectionResult.Failure failure) {
            return ok("Resignation accepted. " + failure.message());
        }
        return ok("Resignation accepted. A by-election has been called for seat " + seatIndex + ".");
    }

    private ResignationResult completeVillagerPremierResignation(
            String kingdomId, KingdomElectionState electionState, ResignationSubject subject) {
        int seatIndex = subject.seatIndex().orElseThrow();
        if (!electionState.isPremierVillagerSeat(seatIndex)) {
            return fail("That seat is not the Premier villager.");
        }
        electionState.clearPremierVillager();
        ElectionResult election = electionService.startPremierElection(kingdomId);
        if (election instanceof ElectionResult.Success) {
            return ok("Resignation accepted. A Premier election has been called.");
        }
        return ok("Resignation accepted. The Premier villager has left office.");
    }

    private ResignationResult completeVillagerMpResignation(
            String kingdomId, KingdomElectionState electionState, ResignationSubject subject) {
        int seatIndex = subject.seatIndex().orElseThrow();
        MpSeat seat = electionState.seat(seatIndex).orElse(null);
        if (seat == null || seat.kind() != MpSeatKind.VILLAGER) {
            return fail("That villager MP seat is not occupied.");
        }
        if (electionState.isPremierVillagerSeat(seatIndex)) {
            electionState.clearPremierVillager();
        }
        seat.clear();
        ElectionResult election = electionService.startByElection(kingdomId, seatIndex);
        if (election instanceof ElectionResult.Failure failure) {
            return ok("Resignation accepted. " + failure.message());
        }
        return ok("Resignation accepted. A by-election has been called for seat " + seatIndex + ".");
    }

    private Optional<ResignationSubject> resolveSubject(
            PlayerMembership membership, KingdomElectionState electionState, OptionalInt targetedVillagerSeatIndex) {
        if (targetedVillagerSeatIndex.isPresent()) {
            return villagerSeatSubject(electionState, targetedVillagerSeatIndex.getAsInt());
        }
        NobleRank rank = membership.getRank();
        if (rank == NobleRank.PREMIER) {
            return Optional.of(ResignationSubject.playerPremier(membership.getPlayerId()));
        }
        if (rank == NobleRank.MP) {
            OptionalInt seatIndex = electionState.seatIndexForPlayer(membership.getPlayerId());
            if (seatIndex.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(ResignationSubject.playerMp(membership.getPlayerId(), seatIndex.getAsInt()));
        }
        return Optional.empty();
    }

    private Optional<ResignationSubject> villagerSeatSubject(KingdomElectionState electionState, int seatIndex) {
        MpSeat seat = electionState.seat(seatIndex).orElse(null);
        if (seat == null || seat.kind() != MpSeatKind.VILLAGER || !seat.isOccupied()) {
            return Optional.empty();
        }
        boolean premier = electionState.isPremierVillagerSeat(seatIndex);
        return Optional.of(ResignationSubject.villagerSeat(seatIndex, premier));
    }

    private void vacatePlayerMpSeat(KingdomElectionState electionState, UUID playerId, int seatIndex) {
        electionState.seat(seatIndex).ifPresent(seat -> {
            if (seat.kind() == MpSeatKind.PLAYER && seat.playerId().filter(playerId::equals).isPresent()) {
                seat.clear();
            }
        });
    }

    private void clearPremierTitle(String kingdomId) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (kingdomId.equals(membership.getKingdomId()) && membership.getRank() == NobleRank.PREMIER) {
                kingdomService.clearTitle(membership.getPlayerId());
            }
        }
    }

    private static ResignationResult ok(String message) {
        return new ResignationResult.Success(message);
    }

    private static ResignationResult fail(String message) {
        return new ResignationResult.Failure(message);
    }
}
