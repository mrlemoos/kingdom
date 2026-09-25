package dev.mrlemoos.kingdom.parliament;

import java.util.List;

/**
 * One entry in <b>Hansard</b>: a single piece of business the House decided, as it stood when the
 * result was declared. Bills and motions record the benches that divided; a referendum records the
 * realm's answer with no benches at all, its electorate being the membership rather than the eight
 * seats.
 *
 * @param business      the business decided, lower case: a bill or motion type, or {@code referendum}
 * @param carried       whether the House decided in favour
 * @param electorate    how many were entitled to decide, against which turnout is read
 * @param blocs         how the benches divided, empty where the business was not a division
 * @param decidedOnMcDay the in-game day the result was declared
 * @param outcome       blank for a division; otherwise what befell business the House did not divide
 *                      on — a decree proclaimed, an election's returns, a treaty coming into force
 */
public record HansardRecord(
        String title,
        String business,
        boolean carried,
        int aye,
        int nay,
        int abstain,
        int electorate,
        List<DivisionBloc> blocs,
        long decidedOnMcDay,
        String outcome) {

    public HansardRecord {
        title = title == null ? "" : title;
        business = business == null ? "" : business;
        blocs = blocs == null ? List.of() : List.copyOf(blocs);
        outcome = outcome == null ? "" : outcome;
    }

    public HansardRecord(
            String title, String business, boolean carried, int aye, int nay, int abstain, int electorate,
            List<DivisionBloc> blocs, long decidedOnMcDay) {
        this(title, business, carried, aye, nay, abstain, electorate, blocs, decidedOnMcDay, "");
    }

    /** Business entered in the record without a division. */
    public static HansardRecord notice(String title, String business, String outcome, long decidedOnMcDay) {
        return new HansardRecord(title, business, true, 0, 0, 0, 0, List.of(), decidedOnMcDay, outcome);
    }

    public boolean isNotice() {
        return !outcome.isBlank();
    }

    /** Votes recorded either way. */
    public int votesCast() {
        return aye + nay + abstain;
    }

    /** The share of those entitled who recorded a vote; zero where no electorate is known. */
    public double turnout() {
        return electorate <= 0 ? 0d : (double) votesCast() / electorate;
    }
}
