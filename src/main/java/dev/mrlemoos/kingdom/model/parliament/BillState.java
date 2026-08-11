package dev.mrlemoos.kingdom.model.parliament;

public enum BillState {
    /** A motion tabled but not yet seconded: it may not be divided upon. */
    AWAITING_SECOND,
    TABLED,
    DIVISION_OPEN,
    PASSED,
    FAILED,
    AWAITING_ASSENT,
    ASSENTED,
    REJECTED
}
