package dev.leo.kingdom.resignation;

import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;

public final class ResignationAuthority {

    private ResignationAuthority() {}

    public static boolean canResolveResignation(String kingdomId, KingdomService kingdomService, NobleRank rank) {
        if (rank == NobleRank.KING || rank == NobleRank.QUEEN) {
            return hasRankInKingdom(kingdomId, kingdomService, rank);
        }
        if (rank == NobleRank.PRINCE) {
            return !hasMonarchInKingdom(kingdomId, kingdomService)
                    && hasRankInKingdom(kingdomId, kingdomService, NobleRank.PRINCE);
        }
        return false;
    }

    public static Optional<UUID> monarchOrRegent(String kingdomId, KingdomService kingdomService) {
        Optional<UUID> king = findHolder(kingdomId, kingdomService, NobleRank.KING);
        if (king.isPresent()) {
            return king;
        }
        Optional<UUID> queen = findHolder(kingdomId, kingdomService, NobleRank.QUEEN);
        if (queen.isPresent()) {
            return queen;
        }
        return findHolder(kingdomId, kingdomService, NobleRank.PRINCE);
    }

    private static boolean hasMonarchInKingdom(String kingdomId, KingdomService kingdomService) {
        return hasRankInKingdom(kingdomId, kingdomService, NobleRank.KING)
                || hasRankInKingdom(kingdomId, kingdomService, NobleRank.QUEEN);
    }

    private static boolean hasRankInKingdom(String kingdomId, KingdomService kingdomService, NobleRank rank) {
        return findHolder(kingdomId, kingdomService, rank).isPresent();
    }

    private static Optional<UUID> findHolder(String kingdomId, KingdomService kingdomService, NobleRank rank) {
        return kingdomService.getMembershipsView().values().stream()
                .filter(m -> kingdomId.equals(m.getKingdomId()))
                .filter(PlayerMembership::hasNobleTitle)
                .filter(m -> m.getRank() == rank)
                .map(PlayerMembership::getPlayerId)
                .findFirst();
    }
}
