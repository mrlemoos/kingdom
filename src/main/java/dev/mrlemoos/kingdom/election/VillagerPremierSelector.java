package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import java.util.Map;
import java.util.OptionalInt;

public final class VillagerPremierSelector {

    private VillagerPremierSelector() {}

    public static OptionalInt selectPremierSeat(KingdomElectionState electionState, Map<String, Integer> professionCounts) {
        if (electionState == null || professionCounts == null) {
            return OptionalInt.empty();
        }

        int winningSeatIndex = -1;
        int winningCount = -1;

        for (MpSeat seat : electionState.seatsView().values()) {
            if (seat.kind() != MpSeatKind.VILLAGER || seat.profession().isEmpty()) {
                continue;
            }
            String profession = seat.profession().orElseThrow();
            if (ProfessionConstituencyResolver.CITIZEN_PROFESSION.equals(profession)) {
                continue;
            }

            int count = professionCounts.getOrDefault(profession, 0);
            if (count > winningCount || (count == winningCount && seat.index() < winningSeatIndex)) {
                winningCount = count;
                winningSeatIndex = seat.index();
            }
        }

        return winningSeatIndex > 0 ? OptionalInt.of(winningSeatIndex) : OptionalInt.empty();
    }
}
