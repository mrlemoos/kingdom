package dev.mrlemoos.kingdom.model;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NobleRankTest {

    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void dukeTitlePrefixIsBlue() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.DUKE, TitleStyle.MASCULINE);

        assertTrue(membership.colouredChatPrefix().startsWith(c("&9")));
        assertEquals(c("&9[Duke] "), membership.colouredChatPrefix());
    }

    @Test
    void duchessTitlePrefixIsBlue() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.DUKE, TitleStyle.FEMININE);

        assertEquals(c("&9[Duchess] "), membership.colouredChatPrefix());
    }

    @Test
    void kingTitlePrefixIsGold() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KING, TitleStyle.MASCULINE);

        assertEquals(c("&6[King] "), membership.colouredChatPrefix());
    }

    @Test
    void countTitlePrefixIsRed() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.COUNT, TitleStyle.MASCULINE);

        assertEquals(c("&c[Count] "), membership.colouredChatPrefix());
    }

    @Test
    void premierTitlePrefixIsDarkGreen() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.PREMIER, TitleStyle.MASCULINE);

        assertEquals(c("&2[Premier] "), membership.colouredChatPrefix());
    }

    @Test
    void ladyTitlePrefixIsLightPurple() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.LORD, TitleStyle.FEMININE);

        assertEquals(c("&d[Lady] "), membership.colouredChatPrefix());
    }

    @Test
    void mpTitlePrefixIsGray() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.MP, TitleStyle.MASCULINE);

        assertEquals(c("&7[MP] "), membership.colouredChatPrefix());
    }

    @Test
    void mpVillagerNametagUsesSamePrefixAsPlayer() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.MP, TitleStyle.MASCULINE);

        assertEquals(
                membership.colouredChatPrefix() + c("&fFarmer"),
                NoblePrefixDisplay.mpVillagerNametag("Farmer"));
    }

    @Test
    void premierVillagerNametagUsesSamePrefixAsPlayer() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.PREMIER, TitleStyle.MASCULINE);

        assertEquals(
                membership.colouredChatPrefix() + c("&fFarmer"),
                NoblePrefixDisplay.premierVillagerNametag("Farmer"));
    }

    @Test
    void knightTitlePrefixIsWhite() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KNIGHT, TitleStyle.MASCULINE);

        assertEquals(c("&f[Knight] "), membership.colouredChatPrefix());
    }

    @Test
    void dameUsesKnightDisplay() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KNIGHT, TitleStyle.FEMININE);

        assertEquals(c("&f[Dame] "), membership.colouredChatPrefix());
    }

    @Test
    void princeTitlePrefixIsYellow() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.PRINCE, TitleStyle.MASCULINE);

        assertEquals(c("&e[Prince] "), membership.colouredChatPrefix());
    }

    @Test
    void princessTitlePrefixIsYellow() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.PRINCE, TitleStyle.FEMININE);

        assertEquals(c("&e[Princess] "), membership.colouredChatPrefix());
    }

    @Test
    void princessResolvesFromCommandAsPrinceRank() {
        assertEquals(NobleRank.PRINCE, NobleRank.fromCommand("princess"));
    }

    @Test
    void hierarchyOrdersPrinceAbovePremier() {
        assertTrue(NobleRank.PRINCE.hierarchyOrder() < NobleRank.PREMIER.hierarchyOrder());
        assertTrue(NobleRank.PREMIER.hierarchyOrder() < NobleRank.DUKE.hierarchyOrder());
        assertTrue(NobleRank.DUKE.hierarchyOrder() < NobleRank.MP.hierarchyOrder());
        assertTrue(NobleRank.MP.hierarchyOrder() < NobleRank.KNIGHT.hierarchyOrder());
    }
}
