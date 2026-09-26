package dev.mrlemoos.kingdom.poll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Slice 9.8 — the poll card is handed over once, and spent once the member has voted. */
class PollCardDeliveryTest {

    private static final String KINGDOM = "northmarch";

    private ServerMock server;
    private KingdomService kingdomService;
    private ElectionService electionService;
    private PollCardItem cards;
    private PollCardDelivery delivery;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        cards = new PollCardItem(MockBukkit.createMockPlugin());
        delivery = new PollCardDelivery(kingdomService, cards);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock member() {
        PlayerMock player = server.addPlayer();
        kingdomService.joinKingdom(player.getUniqueId(), KINGDOM);
        return player;
    }

    private long cardsHeld(PlayerMock player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(cards::isPollCard)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private String openPollId() {
        return PollCardRules.openPolls(kingdomService.getKingdom(KINGDOM).orElseThrow()).get(0).pollId();
    }

    @Test
    void aCardRemembersItsRealmAndItsPoll() {
        ItemStack card = cards.create(KINGDOM, "election:GENERAL:1", "General election");

        assertTrue(cards.isPollCard(card));
        assertEquals(KINGDOM, cards.kingdomId(card).orElseThrow());
        assertEquals("election:GENERAL:1", cards.pollId(card).orElseThrow());
        assertFalse(cards.isPollCard(new ItemStack(org.bukkit.Material.PAPER)));
    }

    @Test
    void deliveryIsIdempotent() {
        PlayerMock citizen = member();
        electionService.startGeneralElection(KINGDOM);

        delivery.deliverOwed(citizen);
        delivery.deliverOwed(citizen);

        assertEquals(1, cardsHeld(citizen));
        assertTrue(delivery.holds(citizen, openPollId()));
    }

    @Test
    void nothingIsDeliveredWhileNoPollIsOpen() {
        PlayerMock citizen = member();

        delivery.deliverOwed(citizen);

        assertEquals(0, cardsHeld(citizen));
    }

    @Test
    void theCrownIsHandedNoCardForAGeneralElection() {
        PlayerMock king = member();
        kingdomService.assignTitle(king.getUniqueId(), NobleRank.KING, TitleStyle.MASCULINE);
        electionService.startGeneralElection(KINGDOM);

        delivery.deliverOwed(king);

        assertEquals(0, cardsHeld(king));
    }

    @Test
    void aSpentCardIsNotRedeliveredOnJoin() {
        PlayerMock voter = member();
        PlayerMock candidate = member();
        electionService.startGeneralElection(KINGDOM);
        electionService.nominate(KINGDOM, candidate.getUniqueId());
        delivery.deliverOwed(voter);
        String pollId = openPollId();

        electionService.castElectionVote(KINGDOM, voter.getUniqueId(), candidate.getUniqueId());
        delivery.consume(voter, pollId);
        delivery.deliverOwed(voter);

        assertEquals(0, cardsHeld(voter));
    }

    @Test
    void theSweepHandsEveryOwedMemberOnlineACardWhenPollsOpen() {
        PlayerMock one = member();
        PlayerMock two = member();
        delivery.sweep();
        electionService.startGeneralElection(KINGDOM);

        delivery.sweep();
        delivery.sweep();

        assertEquals(1, cardsHeld(one));
        assertEquals(1, cardsHeld(two));
    }
}
