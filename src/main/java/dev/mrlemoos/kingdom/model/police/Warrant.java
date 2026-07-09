package dev.mrlemoos.kingdom.model.police;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import java.util.Objects;
import java.util.UUID;

/**
 * A constable warrant application. Inactive until the Crown approves it.
 */
public final class Warrant {

    private final String id;
    private final String kingdomId;
    private final UUID suspectId;
    private final String actBillId;
    private final ConductKind provisionKind;
    private WarrantStatus status;
    private final long openedAtMs;

    public Warrant(
            String id,
            String kingdomId,
            UUID suspectId,
            String actBillId,
            ConductKind provisionKind,
            WarrantStatus status,
            long openedAtMs) {
        this.id = Objects.requireNonNull(id, "id");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.suspectId = Objects.requireNonNull(suspectId, "suspectId");
        this.actBillId = Objects.requireNonNull(actBillId, "actBillId");
        this.provisionKind = Objects.requireNonNull(provisionKind, "provisionKind");
        this.status = Objects.requireNonNull(status, "status");
        this.openedAtMs = openedAtMs;
    }

    public String id() {
        return id;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID suspectId() {
        return suspectId;
    }

    public String actBillId() {
        return actBillId;
    }

    public ConductKind provisionKind() {
        return provisionKind;
    }

    public WarrantStatus status() {
        return status;
    }

    public void setStatus(WarrantStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public long openedAtMs() {
        return openedAtMs;
    }
}
