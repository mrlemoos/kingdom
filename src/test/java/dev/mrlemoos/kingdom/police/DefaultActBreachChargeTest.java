package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import org.junit.jupiter.api.Test;

class DefaultActBreachChargeTest {

    @Test
    void buildBanSuggestsFine() {
        DefaultActBreachCharge.Suggestion suggestion =
                DefaultActBreachCharge.forProvision(ConductKind.BUILD_BAN);
        assertEquals(SentenceType.FINE, suggestion.preferred());
        assertEquals(10.0, suggestion.fineAmount(), 1e-9);
    }

    @Test
    void curfewSuggestsWarning() {
        assertEquals(
                SentenceType.WARNING,
                DefaultActBreachCharge.forProvision(ConductKind.CURFEW).preferred());
    }

    @Test
    void warLimitSuggestsPrison() {
        DefaultActBreachCharge.Suggestion suggestion =
                DefaultActBreachCharge.forProvision(ConductKind.WAR_LIMIT);
        assertEquals(SentenceType.PRISON, suggestion.preferred());
        assertEquals(15, suggestion.prisonMinutes());
    }
}
