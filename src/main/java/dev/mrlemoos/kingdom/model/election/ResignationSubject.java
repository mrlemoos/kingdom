package dev.mrlemoos.kingdom.model.election;

import java.util.Optional;
import java.util.UUID;

public record ResignationSubject(
        ResignationSubjectKind kind,
        Optional<UUID> playerId,
        Optional<Integer> seatIndex) {

    public static ResignationSubject playerPremier(UUID playerId) {
        return new ResignationSubject(ResignationSubjectKind.PLAYER_PREMIER, Optional.of(playerId), Optional.empty());
    }

    public static ResignationSubject playerMp(UUID playerId, int seatIndex) {
        return new ResignationSubject(ResignationSubjectKind.PLAYER_MP, Optional.of(playerId), Optional.of(seatIndex));
    }

    public static ResignationSubject villagerSeat(int seatIndex, boolean premier) {
        return new ResignationSubject(
                premier ? ResignationSubjectKind.VILLAGER_PREMIER : ResignationSubjectKind.VILLAGER_MP,
                Optional.empty(),
                Optional.of(seatIndex));
    }
}
