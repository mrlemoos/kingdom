package dev.mrlemoos.kingdom.resignation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.election.ResignationSubjectKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResignationServiceTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MP_ONE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRINCE = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private KingdomService kingdomService;
    private ElectionService electionService;
    private ResignationService resignationService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        resignationService = new ResignationService(kingdomService, electionService, () -> 1_700_000_000_000L);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(PREMIER, "northmarch");
        kingdomService.joinKingdom(MP_ONE, "northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_ONE, TitleStyle.MASCULINE);

        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        electionState.seat(1).orElseThrow().assignPlayer(MP_ONE);
    }

    @Test
    void premierOffersResignationAndRemainsInOfficeUntilAccepted() {
        ResignationResult offered = resignationService.offerResignation("northmarch", PREMIER, OptionalInt.empty());

        assertInstanceOf(ResignationResult.Success.class, offered);
        assertEquals(NobleRank.PREMIER, kingdomService.getMembership(PREMIER).orElseThrow().getRank());
        assertTrue(resignationService.pendingResignation("northmarch").isPresent());
        assertEquals(ResignationSubjectKind.PLAYER_PREMIER, resignationService
                .pendingResignation("northmarch")
                .orElseThrow()
                .subject()
                .kind());
    }

    @Test
    void kingAcceptsPremierResignationAndOpensPremierElection() {
        resignationService.offerResignation("northmarch", PREMIER, OptionalInt.empty());

        ResignationResult accepted = resignationService.acceptResignation("northmarch", NobleRank.KING);

        assertInstanceOf(ResignationResult.Success.class, accepted);
        assertTrue(kingdomService.getMembership(PREMIER).orElseThrow().getRank() != NobleRank.PREMIER);
        assertTrue(resignationService.pendingResignation("northmarch").isEmpty());
        var election = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().election();
        assertTrue(election.isActive());
        assertEquals(ElectionType.PREMIER, election.type().orElseThrow());
    }

    @Test
    void kingRejectsResignationAndPremierRemainsInOffice() {
        resignationService.offerResignation("northmarch", PREMIER, OptionalInt.empty());

        ResignationResult rejected = resignationService.rejectResignation("northmarch", NobleRank.KING);

        assertInstanceOf(ResignationResult.Success.class, rejected);
        assertEquals(NobleRank.PREMIER, kingdomService.getMembership(PREMIER).orElseThrow().getRank());
        assertTrue(resignationService.pendingResignation("northmarch").isEmpty());
    }

    @Test
    void mpOffersResignationForOwnSeat() {
        ResignationResult offered = resignationService.offerResignation("northmarch", MP_ONE, OptionalInt.empty());

        assertInstanceOf(ResignationResult.Success.class, offered);
        assertEquals(
                ResignationSubjectKind.PLAYER_MP,
                resignationService.pendingResignation("northmarch").orElseThrow().subject().kind());
    }

    @Test
    void kingAcceptsMpResignationAndCallsByElection() {
        resignationService.offerResignation("northmarch", MP_ONE, OptionalInt.empty());

        ResignationResult accepted = resignationService.acceptResignation("northmarch", NobleRank.KING);

        assertInstanceOf(ResignationResult.Success.class, accepted);
        assertTrue(kingdomService.getMembership(MP_ONE).orElseThrow().getRank() != NobleRank.MP);
        var election = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().election();
        assertEquals(ElectionType.BY_ELECTION_PLAYER, election.type().orElseThrow());
        assertEquals(1, election.byElectionSeatIndex().orElseThrow());
    }

    @Test
    void villagerPremierResignationUsesSamePendingFlow() {
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        kingdomService.clearTitle(PREMIER);
        electionState.seat(2).orElseThrow().assignVillager("farmer", UUID.randomUUID());
        electionState.setPremierVillagerSeatIndex(2);

        ResignationResult offered = resignationService.offerResignation("northmarch", MP_ONE, OptionalInt.of(2));

        assertInstanceOf(ResignationResult.Success.class, offered);
        assertEquals(
                ResignationSubjectKind.VILLAGER_PREMIER,
                resignationService.pendingResignation("northmarch").orElseThrow().subject().kind());
        assertTrue(electionState.isPremierVillagerSeat(2));
    }

    @Test
    void princeMayResolveResignationWhenNoMonarchIsSeated() {
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.clearTitle(KING);
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        resignationService.offerResignation("northmarch", PREMIER, OptionalInt.empty());

        ResignationResult accepted = resignationService.acceptResignation("northmarch", NobleRank.PRINCE);

        assertInstanceOf(ResignationResult.Success.class, accepted);
    }

    @Test
    void princeCannotResolveResignationWhileMonarchIsSeated() {
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        resignationService.offerResignation("northmarch", PREMIER, OptionalInt.empty());

        ResignationResult accepted = resignationService.acceptResignation("northmarch", NobleRank.PRINCE);

        assertInstanceOf(ResignationResult.Failure.class, accepted);
    }

    @Test
    void villagerMpResignationTargetsSeatByIndex() {
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        electionState.seat(5).orElseThrow().assignVillager("librarian", UUID.randomUUID());

        ResignationResult offered = resignationService.offerResignation("northmarch", MP_ONE, OptionalInt.of(5));

        assertInstanceOf(ResignationResult.Success.class, offered);
        assertEquals(
                ResignationSubjectKind.VILLAGER_MP,
                resignationService.pendingResignation("northmarch").orElseThrow().subject().kind());
    }

    @Test
    void acceptingVillagerMpResignationVacatesSeatAndOpensByElection() {
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        electionState.seat(5).orElseThrow().assignVillager("librarian", UUID.randomUUID());
        resignationService.offerResignation("northmarch", MP_ONE, OptionalInt.of(5));

        ResignationResult accepted = resignationService.acceptResignation("northmarch", NobleRank.KING);

        assertInstanceOf(ResignationResult.Success.class, accepted);
        assertTrue(electionState.seat(5).orElseThrow().kind() == null);
        assertEquals(
                ElectionType.BY_ELECTION_VILLAGER,
                electionState.election().type().orElseThrow());
    }
}
