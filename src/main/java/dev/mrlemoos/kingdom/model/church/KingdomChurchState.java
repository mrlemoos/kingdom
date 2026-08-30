package dev.mrlemoos.kingdom.model.church;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Church seat, consecration, sworn priest, cleric villager, marriages, funeral records and whether
 * the monarch has been crowned, for one kingdom.
 */
public final class KingdomChurchState {

    private ChurchSite church;
    private boolean consecrated;
    private UUID priestId;
    private UUID clericEntityId;
    private UUID crownedMonarchId;
    private final List<Marriage> marriages = new ArrayList<>();
    private final Map<UUID, FuneralRecord> funerals = new LinkedHashMap<>();
    private final Map<UUID, VillagerFuneralRecord> villagerFunerals = new LinkedHashMap<>();

    // --- the church ------------------------------------------------------

    public Optional<ChurchSite> church() {
        return Optional.ofNullable(church);
    }

    public boolean hasChurch() {
        return church != null;
    }

    /** Siting a church — even in the same place — leaves it unconsecrated. */
    public void setChurch(ChurchSite site) {
        church = site;
        consecrated = false;
    }

    public void clearChurch() {
        church = null;
        consecrated = false;
        clericEntityId = null;
    }

    public boolean isConsecrated() {
        return church != null && consecrated;
    }

    public void consecrate() {
        if (church != null) {
            consecrated = true;
        }
    }

    /** Restores persisted consecration without re-running the rite. */
    public void restoreConsecration(boolean value) {
        consecrated = value && church != null;
    }

    // --- the priesthood --------------------------------------------------

    public Optional<UUID> priestId() {
        return Optional.ofNullable(priestId);
    }

    public boolean isPriest(UUID playerId) {
        return playerId != null && playerId.equals(priestId);
    }

    public void swearPriest(UUID playerId) {
        priestId = playerId;
    }

    public void unswearPriest() {
        priestId = null;
    }

    public Optional<UUID> clericEntityId() {
        return Optional.ofNullable(clericEntityId);
    }

    public void setClericEntityId(UUID entityId) {
        clericEntityId = entityId;
    }

    public void clearClericEntityId() {
        clericEntityId = null;
    }

    // --- the crown -------------------------------------------------------

    public Optional<UUID> crownedMonarchId() {
        return Optional.ofNullable(crownedMonarchId);
    }

    public boolean isCrowned(UUID monarchId) {
        return monarchId != null && monarchId.equals(crownedMonarchId);
    }

    public void crown(UUID monarchId) {
        crownedMonarchId = monarchId;
    }

    public void uncrown() {
        crownedMonarchId = null;
    }

    // --- marriages -------------------------------------------------------

    public List<Marriage> marriagesView() {
        return List.copyOf(marriages);
    }

    public Optional<Marriage> marriageOf(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return marriages.stream().filter(marriage -> marriage.includes(playerId)).findFirst();
    }

    public Optional<UUID> spouseOf(UUID playerId) {
        return marriageOf(playerId).map(marriage -> marriage.spouseOf(playerId));
    }

    public boolean isMarried(UUID playerId) {
        return marriageOf(playerId).isPresent();
    }

    /** @return false when either party is already wed. */
    public boolean wed(Marriage marriage) {
        if (marriage == null || isMarried(marriage.first()) || isMarried(marriage.second())) {
            return false;
        }
        return marriages.add(marriage);
    }

    /** @return true when a marriage was actually dissolved. */
    public boolean dissolve(UUID playerId) {
        Optional<Marriage> marriage = marriageOf(playerId);
        return marriage.isPresent() && marriages.remove(marriage.get());
    }

    public void replaceMarriages(List<Marriage> loaded) {
        marriages.clear();
        if (loaded != null) {
            marriages.addAll(loaded);
        }
    }

    // --- funerals --------------------------------------------------------

    public Optional<FuneralRecord> funeralRecord(UUID playerId) {
        return playerId == null ? Optional.empty() : Optional.ofNullable(funerals.get(playerId));
    }

    /** One record to a player: a later death overwrites the earlier one. */
    public void holdFuneralRecord(UUID playerId, FuneralRecord record) {
        if (playerId != null && record != null) {
            funerals.put(playerId, record);
        }
    }

    public Optional<FuneralRecord> takeFuneralRecord(UUID playerId) {
        return playerId == null ? Optional.empty() : Optional.ofNullable(funerals.remove(playerId));
    }

    public Map<UUID, FuneralRecord> funeralRecordsView() {
        return Map.copyOf(funerals);
    }

    public void replaceFuneralRecords(Map<UUID, FuneralRecord> loaded) {
        funerals.clear();
        if (loaded != null) {
            funerals.putAll(loaded);
        }
    }

    public Optional<VillagerFuneralRecord> villagerFuneralRecord(UUID villagerId) {
        return villagerId == null ? Optional.empty() : Optional.ofNullable(villagerFunerals.get(villagerId));
    }

    public void holdVillagerFuneralRecord(UUID villagerId, VillagerFuneralRecord record) {
        if (villagerId != null && record != null) {
            villagerFunerals.put(villagerId, record);
        }
    }

    public Optional<VillagerFuneralRecord> takeVillagerFuneralRecord(UUID villagerId) {
        return villagerId == null ? Optional.empty() : Optional.ofNullable(villagerFunerals.remove(villagerId));
    }

    public Map<UUID, VillagerFuneralRecord> villagerFuneralRecordsView() {
        return Map.copyOf(villagerFunerals);
    }

    public void replaceVillagerFuneralRecords(Map<UUID, VillagerFuneralRecord> loaded) {
        villagerFunerals.clear();
        if (loaded != null) {
            villagerFunerals.putAll(loaded);
        }
    }
}
