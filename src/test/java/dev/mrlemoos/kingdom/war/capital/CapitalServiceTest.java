package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A kingdom's {@code Capital} (see the glossary entry in {@code CONTEXT.md}) is a monarch-set
 * WorldGuard subregion id inside linked territory. {@link CapitalService} is the in-memory
 * kingdom-to-capital store for this slice — persistence to {@code data.yml} is a follow-up.
 */
class CapitalServiceTest {

    @Test
    void kingdomWithNoCapitalSetHasNoCapital() {
        CapitalService service = new CapitalService();

        assertFalse(service.hasCapital("southreach"));
        assertEquals(Optional.empty(), service.getCapital("southreach"));
    }

    @Test
    void monarchSetsCapitalRegionOnKingdom() {
        CapitalService service = new CapitalService();

        service.setCapital("southreach", "southreach_capital");

        assertTrue(service.hasCapital("southreach"));
        Optional<CapitalRegion> capital = service.getCapital("southreach");
        assertTrue(capital.isPresent());
        assertEquals("southreach_capital", capital.get().regionId());
    }

    @Test
    void monarchSetsCapitalRegionWithAnExplicitWorld() {
        CapitalService service = new CapitalService();

        service.setCapital("southreach", "southreach_capital", "world_nether");

        Optional<CapitalRegion> capital = service.getCapital("southreach");
        assertTrue(capital.isPresent());
        assertEquals("world_nether", capital.get().worldName());
    }

    @Test
    void settingCapitalAgainReplacesThePreviousRegion() {
        CapitalService service = new CapitalService();
        service.setCapital("southreach", "old_capital");

        service.setCapital("southreach", "new_capital");

        Optional<CapitalRegion> capital = service.getCapital("southreach");
        assertTrue(capital.isPresent());
        assertEquals("new_capital", capital.get().regionId());
    }

    @Test
    void clearingCapitalRemovesIt() {
        CapitalService service = new CapitalService();
        service.setCapital("southreach", "southreach_capital");

        service.clearCapital("southreach");

        assertFalse(service.hasCapital("southreach"));
    }

    @Test
    void blankRegionIdIsRejected() {
        CapitalService service = new CapitalService();

        assertThrows(IllegalArgumentException.class, () -> service.setCapital("southreach", " "));
    }

    @Test
    void differentKingdomsHaveIndependentCapitals() {
        CapitalService service = new CapitalService();
        service.setCapital("southreach", "southreach_capital");

        assertFalse(service.hasCapital("northmarch"));
    }
}
