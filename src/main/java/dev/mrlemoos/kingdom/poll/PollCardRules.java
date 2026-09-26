package dev.mrlemoos.kingdom.poll;

import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionState;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Which polls are open in a realm, and what a <b>poll card</b> offers each member for them. A poll is
 * named by an id that changes whenever the question changes — a new election, the Speaker's casting
 * vote after a tied count, a new referendum — so a card for a poll that has moved on reads as stale.
 */
public final class PollCardRules {

    public enum PollKind {
        /** Nominations and the ballot, open together. */
        ELECTION,
        /** A tied count awaiting the Speaker's casting vote. */
        SPEAKER_TIE,
        /** A question put to the whole realm. */
        REFERENDUM
    }

    public record OpenPoll(String pollId, PollKind kind, String title) {}

    private PollCardRules() {}

    /** Every poll open in the realm that a member might be handed a card for. */
    public static List<OpenPoll> openPolls(Kingdom kingdom) {
        List<OpenPoll> polls = new ArrayList<>();
        ElectionState election = kingdom.getElectionState().election();
        Optional<ElectionType> type = election.type();
        if (election.isActive() && type.isPresent() && type.get() != ElectionType.BY_ELECTION_VILLAGER) {
            boolean tie = election.phase() == ElectionPhase.AWAITING_SPEAKER_TIE;
            String id = "election:" + type.get().name() + ":" + election.endsAtMs() + (tie ? ":tie" : "");
            polls.add(new OpenPoll(id, tie ? PollKind.SPEAKER_TIE : PollKind.ELECTION, electionTitle(election)));
        }
        Optional<Bill> referendum = openReferendum(kingdom);
        if (referendum.isPresent()) {
            polls.add(new OpenPoll(
                    "referendum:" + referendum.get().id(), PollKind.REFERENDUM, referendumTitle(referendum.get())));
        }
        return polls;
    }

    public static Optional<OpenPoll> openPoll(Kingdom kingdom, String pollId) {
        for (OpenPoll poll : openPolls(kingdom)) {
            if (poll.pollId().equals(pollId)) {
                return Optional.of(poll);
            }
        }
        return Optional.empty();
    }

    /** What the card for {@code pollId} offers this member now. */
    public static PollCardOffer offer(Kingdom kingdom, PlayerMembership membership, String pollId) {
        Optional<OpenPoll> poll = openPoll(kingdom, pollId);
        if (poll.isEmpty() || !kingdom.getId().equals(membership.getKingdomId())) {
            return PollCardOffer.closed();
        }
        UUID playerId = membership.getPlayerId();
        return switch (poll.get().kind()) {
            case REFERENDUM -> {
                Optional<Bill> referendum = openReferendum(kingdom);
                boolean voted = referendum.isPresent() && referendum.get().votesView().containsKey(playerId);
                yield new PollCardOffer(PollKind.REFERENDUM, false, false, true, List.of(), voted);
            }
            case SPEAKER_TIE -> {
                ElectionState election = kingdom.getElectionState().election();
                boolean speaker = membership.getRank() == NobleRank.SPEAKER;
                yield new PollCardOffer(
                        PollKind.SPEAKER_TIE,
                        false,
                        false,
                        speaker,
                        speaker ? orderedTie(election) : List.of(),
                        election.speakerTieChoice().isPresent());
            }
            case ELECTION -> electionOffer(kingdom, membership);
        };
    }

    /** Whether a member should be handed a card for this poll: it asks something of them and they have not voted. */
    public static boolean owed(Kingdom kingdom, PlayerMembership membership, OpenPoll poll) {
        PollCardOffer offer = offer(kingdom, membership, poll.pollId());
        return offer.hasBusiness() && !offer.voted();
    }

    private static PollCardOffer electionOffer(Kingdom kingdom, PlayerMembership membership) {
        ElectionState election = kingdom.getElectionState().election();
        UUID playerId = membership.getPlayerId();
        boolean premier = election.type().filter(type -> type == ElectionType.PREMIER).isPresent();
        boolean eligibleToStand;
        boolean eligibleToVote;
        if (premier) {
            boolean seated = ElectionService.isSeatedPlayerMp(kingdom.getElectionState(), playerId);
            eligibleToStand = seated;
            eligibleToVote = seated;
        } else {
            eligibleToStand = ElectionService.isCitizen(membership);
            eligibleToVote = ElectionService.canVoteInElection(membership);
        }
        boolean standing = election.nominationsView().contains(playerId);
        return new PollCardOffer(
                PollKind.ELECTION,
                eligibleToStand && !standing,
                standing,
                eligibleToVote,
                eligibleToVote ? election.nominationsView() : List.of(),
                election.votesView().containsKey(playerId));
    }

    private static List<UUID> orderedTie(ElectionState election) {
        List<UUID> tied = new ArrayList<>();
        for (UUID candidate : election.nominationsView()) {
            if (election.speakerTieCandidatesView().contains(candidate)) {
                tied.add(candidate);
            }
        }
        for (UUID candidate : election.speakerTieCandidatesView()) {
            if (!tied.contains(candidate)) {
                tied.add(candidate);
            }
        }
        return tied;
    }

    private static Optional<Bill> openReferendum(Kingdom kingdom) {
        Optional<Bill> bill = kingdom.getParliamentState().currentBill();
        if (bill.isPresent()
                && bill.get().type() == BillType.REFERENDUM
                && bill.get().state() == BillState.DIVISION_OPEN) {
            return bill;
        }
        return Optional.empty();
    }

    private static String electionTitle(ElectionState election) {
        Optional<ElectionType> type = election.type();
        if (type.isEmpty()) {
            return "Election";
        }
        return switch (type.get()) {
            case GENERAL -> "General election";
            case PREMIER -> "Premier election";
            case BY_ELECTION_PLAYER, BY_ELECTION_VILLAGER -> {
                Optional<Integer> seat = election.byElectionSeatIndex();
                yield seat.isPresent() ? "By-election for seat " + seat.get() : "By-election";
            }
        };
    }

    private static String referendumTitle(Bill referendum) {
        if (referendum.payload() instanceof BillPayload.Referendum payload) {
            return "Referendum: " + payload.question();
        }
        return "Referendum";
    }
}
