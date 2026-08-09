package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.election.ProfessionConstituencyResolver;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;

/**
 * The Speaker's roll-call of the Commons: who was returned to each bench and by how many. Player
 * MPs are ranked by their votes, profession MPs by the size of their constituency, and the two are
 * never ranked against one another — a vote and a villager are not the same unit.
 */
public final class CommonsReturn {

    private CommonsReturn() {}

    /** The full roll-call, as the Speaker reads it at a State Opening. */
    public static List<String> rollCall(
            KingdomElectionState electionState, int totalSeats, Function<UUID, String> playerName) {
        List<MpSeat> players = new ArrayList<>();
        List<MpSeat> villagers = new ArrayList<>();
        for (MpSeat seat : electionState.seatsView().values()) {
            if (!seat.isOccupied()) {
                continue;
            }
            if (seat.kind() == MpSeatKind.PLAYER) {
                players.add(seat);
            } else {
                villagers.add(seat);
            }
        }
        players.sort(byReturnCountDescending());
        villagers.sort(byReturnCountDescending());

        List<String> lines = new ArrayList<>();
        lines.add("I report the return of the Commons.");
        for (MpSeat seat : players) {
            lines.add(returnOf(seat, playerName));
        }
        for (MpSeat seat : villagers) {
            lines.add(returnOf(seat, playerName));
        }
        int vacant = totalSeats - players.size() - villagers.size();
        if (vacant == 1) {
            lines.add("1 seat stands vacant.");
        } else if (vacant > 1) {
            lines.add(vacant + " seats stand vacant.");
        }
        lines.add("The Commons is duly returned.");
        return List.copyOf(lines);
    }

    /** A single seat's return, as the Speaker reads it after a by-election. */
    public static String seatReturn(MpSeat seat, Function<UUID, String> playerName) {
        if (!seat.isOccupied()) {
            return "Seat " + seat.index() + " — vacant.";
        }
        return "Seat " + seat.index() + " — " + returnOf(seat, playerName);
    }

    private static Comparator<MpSeat> byReturnCountDescending() {
        // ponytail: seats with no recorded return sort last, then by bench order.
        return Comparator.comparingInt((MpSeat seat) -> seat.returnCount().orElse(Integer.MIN_VALUE))
                .reversed()
                .thenComparingInt(MpSeat::index);
    }

    private static String returnOf(MpSeat seat, Function<UUID, String> playerName) {
        if (seat.kind() == MpSeatKind.PLAYER) {
            UUID holder = seat.playerId().orElse(null);
            String name = holder == null ? "Unknown" : playerName.apply(holder);
            OptionalInt votes = seat.returnCount();
            if (votes.isEmpty()) {
                return "MP " + name + ", returned unopposed.";
            }
            return "MP " + name + ", returned with " + votes.getAsInt()
                    + plural(votes.getAsInt(), " vote", " votes") + ".";
        }
        String profession = seat.profession().orElse(ProfessionConstituencyResolver.CITIZEN_PROFESSION);
        String label = ProfessionConstituencyResolver.displayLabel(profession);
        if (ProfessionConstituencyResolver.CITIZEN_PROFESSION.equals(profession) || seat.returnCount().isEmpty()) {
            // A Citizen backfills an empty bench rather than being returned by any constituency.
            return "MP " + label + ", returned unopposed for " + label + ".";
        }
        int villagers = seat.returnCount().getAsInt();
        return "MP " + label + ", returned for " + label + ", " + villagers
                + plural(villagers, " villager", " villagers") + ".";
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}
