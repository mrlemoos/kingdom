package dev.mrlemoos.kingdom.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.police.JurisdictionPort;
import dev.mrlemoos.kingdom.police.MechanicalJusticeConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.WantedNametagPolicy;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerPrefixComposerTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private String territoryKingdomId;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        territoryKingdomId = null;

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SUSPECT, NobleRank.KNIGHT, TitleStyle.MASCULINE);
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt(
                "northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
    }

    @Test
    void wantedReplacesNoblePrefixInsideJurisdictionWithActiveWarrant() {
        openAndApproveWarrant();
        territoryKingdomId = "northmarch";

        PlayerPrefixComposer composer = composer();

        assertEquals(WantedNametagPolicy.colouredWantedPrefix(), composer.fullColouredPrefix(SUSPECT));
    }

    @Test
    void outsideJurisdictionKeepsNoblePrefixDespiteActiveWarrant() {
        openAndApproveWarrant();
        territoryKingdomId = null;

        PlayerPrefixComposer composer = composer();

        String prefix = composer.fullColouredPrefix(SUSPECT);
        assertTrue(prefix.contains("[Knight]"));
        assertTrue(!prefix.contains("[WANTED]"));
    }

    @Test
    void wantedClearsWhenWarrantServed() {
        openAndApproveWarrant();
        territoryKingdomId = "northmarch";
        Warrant active = justice.findActiveForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.markWarrantServed("northmarch", active.id());

        assertTrue(!composer().fullColouredPrefix(SUSPECT).contains("[WANTED]"));
    }

    private PlayerPrefixComposer composer() {
        JurisdictionPort port = playerId -> Optional.ofNullable(territoryKingdomId);
        return new PlayerPrefixComposer(kingdomService, policeService, justice, port);
    }

    private void openAndApproveWarrant() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
    }
}
