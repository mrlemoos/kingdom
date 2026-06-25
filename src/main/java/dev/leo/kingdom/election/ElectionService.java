package dev.leo.kingdom.election;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TitleStyle;
import dev.leo.kingdom.model.election.ElectionPhase;
import dev.leo.kingdom.model.election.ElectionState;
import dev.leo.kingdom.model.election.ElectionType;
import dev.leo.kingdom.model.election.KingdomElectionState;
import dev.leo.kingdom.model.election.MpSeat;
import dev.leo.kingdom.model.election.MpSeatKind;
import dev.leo.kingdom.model.parliament.BillState;
import dev.leo.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ElectionService {

    private final KingdomService kingdomService;
    private final ElectionConfig config;
    private final Supplier<Long> clockMs;

    public ElectionService(KingdomService kingdomService, ElectionConfig config) {
        this(kingdomService, config, System::currentTimeMillis);
    }

    ElectionService(KingdomService kingdomService, ElectionConfig config, Supplier<Long> clockMs) {
        this.kingdomService = kingdomService;
        this.config = config;
        this.clockMs = clockMs;
    }

    public ElectionResult startGeneralElection(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionResult.fail("Unknown kingdom.");
        }
        KingdomElectionState electionState = kingdom.get().getElectionState();
        if (electionState.election().isActive()) {
            return ElectionResult.fail("An election is already in progress.");
        }
        if (hasOpenDivision(kingdom.get())) {
            return ElectionResult.fail("Cannot call an election while a division is open.");
        }

        clearAllMpTitles(kingdomId);
        electionState.clearAllSeats();
        electionState.election().openGeneral(clockMs.get() + config.durationMs());
        return ElectionResult.ok("General election called. Nominations open for " + config.durationMcDays() + " in-game days.");
    }

    public ElectionResult startByElection(String kingdomId, int seatIndex) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionResult.fail("Unknown kingdom.");
        }
        if (seatIndex < 1 || seatIndex > config.totalSeats()) {
            return ElectionResult.fail("Seat index must be 1–" + config.totalSeats() + ".");
        }
        KingdomElectionState electionState = kingdom.get().getElectionState();
        if (electionState.election().isActive()) {
            return ElectionResult.fail("An election is already in progress.");
        }
        Optional<MpSeat> seat = electionState.seat(seatIndex);
        if (seat.isEmpty() || seat.get().isOccupied()) {
            return ElectionResult.fail("That seat is not vacant.");
        }

        long endsAt = clockMs.get() + config.durationMs();
        if (seatIndex <= config.maxPlayerSeats()) {
            electionState.election().openByElectionPlayer(seatIndex, endsAt);
        } else {
            electionState.election().openByElectionVillager(seatIndex, endsAt);
        }
        return ElectionResult.ok("By-election called for seat " + seatIndex + ".");
    }

    public ElectionResult nominate(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionResult.fail("Unknown kingdom.");
        }
        ElectionState election = kingdom.get().getElectionState().election();
        if (election.phase() != ElectionPhase.OPEN) {
            return ElectionResult.fail("Nominations are not open.");
        }
        Optional<ElectionType> type = election.type();
        if (type.isEmpty() || type.get() == ElectionType.BY_ELECTION_VILLAGER) {
            return ElectionResult.fail("Citizen nominations are not open for this election.");
        }

        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(kingdomId)) {
            return ElectionResult.fail("You are not a member of this kingdom.");
        }
        if (!isCitizen(membership.get())) {
            return ElectionResult.fail("Only citizens may stand for a player MP seat.");
        }

        election.nominate(playerId, clockMs.get());
        return ElectionResult.ok("You are nominated for a player MP seat.");
    }

    public ElectionResult castElectionVote(String kingdomId, UUID voterId, UUID candidateId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionResult.fail("Unknown kingdom.");
        }
        ElectionState election = kingdom.get().getElectionState().election();
        if (election.phase() != ElectionPhase.OPEN) {
            return ElectionResult.fail("Voting is not open.");
        }
        Optional<ElectionType> type = election.type();
        if (type.isEmpty() || type.get() == ElectionType.BY_ELECTION_VILLAGER) {
            return ElectionResult.fail("There is no citizen vote in this by-election.");
        }

        Optional<PlayerMembership> membership = kingdomService.getMembership(voterId);
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(kingdomId)) {
            return ElectionResult.fail("You are not a member of this kingdom.");
        }
        if (!canVoteInElection(membership.get())) {
            return ElectionResult.fail("You may not vote in this election.");
        }
        if (!election.nominationsView().contains(candidateId)) {
            return ElectionResult.fail("That candidate is not nominated.");
        }
        if (!election.castVote(voterId, candidateId)) {
            return ElectionResult.fail("Could not record your vote.");
        }
        return ElectionResult.ok("Vote recorded.");
    }

    public ElectionResult castSpeakerElectionVote(String kingdomId, UUID speakerId, UUID candidateId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionResult.fail("Unknown kingdom.");
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(speakerId);
        if (membership.isEmpty()
                || !membership.get().getKingdomId().equals(kingdomId)
                || membership.get().getRank() != NobleRank.SPEAKER) {
            return ElectionResult.fail("Only the Speaker may cast an election casting vote.");
        }

        ElectionState election = kingdom.get().getElectionState().election();
        if (election.phase() != ElectionPhase.AWAITING_SPEAKER_TIE) {
            return ElectionResult.fail("No election tie awaits a casting vote.");
        }
        if (!election.castSpeakerTieVote(candidateId)) {
            return ElectionResult.fail("That candidate is not tied for the last player MP seat.");
        }
        return ElectionResult.ok("Election casting vote recorded.");
    }

    public ElectionCloseOutcome tryCloseElection(String kingdomId, Map<String, Integer> professionCounts) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ElectionCloseOutcome.failed("Unknown kingdom.");
        }
        KingdomElectionState electionState = kingdom.get().getElectionState();
        ElectionState election = electionState.election();
        if (!election.isActive()) {
            return ElectionCloseOutcome.failed("No election is in progress.");
        }
        if (clockMs.get() < election.endsAtMs() && election.phase() == ElectionPhase.OPEN) {
            return ElectionCloseOutcome.failed("The election period has not ended.");
        }

        return switch (election.type().orElseThrow()) {
            case GENERAL -> closeGeneralElection(kingdom.get(), electionState, election, professionCounts);
            case BY_ELECTION_PLAYER -> closePlayerByElection(kingdom.get(), electionState, election);
            case BY_ELECTION_VILLAGER -> closeVillagerByElection(kingdom.get(), electionState, election, professionCounts);
        };
    }

    public List<Integer> vacantSeatIndices(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return List.of();
        }
        List<Integer> vacant = new ArrayList<>();
        for (MpSeat seat : kingdom.get().getElectionState().seatsView().values()) {
            if (!seat.isOccupied()) {
                vacant.add(seat.index());
            }
        }
        return vacant;
    }

    public boolean isMp(UUID playerId) {
        return kingdomService.getMembership(playerId)
                .map(m -> m.getRank() == NobleRank.MP)
                .orElse(false);
    }

    public static boolean canVoteInElection(PlayerMembership membership) {
        if (!membership.hasNobleTitle()) {
            return true;
        }
        NobleRank rank = membership.getRank();
        return rank != NobleRank.KING
                && rank != NobleRank.QUEEN
                && rank != NobleRank.PRINCE;
    }

    public static boolean isCitizen(PlayerMembership membership) {
        return !membership.hasNobleTitle();
    }

    private ElectionCloseOutcome closeGeneralElection(
            Kingdom kingdom,
            KingdomElectionState electionState,
            ElectionState election,
            Map<String, Integer> professionCounts) {
        PlayerWinnerResolution resolution = resolvePlayerWinners(election, config.maxPlayerSeats());
        if (resolution.needsSpeakerTieVote()) {
            return ElectionCloseOutcome.awaitingSpeakerTie();
        }

        List<UUID> playerWinners = resolution.winners();
        int villagerSlots = config.totalSeats() - playerWinners.size();
        List<String> professions =
                ProfessionConstituencyResolver.topProfessions(professionCounts, villagerSlots);

        clearAllMpTitles(kingdom.getId());
        electionState.clearAllSeats();

        assignPlayerSeats(electionState, playerWinners, 1);
        assignVillagerSeats(electionState, professions, playerWinners.size() + 1);

        election.close();
        return ElectionCloseOutcome.completed(playerWinners, professions);
    }

    private ElectionCloseOutcome closePlayerByElection(
            Kingdom kingdom, KingdomElectionState electionState, ElectionState election) {
        int seatIndex = election.byElectionSeatIndex().orElseThrow();
        PlayerWinnerResolution resolution = resolvePlayerWinners(election, 1);
        if (resolution.needsSpeakerTieVote()) {
            return ElectionCloseOutcome.awaitingSpeakerTie();
        }
        if (resolution.winners().isEmpty()) {
            election.close();
            return ElectionCloseOutcome.failed("No candidate received votes.");
        }

        UUID winner = resolution.winners().getFirst();
        kingdomService.getMembership(winner).ifPresent(m -> {
            if (m.getRank() == NobleRank.MP) {
                return;
            }
            if (m.hasNobleTitle()) {
                return;
            }
            kingdomService.assignTitleFromElection(winner, TitleStyle.MASCULINE);
        });

        MpSeat seat = electionState.seat(seatIndex).orElseThrow();
        seat.clear();
        seat.assignPlayer(winner);
        election.close();
        return ElectionCloseOutcome.completed(List.of(winner), List.of());
    }

    private ElectionCloseOutcome closeVillagerByElection(
            Kingdom kingdom,
            KingdomElectionState electionState,
            ElectionState election,
            Map<String, Integer> professionCounts) {
        int seatIndex = election.byElectionSeatIndex().orElseThrow();
        List<String> seatedProfessions = seatedProfessions(electionState);
        List<String> candidates =
                ProfessionConstituencyResolver.topProfessionsExcluding(professionCounts, 1, seatedProfessions);
        if (candidates.isEmpty()) {
            election.close();
            return ElectionCloseOutcome.failed("No productive villager profession available for this seat.");
        }

        MpSeat seat = electionState.seat(seatIndex).orElseThrow();
        seat.clear();
        seat.assignVillager(candidates.getFirst(), null);
        election.close();
        return ElectionCloseOutcome.completed(List.of(), List.of(candidates.getFirst()));
    }

    private PlayerWinnerResolution resolvePlayerWinners(ElectionState election, int maxWinners) {
        List<UUID> nominations = election.nominationsView();
        if (nominations.isEmpty()) {
            return PlayerWinnerResolution.resolved(List.of());
        }

        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID candidate : nominations) {
            voteCounts.put(candidate, 0);
        }
        for (UUID candidate : election.votesView().values()) {
            voteCounts.merge(candidate, 1, Integer::sum);
        }

        List<UUID> sorted = nominations.stream()
                .sorted(Comparator.comparingInt((UUID id) -> voteCounts.getOrDefault(id, 0))
                        .reversed()
                        .thenComparingLong(election::nominationOrderMs))
                .toList();

        int target = Math.min(maxWinners, sorted.size());
        int boundaryVotes = voteCounts.getOrDefault(sorted.get(target - 1), 0);

        List<UUID> sureWinners = new ArrayList<>();
        List<UUID> tiedAtBoundary = new ArrayList<>();
        for (UUID candidate : sorted) {
            int votes = voteCounts.getOrDefault(candidate, 0);
            if (votes > boundaryVotes) {
                sureWinners.add(candidate);
            } else if (votes == boundaryVotes) {
                tiedAtBoundary.add(candidate);
            }
        }

        int remainingSlots = target - sureWinners.size();
        if (tiedAtBoundary.size() <= remainingSlots) {
            List<UUID> winners = new ArrayList<>(sureWinners);
            winners.addAll(tiedAtBoundary.subList(0, remainingSlots));
            return PlayerWinnerResolution.resolved(winners);
        }

        if (election.phase() == ElectionPhase.OPEN) {
            election.awaitSpeakerTie(new LinkedHashSet<>(tiedAtBoundary));
            return PlayerWinnerResolution.awaitingSpeakerTie();
        }
        if (election.speakerTieChoice().isEmpty()) {
            return PlayerWinnerResolution.awaitingSpeakerTie();
        }

        UUID chosen = election.speakerTieChoice().get();
        List<UUID> winners = new ArrayList<>(sureWinners);
        winners.add(chosen);
        for (UUID candidate : tiedAtBoundary) {
            if (winners.size() >= target) {
                break;
            }
            if (!winners.contains(candidate)) {
                winners.add(candidate);
            }
        }
        return PlayerWinnerResolution.resolved(winners);
    }

    private void assignPlayerSeats(KingdomElectionState electionState, List<UUID> winners, int startIndex) {
        int index = startIndex;
        for (UUID winner : winners) {
            kingdomService.assignTitleFromElection(winner, TitleStyle.MASCULINE);
            MpSeat seat = electionState.seat(index).orElseThrow();
            seat.clear();
            seat.assignPlayer(winner);
            index++;
        }
    }

    private void assignVillagerSeats(KingdomElectionState electionState, List<String> professions, int startIndex) {
        int index = startIndex;
        for (String profession : professions) {
            MpSeat seat = electionState.seat(index).orElseThrow();
            seat.clear();
            seat.assignVillager(profession, null);
            index++;
        }
    }

    private List<String> seatedProfessions(KingdomElectionState electionState) {
        List<String> professions = new ArrayList<>();
        for (MpSeat seat : electionState.seatsView().values()) {
            if (seat.kind() == MpSeatKind.VILLAGER) {
                seat.profession().ifPresent(professions::add);
            }
        }
        return professions;
    }

    private void clearAllMpTitles(String kingdomId) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (kingdomId.equals(membership.getKingdomId()) && membership.getRank() == NobleRank.MP) {
                kingdomService.clearTitle(membership.getPlayerId());
            }
        }
    }

    private boolean hasOpenDivision(Kingdom kingdom) {
        return kingdom.getParliamentState()
                .currentBill()
                .map(bill -> bill.state() == BillState.DIVISION_OPEN)
                .orElse(false);
    }

    public record ElectionCloseOutcome(
            boolean complete,
            boolean needsSpeakerTieVote,
            String message,
            List<UUID> playerWinners,
            List<String> villagerProfessions) {

        static ElectionCloseOutcome completed(List<UUID> playerWinners, List<String> villagerProfessions) {
            return new ElectionCloseOutcome(true, false, "Election closed.", playerWinners, villagerProfessions);
        }

        static ElectionCloseOutcome awaitingSpeakerTie() {
            return new ElectionCloseOutcome(false, true, "Speaker must cast an election casting vote.", List.of(), List.of());
        }

        static ElectionCloseOutcome failed(String message) {
            return new ElectionCloseOutcome(false, false, message, List.of(), List.of());
        }
    }

    private record PlayerWinnerResolution(List<UUID> winners, boolean needsSpeakerTieVote) {
        static PlayerWinnerResolution resolved(List<UUID> winners) {
            return new PlayerWinnerResolution(winners, false);
        }

        static PlayerWinnerResolution awaitingSpeakerTie() {
            return new PlayerWinnerResolution(List.of(), true);
        }
    }
}
