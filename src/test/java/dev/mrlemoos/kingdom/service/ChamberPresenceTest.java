package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChamberPresenceTest {

    @Test
    void withinRadiusWhenInsideChamber() {
        ChamberSite commons = ChamberSite.of("world", 100, 64, 200);

        assertTrue(ChamberPresence.withinChamber(commons, "world", 108, 64, 208, 16));
    }

    @Test
    void outsideWhenWrongWorld() {
        ChamberSite commons = ChamberSite.of("world", 100, 64, 200);

        assertTrue(!ChamberPresence.withinChamber(commons, "world_nether", 100, 64, 200, 16));
    }

    @Test
    void outsideWhenBeyondRadius() {
        ChamberSite commons = ChamberSite.of("world", 100, 64, 200);

        assertTrue(!ChamberPresence.withinChamber(commons, "world", 130, 64, 200, 16));
    }
}
