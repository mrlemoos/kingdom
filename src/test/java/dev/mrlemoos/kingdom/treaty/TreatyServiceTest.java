package dev.mrlemoos.kingdom.treaty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreatyServiceTest {

    private TreatyService treaties;

    @BeforeEach
    void setUp() {
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        treaties = new TreatyService(kingdoms);
    }

    @Test
    void activatesOnlyAfterBothCrownsAssent() {
        treaties.propose("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);

        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);
        assertFalse(treaties.isActive("northmarch", "southreach", TreatyKind.TRADE_PACT));

        treaties.assent("southreach", "northmarch", TreatyKind.TRADE_PACT, 11);
        assertTrue(treaties.isActive("northmarch", "southreach", TreatyKind.TRADE_PACT));
    }

    @Test
    void repealNeedsBothCrownsAssentBeforePactEnds() {
        treaties.propose("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 10);
        treaties.assent("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 10);
        treaties.assent("southreach", "northmarch", TreatyKind.NON_AGGRESSION, 10);

        treaties.repeal("southreach", "northmarch", TreatyKind.NON_AGGRESSION);

        assertTrue(treaties.isActive("northmarch", "southreach", TreatyKind.NON_AGGRESSION));

        treaties.repeal("northmarch", "southreach", TreatyKind.NON_AGGRESSION);

        assertFalse(treaties.isActive("northmarch", "southreach", TreatyKind.NON_AGGRESSION));
    }

    @Test
    void expiresUnassentedProposalAfterSevenInGameDays() {
        treaties.propose("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);
        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 16);

        treaties.expire(17);
        treaties.assent("southreach", "northmarch", TreatyKind.TRADE_PACT, 17);

        assertFalse(treaties.isActive("northmarch", "southreach", TreatyKind.TRADE_PACT));
    }

    @Test
    void expiredRepealLeavesPactActive() {
        treaties.propose("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 10);
        treaties.assent("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 10);
        treaties.assent("southreach", "northmarch", TreatyKind.NON_AGGRESSION, 10);

        treaties.repeal("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 10);
        treaties.expire(17);
        treaties.repeal("southreach", "northmarch", TreatyKind.NON_AGGRESSION, 17);

        assertTrue(treaties.isActive("northmarch", "southreach", TreatyKind.NON_AGGRESSION));
    }
}
