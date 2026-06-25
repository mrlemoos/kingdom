package dev.leo.kingdom.model.election;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MpSeat {

    private final int index;
    private MpSeatKind kind;
    private UUID playerId;
    private String profession;
    private UUID entityId;

    public MpSeat(int index) {
        if (index < 1 || index > 8) {
            throw new IllegalArgumentException("Seat index must be 1–8.");
        }
        this.index = index;
    }

    public int index() {
        return index;
    }

    public MpSeatKind kind() {
        return kind;
    }

    public Optional<UUID> playerId() {
        return Optional.ofNullable(playerId);
    }

    public Optional<String> profession() {
        return Optional.ofNullable(profession);
    }

    public Optional<UUID> entityId() {
        return Optional.ofNullable(entityId);
    }

    public boolean isOccupied() {
        return kind != null;
    }

    public void assignPlayer(UUID holder) {
        this.kind = MpSeatKind.PLAYER;
        this.playerId = Objects.requireNonNull(holder, "holder");
        this.profession = null;
        this.entityId = null;
    }

    public void assignVillager(String professionName, UUID entityId) {
        this.kind = MpSeatKind.VILLAGER;
        this.profession = Objects.requireNonNull(professionName, "profession");
        this.entityId = entityId;
        this.playerId = null;
    }

    public void clear() {
        this.kind = null;
        this.playerId = null;
        this.profession = null;
        this.entityId = null;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
}
