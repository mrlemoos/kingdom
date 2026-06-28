package dev.leo.kingdom.resignation;

import dev.leo.kingdom.model.election.PendingResignation;
import dev.leo.kingdom.model.election.ResignationSubjectKind;

public final class ResignationSummaries {

    private ResignationSummaries() {}

    public static String describe(PendingResignation pending) {
        return switch (pending.subject().kind()) {
            case PLAYER_PREMIER -> "The Premier has offered their resignation.";
            case PLAYER_MP -> "MP seat "
                    + pending.subject().seatIndex().orElse(0)
                    + " has been offered for resignation.";
            case VILLAGER_PREMIER -> "The Premier villager (seat "
                    + pending.subject().seatIndex().orElse(0)
                    + ") has offered their resignation.";
            case VILLAGER_MP -> "Villager MP seat "
                    + pending.subject().seatIndex().orElse(0)
                    + " has been offered for resignation.";
        };
    }
}
