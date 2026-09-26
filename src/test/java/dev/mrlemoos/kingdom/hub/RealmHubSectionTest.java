package dev.mrlemoos.kingdom.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.city.gui.GazetteLiveState;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RealmHubSectionTest {

    private static RealmHubSnapshot.Builder crown() {
        return RealmHubSnapshot.builder()
                .member(true)
                .kingdomName("Northmarch")
                .rank(NobleRank.QUEEN)
                .titleLabel(NobleRank.QUEEN.displayTitle(TitleStyle.FEMININE))
                .warEnabled(true)
                .conscriptionEnabled(true)
                .live(new GazetteLiveState("", "none proclaimed", 0, 0, 0d));
    }

    private static RealmHubSnapshot everythingLive() {
        return crown()
                .electionOpen(true)
                .pollingOpen(true)
                .divisionAwaitingVote(true)
                .wanted(true)
                .resignationToReview(true)
                .musterStatus("Muster open")
                .siegeStatus("Defender territory: Southreach 2")
                .live(new GazetteLiveState("Budget Bill", "realm day 40", 1, 0, 12d, "Treaty active"))
                .build();
    }

    @Test
    void everyTopicMapsToOneSectionOrTheFrontPage() {
        for (RealmHubTopic topic : RealmHubTopic.values()) {
            Optional<RealmHubSection> section = RealmHubSection.of(topic);
            boolean front = topic == RealmHubTopic.STANDING || topic.name().startsWith("LIVE_");
            assertEquals(front, RealmHubSection.onFrontPage(topic), topic.name());
            assertEquals(front, section.isEmpty(), topic + " must be on the front page or in one section");
        }
    }

    @Test
    void everySectionHoldsSomething() {
        Set<RealmHubSection> used = new HashSet<>();
        for (RealmHubTopic topic : RealmHubTopic.values()) {
            Optional<RealmHubSection> section = RealmHubSection.of(topic);
            if (section.isPresent()) {
                used.add(section.get());
            }
        }
        assertEquals(Set.of(RealmHubSection.values()), used);
    }

    @Test
    void liveBusinessLandsOnTheFrontPageAndInNoSection() {
        RealmHubSnapshot snapshot = everythingLive();

        List<RealmHubEntry> front = RealmHubView.frontPage(snapshot);

        assertEquals(RealmHubTopic.STANDING, front.get(0).topic());
        long live = front.stream().filter(e -> e.topic().name().startsWith("LIVE_")).count();
        assertEquals(8, live);
        assertEquals(front.size(), live + 1);
        for (RealmHubSection section : RealmHubSection.values()) {
            for (RealmHubEntry entry : RealmHubView.section(snapshot, section)) {
                assertFalse(entry.topic().name().startsWith("LIVE_"), section + " holds " + entry.topic());
                assertEquals(Optional.of(section), RealmHubSection.of(entry.topic()));
            }
        }
    }

    @Test
    void theFrontPageAndSectionsTogetherHoldEveryEntry() {
        RealmHubSnapshot snapshot = everythingLive();
        List<RealmHubTopic> split = new ArrayList<>();
        for (RealmHubEntry entry : RealmHubView.frontPage(snapshot)) {
            split.add(entry.topic());
        }
        for (RealmHubSection section : RealmHubSection.values()) {
            for (RealmHubEntry entry : RealmHubView.section(snapshot, section)) {
                split.add(entry.topic());
            }
        }
        List<RealmHubTopic> all = new ArrayList<>();
        for (RealmHubEntry entry : RealmHubView.entries(snapshot)) {
            all.add(entry.topic());
        }
        assertEquals(all.size(), split.size());
        assertEquals(new HashSet<>(all), new HashSet<>(split));
    }

    @Test
    void aSectionLaysOutPlacesThenPowersThenExperiences() {
        List<RealmHubEntry> city = RealmHubView.section(crown().build(), RealmHubSection.CITY);

        int last = -1;
        for (RealmHubEntry entry : city) {
            int row = RealmHubSection.row(entry.topic()).ordinal();
            assertTrue(row >= last, "out of order at " + entry.topic());
            last = row;
        }
    }

    @Test
    void eachRowKindStartsOnItsOwnRow() {
        List<RealmHubEntry> city = RealmHubView.section(crown().build(), RealmHubSection.CITY);

        List<Map<Integer, RealmHubEntry>> pages = RealmHubLayout.sectionPages(city);

        assertEquals(1, pages.size());
        Map<RealmHubSection.Row, Set<Integer>> rowsByKind = new java.util.EnumMap<>(RealmHubSection.Row.class);
        for (Map.Entry<Integer, RealmHubEntry> placed : pages.get(0).entrySet()) {
            rowsByKind
                    .computeIfAbsent(RealmHubSection.row(placed.getValue().topic()), k -> new HashSet<>())
                    .add(placed.getKey() / RealmHubLayout.ROW_WIDTH);
        }
        for (RealmHubSection.Row a : rowsByKind.keySet()) {
            for (RealmHubSection.Row b : rowsByKind.keySet()) {
                if (a != b) {
                    Set<Integer> shared = new HashSet<>(rowsByKind.get(a));
                    shared.retainAll(rowsByKind.get(b));
                    assertTrue(shared.isEmpty(), a + " and " + b + " share a row");
                }
            }
        }
    }

    @Test
    void noSectionOverflowsItsRowsWithoutPaging() {
        RealmHubSnapshot snapshot = everythingLive();
        for (RealmHubSection section : RealmHubSection.values()) {
            List<RealmHubEntry> entries = RealmHubView.section(snapshot, section);
            assertEntriesFitTheirPages(entries);
        }
    }

    @Test
    void anOverfullSectionSpillsOntoASecondPage() {
        List<RealmHubEntry> entries = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            entries.add(RealmHubEntry.usable(RealmHubTopic.PLACE_PRISON, "Place " + i, List.of(), null));
        }
        for (int i = 0; i < 20; i++) {
            entries.add(RealmHubEntry.usable(RealmHubTopic.POWER_SITES, "Power " + i, List.of(), null));
        }
        for (int i = 0; i < 10; i++) {
            entries.add(RealmHubEntry.usable(RealmHubTopic.GAZETTE, "Experience " + i, List.of(), null));
        }

        List<Map<Integer, RealmHubEntry>> pages = RealmHubLayout.sectionPages(entries);

        assertTrue(pages.size() > 1);
        assertEntriesFitTheirPages(entries);
    }

    @Test
    void anEmptySectionStillHasOnePage() {
        assertEquals(1, RealmHubLayout.sectionPages(List.of()).size());
    }

    @Test
    void theFrontPageHasADoorPerSectionAndRoomForAllLiveBusiness() {
        Set<Integer> doors = new HashSet<>();
        for (RealmHubSection section : RealmHubSection.values()) {
            int slot = RealmHubLayout.doorSlot(section);
            assertTrue(RealmHubLayout.isEntrySlot(slot), section + " door off the page");
            assertNotEquals(RealmHubLayout.SLOT_STANDING, slot);
            assertTrue(doors.add(slot), section + " shares a door");
        }
        long liveTopics = java.util.Arrays.stream(RealmHubTopic.values())
                .filter(t -> t.name().startsWith("LIVE_"))
                .count();
        assertTrue(liveTopics <= RealmHubLayout.LIVE_CAPACITY);
        for (int i = 0; i < RealmHubLayout.LIVE_CAPACITY; i++) {
            int slot = RealmHubLayout.liveSlot(i);
            assertTrue(RealmHubLayout.isEntrySlot(slot));
            assertFalse(doors.contains(slot), "live slot " + slot + " is a door");
            assertNotEquals(RealmHubLayout.SLOT_STANDING, slot);
        }
    }

    @Test
    void aPowerNamesWhoMayEvenWhenItIsYours() {
        RealmHubEntry gazette = RealmHubView.section(crown().build(), RealmHubSection.CITY).stream()
                .filter(e -> e.topic() == RealmHubTopic.POWER_GAZETTE)
                .findFirst()
                .orElseThrow();

        assertTrue(gazette.usable());
        assertEquals("A Duke or the Crown may publish.", gazette.who());
    }

    private static void assertEntriesFitTheirPages(List<RealmHubEntry> entries) {
        List<Map<Integer, RealmHubEntry>> pages = RealmHubLayout.sectionPages(entries);
        List<RealmHubEntry> placed = new ArrayList<>();
        for (Map<Integer, RealmHubEntry> page : pages) {
            for (Integer slot : new java.util.TreeSet<>(page.keySet())) {
                assertTrue(RealmHubLayout.isEntrySlot(slot), "slot " + slot + " spills into navigation");
                placed.add(page.get(slot));
            }
        }
        assertEquals(entries, placed);
    }
}
