package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class PoliceAuthorityTest {

    @Test
    void theCrownSwearsInSwornRoles() {
        assertTrue(PoliceAuthority.canAppointSwornRole(NobleRank.KING));
        assertTrue(PoliceAuthority.canAppointSwornRole(NobleRank.QUEEN));
    }

    @Test
    void noOneBelowTheCrownSwearsInSwornRoles() {
        assertFalse(PoliceAuthority.canAppointSwornRole(NobleRank.PRINCE));
        assertFalse(PoliceAuthority.canAppointSwornRole(NobleRank.DUKE));
        assertFalse(PoliceAuthority.canAppointSwornRole(NobleRank.KNIGHT));
        assertFalse(PoliceAuthority.canAppointSwornRole(null));
    }

    @Test
    void sitingTheCourtAndCellsStaysWithTheCrownOrAnOperator() {
        assertTrue(PoliceAuthority.canConfigureSites(NobleRank.KING, false));
        assertTrue(PoliceAuthority.canConfigureSites(NobleRank.MP, true));
        assertFalse(PoliceAuthority.canConfigureSites(NobleRank.KNIGHT, false));
        assertFalse(PoliceAuthority.canConfigureSites(NobleRank.DUKE, false));
    }

    @Test
    void aKnightMayDeployPoliceGolems() {
        assertTrue(PoliceAuthority.canDeployGolems(NobleRank.KNIGHT, false));
        assertTrue(PoliceAuthority.canDeployGolems(NobleRank.KING, false));
        assertTrue(PoliceAuthority.canDeployGolems(NobleRank.QUEEN, false));
        assertTrue(PoliceAuthority.canDeployGolems(NobleRank.MP, true));
    }

    @Test
    void aKnightMayDeployGolemsYetMayNotSiteACourt() {
        assertTrue(PoliceAuthority.canDeployGolems(NobleRank.KNIGHT, false));
        assertFalse(PoliceAuthority.canConfigureSites(NobleRank.KNIGHT, false));
    }

    @Test
    void ranksWithNoWarrantMayNotDeployPoliceGolems() {
        assertFalse(PoliceAuthority.canDeployGolems(NobleRank.DUKE, false));
        assertFalse(PoliceAuthority.canDeployGolems(NobleRank.LORD, false));
        assertFalse(PoliceAuthority.canDeployGolems(NobleRank.COUNT, false));
        assertFalse(PoliceAuthority.canDeployGolems(null, false));
    }
}
