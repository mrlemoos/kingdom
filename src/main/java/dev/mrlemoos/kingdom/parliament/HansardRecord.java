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
        long decidedOnMcDay) {

    public HansardRecord {
        title = title == null ? "" : title;
        business = business == null ? "" : business;
        blocs = blocs == null ? List.of() : List.copyOf(blocs);
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
