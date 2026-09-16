package dev.mrlemoos.kingdom.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class RealmCalendarTaskTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void expiresPendingTreatiesOnRealmDayRoll() {
        AtomicLong mcDay = new AtomicLong();
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        RealmCalendarService calendar = new RealmCalendarService(kingdoms, mcDay::get);
        calendar.restore(0L, 0L);
        calendar.seasonTurn().restore(0L);
        TreatyService treaties = new TreatyService(kingdoms, mcDay::get);
        treaties.propose("northmarch", "southreach", TreatyKind.TRADE_PACT, mcDay.get());
        RealmCalendarTask task = new RealmCalendarTask(
                kingdoms, calendar, new YamlKingdomStore(MockBukkit.createMockPlugin()));
        task.setTreatyService(treaties);

        task.run();
        mcDay.set(TreatyService.PROPOSAL_EXPIRY_MC_DAYS);
        task.run();

        assertTrue(treaties.treatiesView().isEmpty());
    }
}
