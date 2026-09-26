package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ParliamentSitingTest {

    private KingdomService kingdomService;
    private ParliamentSiting siting;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        siting = new ParliamentSiting(
                new ParliamentService(kingdomService),
                kingdomService,
                new YamlKingdomStore(MockBukkit.createMockPlugin()),
                null);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Kingdom kingdom() {
        return kingdomService.getKingdom("northmarch").orElseThrow();
    }

    private static MpSeatLocation at(int x) {
        return new MpSeatLocation("world", x, 64, 0, 0f, 0f);
    }

    @Test
    void seatStonesFillTheHouseInOrderAndAClearedSeatIsFilledNext() {
        for (int seat = 1; seat <= 8; seat++) {
            assertInstanceOf(ParliamentResult.Success.class, siting.fillNextSeat("northmarch", at(seat)));
        }
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), siting.setSeats("northmarch"));

        assertInstanceOf(ParliamentResult.Success.class, siting.clearMpSeat("northmarch", 3));
        assertEquals(OptionalInt.of(3), siting.nextEmptySeat("northmarch"));
        siting.fillNextSeat("northmarch", at(30));

        assertEquals(30.0, kingdom().getElectionState().seatLocation(3).orElseThrow().x());
    }

    @Test
    void aNinthSeatIsRefused() {
        for (int seat = 1; seat <= 8; seat++) {
            siting.fillNextSeat("northmarch", at(seat));
        }

        ParliamentResult ninth = siting.fillNextSeat("northmarch", at(9));

        assertEquals(new ParliamentResult.Failure(ParliamentSiting.fullHouse()), ninth);
    }

    @Test
    void aChamberIsClearedAndAnUnsetOneRefused() {
        siting.setCommons("northmarch", ChamberSite.of("world", 1, 64, 1));

        assertInstanceOf(ParliamentResult.Success.class, siting.clear("northmarch", FoundationStone.COMMONS));
        assertTrue(kingdom().getParliamentSites().commons().isEmpty());
        assertEquals(
                new ParliamentResult.Failure("The House of Commons is not yet sited."),
                siting.clear("northmarch", FoundationStone.COMMONS));
    }

    @Test
    void theLordsStoneStoresItsDesignAsTheKingdomFlag() {
        KingdomFlag design = KingdomFlag.plain("BLUE_BANNER");

        ParliamentSiting.Lords lords = siting.setLords("northmarch", ChamberSite.of("world", 5, 64, 5), Optional.of(design));

        assertInstanceOf(ParliamentResult.Success.class, lords.result());
        assertEquals(Optional.of(design), kingdom().getFlag());
        siting.setLords("northmarch", ChamberSite.of("world", 6, 64, 6), Optional.empty());
        assertEquals(Optional.of(design), kingdom().getFlag());
    }
}
