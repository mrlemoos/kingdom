package dev.mrlemoos.kingdom.city.gui;

/** What the Lord Mayor sees when an applicant walks up to the city hall counter. */
public enum PermitApplicantStatus {
    /** Not a member of this kingdom; may never hold its permit. */
    FOREIGNER,
    /** Serving a prison sentence; no permit may be issued. */
    PRISONER,
    /** King, Queen or Prince of this kingdom; needs no permit at all. */
    EXEMPT,
    /** Already licensed. */
    LICENSED,
    /** A member in good standing who may apply. */
    ELIGIBLE
}
