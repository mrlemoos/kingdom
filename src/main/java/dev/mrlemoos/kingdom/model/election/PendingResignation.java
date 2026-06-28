package dev.mrlemoos.kingdom.model.election;

import java.util.Objects;
import java.util.UUID;

public final class PendingResignation {

    private final ResignationSubject subject;
    private final UUID offeredBy;
    private final long offeredAtMs;

    public PendingResignation(ResignationSubject subject, UUID offeredBy, long offeredAtMs) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.offeredBy = Objects.requireNonNull(offeredBy, "offeredBy");
        this.offeredAtMs = offeredAtMs;
    }

    public ResignationSubject subject() {
        return subject;
    }

    public UUID offeredBy() {
        return offeredBy;
    }

    public long offeredAtMs() {
        return offeredAtMs;
    }
}
