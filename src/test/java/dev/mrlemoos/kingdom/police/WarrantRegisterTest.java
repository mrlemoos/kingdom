package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WarrantRegisterTest {

    private static Warrant warrant(String id, String kingdomId, WarrantStatus status, long openedAtMs) {
        return new Warrant(id, kingdomId, UUID.randomUUID(), "act-1", ConductKind.CURFEW, status, openedAtMs);
    }

    @Test
    void listsOnlyTheRealmsActiveWarrantsOldestFirst() {
        Warrant later = warrant("w2", "north", WarrantStatus.ACTIVE, 200L);
        Warrant earlier = warrant("w1", "north", WarrantStatus.ACTIVE, 100L);
        List<Warrant> all = List.of(
                later,
                warrant("w3", "north", WarrantStatus.PENDING_CROWN, 50L),
                warrant("w4", "north", WarrantStatus.SERVED, 60L),
                warrant("w5", "north", WarrantStatus.CANCELLED, 70L),
                warrant("w6", "south", WarrantStatus.ACTIVE, 10L),
                earlier);

        assertEquals(List.of(earlier, later), WarrantRegister.active(all, "north"));
    }

    @Test
    void anEmptyRegisterHasOnePage() {
        assertTrue(WarrantRegister.active(List.of(), "north").isEmpty());
        assertEquals(1, WarrantRegister.pageCount(0));
        assertTrue(WarrantRegister.page(List.<Warrant>of(), 0).isEmpty());
    }

    @Test
    void theRegisterPagesByPageSize() {
        List<Warrant> active = new ArrayList<>();
        for (int i = 0; i < WarrantRegister.PAGE_SIZE + 3; i++) {
            active.add(warrant("w" + i, "north", WarrantStatus.ACTIVE, i));
        }

        assertEquals(2, WarrantRegister.pageCount(active.size()));
        assertEquals(WarrantRegister.PAGE_SIZE, WarrantRegister.page(active, 0).size());
        assertEquals(3, WarrantRegister.page(active, 1).size());
        assertEquals(active.get(WarrantRegister.PAGE_SIZE), WarrantRegister.page(active, 1).get(0));
    }

    @Test
    void aPageBeyondTheEndShowsTheLastPage() {
        List<Warrant> active = List.of(warrant("w1", "north", WarrantStatus.ACTIVE, 1L));

        assertEquals(active, WarrantRegister.page(active, 5));
        assertEquals(active, WarrantRegister.page(active, -1));
    }
}
