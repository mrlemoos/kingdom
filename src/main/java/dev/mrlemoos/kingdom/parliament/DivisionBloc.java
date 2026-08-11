package dev.mrlemoos.kingdom.parliament;

/**
 * One grouping in a division tally: a <b>party</b> of player MPs, the independents who declared
 * none, or a <b>profession bloc</b> of villager MPs.
 */
public record DivisionBloc(DivisionBlocKind kind, String label, String colour, int aye, int nay, int abstain) {

    /** Members of this bloc who recorded a vote either way. */
    public int voted() {
        return aye + nay + abstain;
    }
}
