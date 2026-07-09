package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.SentenceType;

/**
 * Default charge suggestions for mechanical Act breaches.
 * Treason remains a manual constable charge — not produced here.
 */
public final class DefaultActBreachCharge {

    private DefaultActBreachCharge() {}

    public record Suggestion(SentenceType preferred, double fineAmount, int prisonMinutes) {}

    public static Suggestion forProvision(ConductKind kind) {
        return switch (kind) {
            case BUILD_BAN -> new Suggestion(SentenceType.FINE, 10.0, 0);
            case CURFEW -> new Suggestion(SentenceType.WARNING, 0, 0);
            case WAR_LIMIT -> new Suggestion(SentenceType.PRISON, 0, 15);
        };
    }
}
