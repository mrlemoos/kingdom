package dev.mrlemoos.kingdom.model.election;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class KingdomElectionState {

    private final Map<Integer, MpSeat> seats = new HashMap<>();
    private final Map<Integer, MpSeatLocation> seatLocations = new HashMap<>();
    private final ElectionState election = new ElectionState();
    private long lastGeneralElectionMcDay;
    private Integer premierVillagerSeatIndex;
    private boolean pendingInauguralFiscal;
    private boolean pendingInauguralBudget;
    private PendingResignation pendingResignation;

    public KingdomElectionState() {
        for (int index = 1; index <= 8; index++) {
            seats.put(index, new MpSeat(index));
        }
    }

    public Map<Integer, MpSeat> seatsView() {
        return Map.copyOf(seats);
    }

    public Optional<MpSeat> seat(int index) {
        return Optional.ofNullable(seats.get(index));
    }

    public Map<Integer, MpSeatLocation> seatLocationsView() {
        return Map.copyOf(seatLocations);
    }

    public Optional<MpSeatLocation> seatLocation(int index) {
        return Optional.ofNullable(seatLocations.get(index));
    }

    public void setSeatLocation(int index, MpSeatLocation location) {
        if (index < 1 || index > 8) {
            throw new IllegalArgumentException("Seat index must be 1–8.");
        }
        seatLocations.put(index, location);
    }

    public ElectionState election() {
        return election;
    }

    public long lastGeneralElectionMcDay() {
        return lastGeneralElectionMcDay;
    }

    public void setLastGeneralElectionMcDay(long mcDay) {
        this.lastGeneralElectionMcDay = mcDay;
    }

    public java.util.OptionalInt premierVillagerSeatIndex() {
        return premierVillagerSeatIndex != null ? java.util.OptionalInt.of(premierVillagerSeatIndex) : java.util.OptionalInt.empty();
    }

    public void setPremierVillagerSeatIndex(int seatIndex) {
        if (seatIndex < 1 || seatIndex > 8) {
            throw new IllegalArgumentException("Premier villager seat index must be 1–8.");
        }
        this.premierVillagerSeatIndex = seatIndex;
    }

    public boolean isPremierVillagerSeat(int seatIndex) {
        return premierVillagerSeatIndex != null && premierVillagerSeatIndex == seatIndex;
    }

    public void clearPremierVillager() {
        this.premierVillagerSeatIndex = null;
        this.pendingInauguralFiscal = false;
        this.pendingInauguralBudget = false;
    }

    public boolean pendingInauguralFiscal() {
        return pendingInauguralFiscal;
    }

    public void setPendingInauguralFiscal(boolean pendingInauguralFiscal) {
        this.pendingInauguralFiscal = pendingInauguralFiscal;
    }

    public boolean pendingInauguralBudget() {
        return pendingInauguralBudget;
    }

    public void setPendingInauguralBudget(boolean pendingInauguralBudget) {
        this.pendingInauguralBudget = pendingInauguralBudget;
    }

    public Optional<PendingResignation> pendingResignation() {
        return Optional.ofNullable(pendingResignation);
    }

    public void setPendingResignation(PendingResignation pendingResignation) {
        this.pendingResignation = pendingResignation;
    }

    public void clearPendingResignation() {
        this.pendingResignation = null;
    }

    public OptionalInt seatIndexForPlayer(UUID playerId) {
        for (MpSeat seat : seats.values()) {
            if (seat.kind() == MpSeatKind.PLAYER && seat.playerId().filter(playerId::equals).isPresent()) {
                return OptionalInt.of(seat.index());
            }
        }
        return OptionalInt.empty();
    }

    public OptionalInt seatIndexForVillagerEntity(UUID entityId) {
        for (MpSeat seat : seats.values()) {
            if (seat.kind() == MpSeatKind.VILLAGER && seat.entityId().filter(entityId::equals).isPresent()) {
                return OptionalInt.of(seat.index());
            }
        }
        return OptionalInt.empty();
    }

    public void clearAllSeats() {
        seats.values().forEach(MpSeat::clear);
    }

    public void replaceSeats(Map<Integer, MpSeat> loaded) {
        if (loaded == null) {
            return;
        }
        for (Map.Entry<Integer, MpSeat> entry : loaded.entrySet()) {
            MpSeat target = seats.get(entry.getKey());
            if (target == null) {
                continue;
            }
            MpSeat source = entry.getValue();
            target.clear();
            if (source.kind() == MpSeatKind.PLAYER) {
                source.playerId().ifPresent(target::assignPlayer);
            } else if (source.kind() == MpSeatKind.VILLAGER) {
                target.assignVillager(
                        source.profession().orElse("none"),
                        source.entityId().orElse(null));
                source.originLocation().ifPresent(target::setOriginLocation);
            }
            if (source.returnCount().isPresent()) {
                target.setReturnCount(source.returnCount().getAsInt());
            }
        }
    }

    public void replaceSeatLocations(Map<Integer, MpSeatLocation> loaded) {
        seatLocations.clear();
        if (loaded != null) {
            seatLocations.putAll(loaded);
        }
    }
}
