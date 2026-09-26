package dev.mrlemoos.kingdom.poll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Slice 9.8 — what a poll card offers whom, and when it is owed. */
class PollCardRulesTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CITIZEN = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID DUKE = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000c4");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-0000000000c5");
    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000c6");

    private KingdomService kingdomService;
    private ElectionService electionService;
    private ParliamentService parliamentService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        for (UUID id : List.of(CITIZEN, OTHER_CITIZEN, DUKE, KING, SPEAKER, PREMIER)) {
            kingdomService.joinKingdom(id, KINGDOM);
        }
        kingdomService.assignTitle(DUKE, NobleRank.DUKE, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        parliamentService = new ParliamentService(kingdomService);
    }

    private Kingdom kingdom() {
        return kingdomService.getKingdom(KINGDOM).orElseThrow();
    }

    private PlayerMembership member(UUID id) {
        return kingdomService.getMembership(id).orElseThrow();
    }

    private PollCardRules.OpenPoll onlyPoll() {
        List<PollCardRules.OpenPoll> polls = PollCardRules.openPolls(kingdom());
        assertEquals(1, polls.size());
        return polls.get(0);
    }

    @Test
    void noPollsAreOpenWhileTheRealmIsQuiet() {
        assertTrue(PollCardRules.openPolls(kingdom()).isEmpty());
    }

    @Test
    void aCitizenMayStandAndVoteInAGeneralElection() {
        electionService.startGeneralElection(KINGDOM);
        PollCardRules.OpenPoll poll = onlyPoll();

        PollCardOffer offer = PollCardRules.offer(kingdom(), member(CITIZEN), poll.pollId());

        assertEquals(PollCardRules.PollKind.ELECTION, offer.kind());
        assertTrue(offer.mayStand());
        assertTrue(offer.mayVote());
        assertFalse(offer.standing());
        assertTrue(PollCardRules.owed(kingdom(), member(CITIZEN), poll));
    }

    @Test
    void aTitledNobleMayVoteButNotStand() {
        electionService.startGeneralElection(KINGDOM);

        PollCardOffer offer = PollCardRules.offer(kingdom(), member(DUKE), onlyPoll().pollId());

        assertFalse(offer.mayStand());
        assertTrue(offer.mayVote());
    }

    @Test
    void theCrownIsOwedNoCardInAGeneralElection() {
        electionService.startGeneralElection(KINGDOM);
        PollCardRules.OpenPoll poll = onlyPoll();

        PollCardOffer offer = PollCardRules.offer(kingdom(), member(KING), poll.pollId());

        assertFalse(offer.hasBusiness());
        assertFalse(PollCardRules.owed(kingdom(), member(KING), poll));
    }

    @Test
    void theBallotListsTheNominatedCandidatesAndAStandingCandidateCannotStandAgain() {
        electionService.startGeneralElection(KINGDOM);
        electionService.nominate(KINGDOM, CITIZEN);

        PollCardOffer offer = PollCardRules.offer(kingdom(), member(CITIZEN), onlyPoll().pollId());

        assertTrue(offer.standing());
        assertFalse(offer.mayStand());
        assertEquals(List.of(CITIZEN), offer.candidates());
    }

    @Test
    void aVoterIsOwedNoFurtherCardOnceTheyHaveVoted() {
        electionService.startGeneralElection(KINGDOM);
        electionService.nominate(KINGDOM, OTHER_CITIZEN);
        PollCardRules.OpenPoll poll = onlyPoll();

        assertInstanceOf(
                ElectionResult.Success.class, electionService.castElectionVote(KINGDOM, CITIZEN, OTHER_CITIZEN));

        assertTrue(PollCardRules.offer(kingdom(), member(CITIZEN), poll.pollId()).voted());
        assertFalse(PollCardRules.owed(kingdom(), member(CITIZEN), poll));
        assertTrue(PollCardRules.owed(kingdom(), member(DUKE), poll));
    }

    @Test
    void onlySeatedPlayerMpsAreOfferedTheBallotInAPremierElection() {
        kingdom().getElectionState().seat(1).orElseThrow().assignPlayer(PREMIER);
        kingdomService.assignTitleFromElection(PREMIER, TitleStyle.MASCULINE);
        assertInstanceOf(ElectionResult.Success.class, electionService.startPremierElection(KINGDOM));
        PollCardRules.OpenPoll poll = onlyPoll();

        PollCardOffer mp = PollCardRules.offer(kingdom(), member(PREMIER), poll.pollId());
        PollCardOffer citizen = PollCardRules.offer(kingdom(), member(CITIZEN), poll.pollId());

        assertTrue(mp.mayStand());
        assertTrue(mp.mayVote());
        assertFalse(citizen.hasBusiness());
    }

    @Test
    void aTiedCountOffersTheSpeakerTheCastingVoteAndNobodyElse() {
        electionService.startGeneralElection(KINGDOM);
        electionService.nominate(KINGDOM, CITIZEN);
        electionService.nominate(KINGDOM, OTHER_CITIZEN);
        String openId = onlyPoll().pollId();
        kingdom().getElectionState().election().awaitSpeakerTie(Set.of(CITIZEN, OTHER_CITIZEN));

        PollCardRules.OpenPoll tie = onlyPoll();
        PollCardOffer speaker = PollCardRules.offer(kingdom(), member(SPEAKER), tie.pollId());
        PollCardOffer citizen = PollCardRules.offer(kingdom(), member(DUKE), tie.pollId());

        assertEquals(PollCardRules.PollKind.SPEAKER_TIE, tie.kind());
        assertTrue(speaker.mayVote());
        assertFalse(speaker.mayStand());
        assertEquals(Set.of(CITIZEN, OTHER_CITIZEN), Set.copyOf(speaker.candidates()));
        assertFalse(citizen.hasBusiness());
        assertTrue(PollCardRules.offer(kingdom(), member(DUKE), openId).stale());
    }

    @Test
    void aCardForAClosedPollIsStale() {
        electionService.startGeneralElection(KINGDOM);
        String pollId = onlyPoll().pollId();
        kingdom().getElectionState().election().close();

        assertTrue(PollCardRules.offer(kingdom(), member(CITIZEN), pollId).stale());
        assertFalse(PollCardRules.offer(kingdom(), member(CITIZEN), pollId).hasBusiness());
    }

    @Test
    void everyMemberIsOwedAReferendumCardUntilTheyCastABallot() {
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, "Keep the curfew?"));
        PollCardRules.OpenPoll poll = onlyPoll();

        assertEquals(PollCardRules.PollKind.REFERENDUM, poll.kind());
        assertTrue(PollCardRules.owed(kingdom(), member(KING), poll));

        parliamentService.castBallot(KINGDOM, KING, VoteChoice.AYE);

        assertFalse(PollCardRules.owed(kingdom(), member(KING), poll));
        assertTrue(PollCardRules.owed(kingdom(), member(CITIZEN), poll));
    }
}
