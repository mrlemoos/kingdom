package dev.leo.kingdom.service;

import dev.leo.kingdom.model.parliament.VoteChoice;
import java.util.Map;
import java.util.UUID;

final class VoteTally {

    private final int aye;
    private final int nay;

    private VoteTally(int aye, int nay) {
        this.aye = aye;
        this.nay = nay;
    }

    static VoteTally from(Map<UUID, VoteChoice> votes) {
        int aye = 0;
        int nay = 0;
        for (VoteChoice choice : votes.values()) {
            if (choice == VoteChoice.AYE) {
                aye++;
            } else if (choice == VoteChoice.NAY) {
                nay++;
            }
        }
        return new VoteTally(aye, nay);
    }

    int aye() {
        return aye;
    }

    int nay() {
        return nay;
    }
}
