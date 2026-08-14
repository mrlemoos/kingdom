package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import java.util.Optional;

/**
 * Which city-hall counter a visitor is shown when they right-click the Lord Mayor.
 */
public final class LordMayorCounter {

    public enum Action {
        OATH,
        APPLY,
        REGISTER,
        FOREIGN_MEMBER
    }

    private LordMayorCounter() {}

    public static Action action(Optional<PlayerMembership> membership, String mayorKingdomId) {
        if (membership.isEmpty()) {
            return Action.OATH;
        }
        PlayerMembership visitor = membership.get();
        if (!mayorKingdomId.equals(visitor.getKingdomId())) {
            return Action.FOREIGN_MEMBER;
        }
        if (isRegistrar(visitor, mayorKingdomId)) {
            return Action.REGISTER;
        }
        return Action.APPLY;
    }

    public static boolean isRegistrar(PlayerMembership membership, String mayorKingdomId) {
        if (!mayorKingdomId.equals(membership.getKingdomId())) {
            return false;
        }
        NobleRank rank = membership.getRank();
        return rank != null && CityService.isRoyalExempt(rank);
    }
}
