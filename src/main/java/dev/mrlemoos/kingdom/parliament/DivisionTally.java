package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.election.ProfessionConstituencyResolver;
import dev.mrlemoos.kingdom.election.StableSeatUuid;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * How the House divided, read bench by bench: player MPs are grouped by the <b>party</b> they
 * declared on nomination, and villager MPs by the <b>profession bloc</b> that returned them. A
 * player who declared no party stands with the independents; a villager never stands under a party.
 */
public final class DivisionTally {

    /** The colour the independents are read out in. */
    public static final String INDEPENDENT_COLOUR = "&7";

    /** The colour a profession bloc is read out in. */
    public static final String PROFESSION_BLOC_COLOUR = "&2";

    private DivisionTally() {}

    /**
     * Groups the division's votes by bloc. Votes cast by anyone who holds no seat are disregarded —
     * only the eight benches divide.
     */
    public static List<DivisionBloc> tally(
            String kingdomId, Map<UUID, VoteChoice> votes, Collection<MpSeat> seats) {
        Map<String, Accumulator> blocs = new LinkedHashMap<>();
        for (MpSeat seat : seats) {
            if (!seat.isOccupied()) {
                continue;
            }
            Accumulator bloc = blocFor(kingdomId, seat, blocs);
            if (bloc == null) {
                continue;
            }
            VoteChoice choice = votes.get(voterIdFor(kingdomId, seat));
            bloc.record(choice);
        }
        List<DivisionBloc> tallied = new ArrayList<>();
        for (Accumulator accumulator : blocs.values()) {
            tallied.add(accumulator.toBloc());
        }
        tallied.sort(byKindThenStrength());
        return List.copyOf(tallied);
    }

    /** One line per bloc, as the House hears the result read out. */
    public static List<String> renderLines(List<DivisionBloc> blocs) {
        List<String> lines = new ArrayList<>();
        for (DivisionBloc bloc : blocs) {
            lines.add(line(bloc));
        }
        return List.copyOf(lines);
    }

    /** A single bloc's line: its name in its own colour, then how it divided. */
    public static String line(DivisionBloc bloc) {
        return bloc.colour() + bloc.label() + " &7— &a" + bloc.aye() + " aye&7, &c" + bloc.nay()
                + " nay&7, &e" + bloc.abstain() + " abstain";
    }

    private static UUID voterIdFor(String kingdomId, MpSeat seat) {
        if (seat.kind() == MpSeatKind.PLAYER) {
            return seat.playerId().orElse(null);
        }
        return StableSeatUuid.forSeat(kingdomId, seat.index());
    }

    private static Accumulator blocFor(String kingdomId, MpSeat seat, Map<String, Accumulator> blocs) {
        if (seat.kind() == MpSeatKind.PLAYER) {
            if (seat.playerId().isEmpty()) {
                return null;
            }
            Optional<CandidateDeclaration> declaration = seat.declaration();
            boolean party = declaration.isPresent() && declaration.get().hasParty();
            String label = party ? declaration.get().partyName() : CandidateDeclaration.INDEPENDENT_LABEL;
            String colour = party ? declaration.get().partyColour() : INDEPENDENT_COLOUR;
            DivisionBlocKind kind = party ? DivisionBlocKind.PARTY : DivisionBlocKind.INDEPENDENT;
            return blocs.computeIfAbsent(kind + "|" + label, key -> new Accumulator(kind, label, colour));
        }
        String profession = seat.profession().orElse(ProfessionConstituencyResolver.CITIZEN_PROFESSION);
        String label = ProfessionConstituencyResolver.displayLabel(profession);
        return blocs.computeIfAbsent(
                DivisionBlocKind.PROFESSION + "|" + label,
                key -> new Accumulator(DivisionBlocKind.PROFESSION, label, PROFESSION_BLOC_COLOUR));
    }

    private static Comparator<DivisionBloc> byKindThenStrength() {
        // Parties first, then the independents, then the profession benches.
        return Comparator.comparingInt((DivisionBloc bloc) -> bloc.kind().ordinal())
                .thenComparing(Comparator.comparingInt(DivisionBloc::voted).reversed())
                .thenComparing(DivisionBloc::label);
    }

    /** Counts one bloc's votes as the seats are walked. */
    private static final class Accumulator {

        private final DivisionBlocKind kind;
        private final String label;
        private final String colour;
        private int aye;
        private int nay;
        private int abstain;

        private Accumulator(DivisionBlocKind kind, String label, String colour) {
            this.kind = kind;
            this.label = label;
            this.colour = colour;
        }

        private void record(VoteChoice choice) {
            if (choice == VoteChoice.AYE) {
                aye++;
            } else if (choice == VoteChoice.NAY) {
                nay++;
            } else if (choice == VoteChoice.ABSTAIN) {
                abstain++;
            }
        }

        private DivisionBloc toBloc() {
            return new DivisionBloc(kind, label, colour, aye, nay, abstain);
        }
    }
}
