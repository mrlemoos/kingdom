package dev.leo.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.TitleStyle;
import dev.leo.kingdom.model.parliament.BillState;
import dev.leo.kingdom.model.parliament.ChamberSite;
import dev.leo.kingdom.model.parliament.VoteChoice;
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
