package dev.leo.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TitleStyle;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KingdomServiceTest {

    private KingdomService service;
    private final UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID carol = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID dave = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        service = new KingdomService();
        service.createKingdom("northmarch", "Northmarch");
        service.createKingdom("riviera", "Riviera");
    }

    @Test
    void playerCanJoinKingdomOnce() {
        KingdomResult joined = service.joinKingdom(alice, "northmarch");

        assertInstanceOf(KingdomResult.Success.class, joined);
        assertEquals("northmarch", service.getMembership(alice).orElseThrow().getKingdomId());
    }

    @Test
    void playerCannotJoinTwice() {
        service.joinKingdom(alice, "northmarch");

        KingdomResult secondJoin = service.joinKingdom(alice, "riviera");

        assertInstanceOf(KingdomResult.Failure.class, secondJoin);
        assertEquals("northmarch", service.getMembership(alice).orElseThrow().getKingdomId());
    }

    @Test
    void adminMoveClearsNobleTitle() {
        service.joinKingdom(alice, "northmarch");
        service.assignTitle(alice, NobleRank.DUKE, TitleStyle.MASCULINE);

        KingdomResult moved = service.movePlayer(alice, "riviera");

        assertInstanceOf(KingdomResult.Success.class, moved);
        PlayerMembership membership = service.getMembership(alice).orElseThrow();
        assertEquals("riviera", membership.getKingdomId());
        assertTrue(membership.chatPrefix().isEmpty());
    }

    @Test
    void dukeSlotsAreLimitedToTwo() {
        service.joinKingdom(alice, "northmarch");
        service.joinKingdom(bob, "northmarch");
        service.joinKingdom(carol, "northmarch");

        service.assignTitle(alice, NobleRank.DUKE, TitleStyle.MASCULINE);
        service.assignTitle(bob, NobleRank.DUKE, TitleStyle.FEMININE);

        KingdomResult thirdDuke = service.assignTitle(carol, NobleRank.DUKE, TitleStyle.MASCULINE);

        assertInstanceOf(KingdomResult.Failure.class, thirdDuke);
    }

    @Test
    void duchessUsesDukeSlotWithFeminineDisplay() {
        service.joinKingdom(alice, "northmarch");

        service.assignTitle(alice, NobleRank.DUKE, TitleStyle.FEMININE);

        assertEquals("[Duchess] ", service.nobleChatPrefix(alice));
    }

    @Test
    void citizensHaveNoChatPrefix() {
        service.joinKingdom(alice, "northmarch");

        assertEquals("", service.nobleChatPrefix(alice));
    }

    @Test
    void premierSlotsAreLimitedToOne() {
        service.joinKingdom(alice, "northmarch");
        service.joinKingdom(bob, "northmarch");

        service.assignTitle(alice, NobleRank.PREMIER, TitleStyle.MASCULINE);

        KingdomResult secondPremier = service.assignTitle(bob, NobleRank.PREMIER, TitleStyle.MASCULINE);

        assertInstanceOf(KingdomResult.Failure.class, secondPremier);
    }

    @Test
    void knightSlotsAreUnlimited() {
        service.joinKingdom(alice, "northmarch");
        service.joinKingdom(bob, "northmarch");
        service.joinKingdom(carol, "northmarch");
        service.joinKingdom(dave, "northmarch");

        assertInstanceOf(KingdomResult.Success.class, service.assignTitle(alice, NobleRank.KNIGHT, TitleStyle.MASCULINE));
        assertInstanceOf(KingdomResult.Success.class, service.assignTitle(bob, NobleRank.KNIGHT, TitleStyle.MASCULINE));
        assertInstanceOf(KingdomResult.Success.class, service.assignTitle(carol, NobleRank.KNIGHT, TitleStyle.MASCULINE));
        assertInstanceOf(KingdomResult.Success.class, service.assignTitle(dave, NobleRank.KNIGHT, TitleStyle.FEMININE));
    }

    @Test
    void princeSlotsAreLimitedToTwo() {
        service.joinKingdom(alice, "northmarch");
        service.joinKingdom(bob, "northmarch");
        service.joinKingdom(carol, "northmarch");

        service.assignTitle(alice, NobleRank.PRINCE, TitleStyle.MASCULINE);
        service.assignTitle(bob, NobleRank.PRINCE, TitleStyle.FEMININE);

        KingdomResult thirdPrince = service.assignTitle(carol, NobleRank.PRINCE, TitleStyle.MASCULINE);

        assertInstanceOf(KingdomResult.Failure.class, thirdPrince);
    }

    @Test
    void princessUsesPrinceSlotWithFeminineDisplay() {
        service.joinKingdom(alice, "northmarch");

        service.assignTitle(alice, NobleRank.PRINCE, TitleStyle.FEMININE);

        assertEquals("[Princess] ", service.nobleChatPrefix(alice));
    }

    @Test
    void ladyUsesLordSlotWithFeminineDisplay() {
        service.joinKingdom(alice, "northmarch");

        service.assignTitle(alice, NobleRank.LORD, TitleStyle.FEMININE);

        assertEquals("[Lady] ", service.nobleChatPrefix(alice));
    }

    @Test
    void newKingdomDefaultsLinkedWorldToOverworld() {
        Kingdom kingdom = service.getKingdom("northmarch").orElseThrow();

        assertEquals(KingdomService.DEFAULT_WORLD, service.resolveWorldName(kingdom));
    }

    @Test
    void territoryLabelIncludesRegionAndWorld() {
        Kingdom kingdom = service.getKingdom("northmarch").orElseThrow();
        kingdom.setWorldGuardRegion("north_hold");
        kingdom.setWorldName("world");

        assertEquals("north_hold (world)", service.territoryLabel(kingdom).orElseThrow());
    }

    @Test
    void territoryLabelEmptyWhenNoRegionLinked() {
        Kingdom kingdom = service.getKingdom("northmarch").orElseThrow();

        assertTrue(service.territoryLabel(kingdom).isEmpty());
    }
}
