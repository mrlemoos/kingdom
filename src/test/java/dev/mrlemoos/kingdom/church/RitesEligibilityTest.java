package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RitesEligibilityTest {

    private static RitesEligibility.Builder subjectAtConsecratedChurch() {
        return RitesEligibility.builder().member(true).atChurch(true).consecrated(true);
    }

    @Test
    void aNonMemberIsOfferedNoRites() {
        List<Rite> rites = RitesEligibility.builder()
                .member(false)
                .atChurch(true)
                .consecrated(true)
                .crown(true)
                .marriagesStanding(true)
                .partnerAtChurch(true)
                .heldExperience(true)
                .villagerAwaitingRites(true)
                .build()
                .rites();

        assertTrue(rites.isEmpty(), rites.toString());
    }

    @Test
    void anUnconsecratedChurchOffersOnlyItsConsecration() {
        List<Rite> rites = RitesEligibility.builder()
                .member(true)
                .atChurch(true)
                .consecrated(false)
                .partnerAtChurch(true)
                .heldExperience(true)
                .villagerAwaitingRites(true)
                .build()
                .rites();

        assertEquals(List.of(Rite.CONSECRATE), rites);
    }

    @Test
    void aConsecratedChurchIsNotConsecratedAgain() {
        assertTrue(!subjectAtConsecratedChurch().build().rites().contains(Rite.CONSECRATE));
    }

    @Test
    void anUnmarriedSubjectIsOfferedMarriageOnlyWhenAPartnerStandsAtTheChurch() {
        assertEquals(List.of(), subjectAtConsecratedChurch().build().rites());
        assertEquals(
                List.of(Rite.MARRY),
                subjectAtConsecratedChurch().partnerAtChurch(true).build().rites());
    }

    @Test
    void aMarriedSubjectIsOfferedDivorceAndNotMarriage() {
        List<Rite> rites =
                subjectAtConsecratedChurch().married(true).partnerAtChurch(true).build().rites();

        assertEquals(List.of(Rite.DIVORCE), rites);
    }

    @Test
    void funeralsAreOfferedOnlyWhereThereIsSomethingToBury() {
        assertEquals(
                List.of(Rite.FUNERAL, Rite.VILLAGER_FUNERAL),
                subjectAtConsecratedChurch().heldExperience(true).villagerAwaitingRites(true).build().rites());
    }

    @Test
    void annulmentIsTheCrownsAloneAndWantsAMarriageToAnnul() {
        assertEquals(List.of(), subjectAtConsecratedChurch().marriagesStanding(true).build().rites());
        assertEquals(List.of(), subjectAtConsecratedChurch().crown(true).build().rites());
        assertEquals(
                List.of(Rite.ANNUL),
                subjectAtConsecratedChurch().crown(true).marriagesStanding(true).build().rites());
    }

    @Test
    void awayFromTheAltarOnlyTheCrownsAnnulmentRemains() {
        List<Rite> rites = RitesEligibility.builder()
                .member(true)
                .atChurch(false)
                .consecrated(true)
                .crown(true)
                .marriagesStanding(true)
                .partnerAtChurch(true)
                .heldExperience(true)
                .build()
                .rites();

        assertEquals(List.of(Rite.ANNUL), rites);
    }

    @Test
    void ritesAreListedInTheirOwnOrder() {
        List<Rite> rites = subjectAtConsecratedChurch()
                .crown(true)
                .marriagesStanding(true)
                .married(true)
                .heldExperience(true)
                .villagerAwaitingRites(true)
                .build()
                .rites();

        assertEquals(List.of(Rite.DIVORCE, Rite.ANNUL, Rite.FUNERAL, Rite.VILLAGER_FUNERAL), rites);
    }
}
