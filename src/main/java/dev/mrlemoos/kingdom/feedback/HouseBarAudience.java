package dev.mrlemoos.kingdom.feedback;

import dev.mrlemoos.kingdom.model.NobleRank;

/**
 * Who may see a bar that belongs to the House: the Crown, the Premier, the Speaker, and the MPs.
 */
public final class HouseBarAudience {

    private HouseBarAudience() {}

    public static boolean sees(NobleRank rank) {
        return rank == NobleRank.KING
                || rank == NobleRank.QUEEN
                || rank == NobleRank.PREMIER
                || rank == NobleRank.SPEAKER
                || rank == NobleRank.MP;
    }
}
