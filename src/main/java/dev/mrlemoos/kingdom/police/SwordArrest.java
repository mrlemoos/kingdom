package dev.mrlemoos.kingdom.police;

/**
 * Whether a strike is an arrest. A sworn constable who strikes a subject wanted by their own realm,
 * inside that realm's jurisdiction, with an iron sword in hand, arrests them; any other strike is
 * ordinary combat and nothing is said.
 */
public enum SwordArrest {
    ARREST,
    PASS;

    public static SwordArrest decide(
            boolean strikerIsConstable, boolean holdingIronSword, boolean targetWanted, boolean inJurisdiction) {
        return strikerIsConstable && holdingIronSword && targetWanted && inJurisdiction ? ARREST : PASS;
    }
}
