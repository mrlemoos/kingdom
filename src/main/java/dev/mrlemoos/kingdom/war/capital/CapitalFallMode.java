package dev.mrlemoos.kingdom.war.capital;

/**
 * Whether a capital-fall war aim (see the glossary entry in {@code CONTEXT.md}) is satisfied by
 * a bare majority or the entirety of the defender's capital subregion chunks. Named in the war
 * bill; kept here rather than on {@code ActiveWar}/{@code BillPayload.War} for this slice — that
 * wiring is a follow-up (see docs/build-order.md Slice 6.4), so {@link WarAimEvaluator} takes the
 * mode as an evaluation-time argument instead.
 */
public enum CapitalFallMode {
    MAJORITY,
    TOTAL
}
