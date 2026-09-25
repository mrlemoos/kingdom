package dev.mrlemoos.kingdom.treaty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreatyServiceTest {

    private TreatyService treaties;
    private KingdomService kingdoms;

    @BeforeEach
    void setUp() {
        kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        treaties = new TreatyService(kingdoms);
    }

    @Test
    void treatyEventsAreEnteredInBothRealmsHansard() {
        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);
        treaties.assent("southreach", "northmarch", TreatyKind.TRADE_PACT, 11);
        treaties.repeal("northmarch", "southreach", TreatyKind.TRADE_PACT, 12);
        treaties.repeal("southreach", "northmarch", TreatyKind.TRADE_PACT, 12);
        treaties.assent("northmarch", "southreach", TreatyKind.NON_AGGRESSION, 13);
        treaties.expire(20);

        for (String realm : new String[] {"northmarch", "southreach"}) {
            var hansard = kingdoms.getKingdom(realm).orElseThrow().getParliamentState().hansardView();
            assertEquals(
                    java.util.List.of("in force", "repealed", "lapsed unanswered"),
                    hansard.stream().map(dev.mrlemoos.kingdom.parliament.HansardRecord::outcome).toList());
            assertEquals("Trade pact between Northmarch and Southreach", hansard.get(0).title());
            assertEquals("treaty", hansard.get(0).business());
            assertEquals(11L, hansard.get(0).decidedOnMcDay());
        }
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
