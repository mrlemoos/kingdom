package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Vacates elected offices on prison without a resignation letter — Premier election or Commons
 * by-election as appropriate. Also clears villager MP/Premier seats by entity id.
 */
public final class PrisonElectedOfficeVacator implements ElectedOfficeVacator {

    private final KingdomService kingdomService;
    private final ElectionService electionService;

    public PrisonElectedOfficeVacator(KingdomService kingdomService, ElectionService electionService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.electionService = Objects.requireNonNull(electionService, "electionService");
    }

    @Override
    public void vacateOnPrison(String kingdomId, UUID convictId, NobleRank vacatedRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        KingdomElectionState electionState = kingdom.get().getElectionState();

        if (vacatedRank == NobleRank.MP || vacatedRank == null) {
            OptionalInt playerSeat = electionState.seatIndexForPlayer(convictId);
            if (playerSeat.isPresent()) {
                electionState.seat(playerSeat.getAsInt()).ifPresent(MpSeat::clear);
                electionService.startByElection(kingdomId, playerSeat.getAsInt());
                return;
            }
            OptionalInt villagerSeat = seatIndexForVillagerEntity(electionState, convictId);
            if (villagerSeat.isPresent()) {
                int seatIndex = villagerSeat.getAsInt();
                boolean wasPremier = electionState.isPremierVillagerSeat(seatIndex);
                if (wasPremier) {
                    electionState.clearPremierVillager();
                }
                electionState.seat(seatIndex).ifPresent(MpSeat::clear);
                if (wasPremier) {
                    electionService.startPremierElection(kingdomId);
                } else {
                    electionService.startByElection(kingdomId, seatIndex);
                }
            }
            return;
        }

        if (vacatedRank == NobleRank.PREMIER) {
            electionService.startPremierElection(kingdomId);
            return;
        }

        // Player Speaker: title already cleared; villager Speaker sweep will preside.
    }

    private static OptionalInt seatIndexForVillagerEntity(KingdomElectionState state, UUID entityId) {
        for (MpSeat seat : state.seatsView().values()) {
            if (seat.kind() == MpSeatKind.VILLAGER
                    && seat.entityId().filter(entityId::equals).isPresent()) {
                return OptionalInt.of(seat.index());
            }
        }
        return OptionalInt.empty();
    }
}
