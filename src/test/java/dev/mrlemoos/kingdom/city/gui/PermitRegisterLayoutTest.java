package dev.mrlemoos.kingdom.city.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermitRegisterLayoutTest {

    private static List<Integer> holders(int count) {
        List<Integer> holders = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            holders.add(index);
        }
        return holders;
    }

    @Test
    void anEmptyRegisterStillHasOnePage() {
        assertEquals(1, PermitRegisterLayout.pageCount(0));
    }

    @Test
    void pagesHoldFortyFiveHeads() {
        assertEquals(1, PermitRegisterLayout.pageCount(45));
        assertEquals(2, PermitRegisterLayout.pageCount(46));
        assertEquals(3, PermitRegisterLayout.pageCount(91));
    }

    @Test
    void pagesAreClampedIntoRange() {
        assertEquals(0, PermitRegisterLayout.clampPage(-4, 10));
        assertEquals(1, PermitRegisterLayout.clampPage(9, 50));
    }

    @Test
    void aPageSlicesItsOwnFortyFive() {
        List<Integer> all = holders(50);

        assertEquals(45, PermitRegisterLayout.pageSlice(all, 0).size());
        assertEquals(5, PermitRegisterLayout.pageSlice(all, 1).size());
        assertEquals(45, PermitRegisterLayout.pageSlice(all, 1).get(0));
        assertTrue(PermitRegisterLayout.pageSlice(all, 7).isEmpty());
    }

    @Test
    void navigationOnlyOffersPagesThatExist() {
        assertFalse(PermitRegisterLayout.hasPrevious(0));
        assertTrue(PermitRegisterLayout.hasPrevious(1));
        assertTrue(PermitRegisterLayout.hasNext(0, 50));
        assertFalse(PermitRegisterLayout.hasNext(1, 50));
    }

    @Test
    void headSlotsAreTheTopFiveRowsOnly() {
        assertTrue(PermitRegisterLayout.isHeadSlot(0));
        assertTrue(PermitRegisterLayout.isHeadSlot(44));
        assertFalse(PermitRegisterLayout.isHeadSlot(45));
        assertFalse(PermitRegisterLayout.isHeadSlot(-1));
    }

    @Test
    void grantAgeReadsInTheLargestUsefulUnit() {
        long now = 10_000_000_000L;
        assertEquals("moments ago", PermitRegisterLayout.grantedAgo(now - 5_000L, now));
        assertEquals("3m ago", PermitRegisterLayout.grantedAgo(now - 200_000L, now));
        assertEquals("2h 30m ago", PermitRegisterLayout.grantedAgo(now - 9_000_000L, now));
        assertEquals("1d 1h ago", PermitRegisterLayout.grantedAgo(now - 90_000_000L, now));
        assertEquals("moments ago", PermitRegisterLayout.grantedAgo(now + 5_000L, now));
    }
}
