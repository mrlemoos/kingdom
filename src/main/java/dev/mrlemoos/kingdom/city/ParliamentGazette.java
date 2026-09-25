package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import java.util.Optional;

/**
 * Parliament's news for the Gazette, so the town crier cries it: the House opened, an election or
 * by-election called, and the fate of a fiscal bill. Authored by the realm rather than by any hand,
 * and so hung directly rather than through {@link GazetteService}, which admits the Crown alone. A
 * kingdom with no capital has no crier and no Gazette to hang it on.
 */
public final class ParliamentGazette {

    private ParliamentGazette() {}

    public static void parliamentOpened(Kingdom kingdom, long mcDay) {
        hang(kingdom, "Parliament is opened",
                "The Crown has opened Parliament. The House sits, and bills may be tabled.", mcDay);
    }

    public static void generalElectionCalled(Kingdom kingdom, int durationMcDays, long mcDay) {
        hang(kingdom, "A general election is called",
                "Parliament is dissolved. Nominations are open for " + durationMcDays + " in-game days.", mcDay);
    }

    public static void byElectionCalled(Kingdom kingdom, int seatIndex, int durationMcDays, long mcDay) {
        hang(kingdom, "A by-election is called",
                "Seat " + seatIndex + " stands vacant. Nominations are open for " + durationMcDays
                        + " in-game days.", mcDay);
    }

    public static void fiscalBillPassed(Kingdom kingdom, String billTitle, long mcDay) {
        hang(kingdom, "The fiscal bill passes",
                billTitle + " has received royal assent; the realm's new rates are law.", mcDay);
    }

    public static void fiscalBillLostInCommons(Kingdom kingdom, String billTitle, long mcDay) {
        hang(kingdom, "The fiscal bill fails",
                billTitle + " was lost in the Commons; the realm's rates stand as they were.", mcDay);
    }

    public static void fiscalBillRefusedAssent(Kingdom kingdom, String billTitle, long mcDay) {
        hang(kingdom, "The fiscal bill fails",
                "The Crown has withheld royal assent from " + billTitle
                        + "; the realm's rates stand as they were.", mcDay);
    }

    private static void hang(Kingdom kingdom, String title, String body, long mcDay) {
        if (!kingdom.getCityState().hasCapital()) {
            return;
        }
        kingdom.getCityState().addGazettePost(new GazettePost(
                title, body, SeasonGazette.REALM_AUTHOR, mcDay, GazettePostKind.ANNOUNCEMENT, Optional.empty()));
    }
}
