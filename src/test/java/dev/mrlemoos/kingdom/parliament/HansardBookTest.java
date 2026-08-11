package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HansardBookTest {

    private static HansardRecord division(int number) {
        return new HansardRecord(
                "Finance Bill " + number,
                "fiscal",
                number % 2 == 0,
                3,
                1,
                0,
                8,
                List.of(
                        new DivisionBloc(DivisionBlocKind.PARTY, "Northern Union", "&9", 2, 0, 0),
                        new DivisionBloc(DivisionBlocKind.PROFESSION, "Farmer", "&2", 1, 1, 0)),
                100L + number);
    }

    @Test
    void emptySessionRendersNoVolume() {
        assertTrue(HansardBook.render("Northmarch", List.of()).isEmpty());
    }

    @Test
    void sixtyDivisionSessionRendersWithoutTruncation() {
        List<HansardRecord> records = new ArrayList<>();
        for (int number = 1; number <= 60; number++) {
            records.add(division(number));
        }

        List<HansardVolume> volumes = HansardBook.render("Northmarch", records);

        assertFalse(volumes.isEmpty());
        StringBuilder all = new StringBuilder();
        for (HansardVolume volume : volumes) {
            assertTrue(volume.pages().size() <= HansardBook.MAX_PAGES_PER_VOLUME,
                    "volume exceeded the written-book page limit");
            for (String page : volume.pages()) {
                assertTrue(page.length() <= HansardBook.MAX_PAGE_CHARS,
                        "page exceeded the written-book character limit: " + page.length());
                all.append(page).append('\n');
            }
        }
        String rendered = all.toString();
        for (HansardRecord record : records) {
            assertTrue(rendered.contains(record.title()), "record was truncated away: " + record.title());
            assertTrue(rendered.contains("Day " + record.decidedOnMcDay()));
        }
        assertTrue(rendered.contains("Northern Union"));
        assertTrue(rendered.contains("Farmer"));
    }

    @Test
    void longSessionSpillsIntoFurtherVolumesNumberedInSequence() {
        List<HansardRecord> records = new ArrayList<>();
        for (int number = 1; number <= 400; number++) {
            records.add(division(number));
        }

        List<HansardVolume> volumes = HansardBook.render("Northmarch", records);

        assertTrue(volumes.size() > 1, "a long session should spill into further volumes");
        for (int index = 0; index < volumes.size(); index++) {
            assertEquals(index + 1, volumes.get(index).number());
        }
        assertTrue(volumes.get(0).title().contains("I"));
        assertTrue(volumes.get(1).title().contains("II"));
    }

    @Test
    void bookCarriesNoColourCodes() {
        List<HansardVolume> volumes = HansardBook.render("Northmarch", List.of(division(1)));

        for (String page : volumes.get(0).pages()) {
            assertFalse(page.contains("&"), "colour codes must not reach the page: " + page);
        }
    }

    @Test
    void overlongLineIsSplitRatherThanTruncated() {
        String longTitle = "A".repeat(700);
        HansardRecord record = new HansardRecord(
                longTitle, "budget", true, 1, 0, 0, 8, List.of(), 5L);

        List<HansardVolume> volumes = HansardBook.render("Northmarch", List.of(record));

        StringBuilder all = new StringBuilder();
        for (String page : volumes.get(0).pages()) {
            assertTrue(page.length() <= HansardBook.MAX_PAGE_CHARS);
            all.append(page);
        }
        assertTrue(all.toString().replace("\n", "").contains(longTitle));
    }

    @Test
    void referendumResultRecordsTurnout() {
        HansardRecord referendum = new HansardRecord(
                "Should the realm join the war?", "referendum", true, 6, 2, 1, 20, List.of(), 42L);

        List<HansardVolume> volumes = HansardBook.render("Northmarch", List.of(referendum));

        String rendered = String.join("\n", volumes.get(0).pages());
        assertTrue(rendered.contains("Turnout"));
        assertEquals(0.45, referendum.turnout(), 1e-9);
    }
}
