package dev.mrlemoos.kingdom.model.police;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A police case from arrest through sentence. Domain-only; Bukkit confinement is a thin layer.
 */
public final class PoliceCase {

    private final String id;
    private final String kingdomId;
    private final UUID accusedId;
    private final UUID arrestingConstableId;
    private final String warrantId;
    private final String actBillId;
    private PoliceCaseStatus status;
    private SentenceType sentenceType;
    private double fineAmount;
    private int prisonMinutes;
    private Integer cellSlot;
    private final long openedAtMs;

    public PoliceCase(
            String id,
            String kingdomId,
            UUID accusedId,
            UUID arrestingConstableId,
            String warrantId,
            String actBillId,
            long openedAtMs) {
        this.id = Objects.requireNonNull(id, "id");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.accusedId = Objects.requireNonNull(accusedId, "accusedId");
        this.arrestingConstableId = Objects.requireNonNull(arrestingConstableId, "arrestingConstableId");
        this.warrantId = Objects.requireNonNull(warrantId, "warrantId");
        this.actBillId = actBillId;
        this.status = PoliceCaseStatus.PENDING_TRIAL;
        this.openedAtMs = openedAtMs;
    }

    public String id() {
        return id;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID accusedId() {
        return accusedId;
    }

    public UUID arrestingConstableId() {
        return arrestingConstableId;
    }

    public String warrantId() {
        return warrantId;
    }

    public Optional<String> actBillId() {
        return Optional.ofNullable(actBillId);
    }

    public PoliceCaseStatus status() {
        return status;
    }

    public Optional<SentenceType> sentenceType() {
        return Optional.ofNullable(sentenceType);
    }

    public double fineAmount() {
        return fineAmount;
    }

    public int prisonMinutes() {
        return prisonMinutes;
    }

    public OptionalInt cellSlot() {
        return cellSlot == null ? OptionalInt.empty() : OptionalInt.of(cellSlot);
    }

    public long openedAtMs() {
        return openedAtMs;
    }

    public void applySentence(SentenceType type, double fine, int minutes, Integer assignedCell) {
        this.sentenceType = Objects.requireNonNull(type, "type");
        this.fineAmount = fine;
        this.prisonMinutes = minutes;
        this.cellSlot = assignedCell;
        this.status = type == SentenceType.ACQUITTAL
                ? PoliceCaseStatus.ACQUITTED
                : PoliceCaseStatus.SENTENCED;
    }
}
