package dev.mrlemoos.kingdom.model.parliament;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AssentedAct(
        String billId,
        String title,
        BillType type,
        long assentedAtMs,
        List<String> bookPages,
        Map<UUID, VoteChoice> divisionVotes,
        VoteChoice speakerCastingVote,
        String shelfWorld,
        int shelfBlockX,
        int shelfBlockY,
        int shelfBlockZ,
        int shelfSlot,
        List<ConductProvision> conductProvisions) {

    public AssentedAct {
        bookPages = List.copyOf(bookPages);
        divisionVotes = Map.copyOf(divisionVotes);
        conductProvisions = conductProvisions == null ? List.of() : List.copyOf(conductProvisions);
    }
}
