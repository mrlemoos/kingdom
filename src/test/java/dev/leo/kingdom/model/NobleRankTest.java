package dev.leo.kingdom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class NobleRankTest {

    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void dukeTitlePrefixIsBlue() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.DUKE, TitleStyle.MASCULINE);

        assertTrue(membership.coloredChatPrefix().startsWith(String.valueOf(ChatColor.BLUE)));
        assertEquals(ChatColor.BLUE + "" + ChatColor.BOLD + "DUKE ", membership.coloredChatPrefix());
    }

    @Test
    void duchessTitlePrefixIsBlue() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.DUKE, TitleStyle.FEMININE);

        assertEquals(ChatColor.BLUE + "" + ChatColor.BOLD + "DUCHESS ", membership.coloredChatPrefix());
    }

    @Test
    void kingTitlePrefixIsGold() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KING, TitleStyle.MASCULINE);

        assertEquals(ChatColor.GOLD + "" + ChatColor.BOLD + "KING ", membership.coloredChatPrefix());
    }

    @Test
    void countTitlePrefixIsRed() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.COUNT, TitleStyle.MASCULINE);

        assertEquals(ChatColor.RED + "" + ChatColor.BOLD + "COUNT ", membership.coloredChatPrefix());
    }

    @Test
    void premierTitlePrefixIsDarkGreen() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.PREMIER, TitleStyle.MASCULINE);

        assertEquals(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "PREMIER ", membership.coloredChatPrefix());
    }

    @Test
    void ladyTitlePrefixIsLightPurple() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.LORD, TitleStyle.FEMININE);

        assertEquals(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "LADY ", membership.coloredChatPrefix());
    }

    @Test
    void mpTitlePrefixIsGray() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.MP, TitleStyle.MASCULINE);

        assertEquals(ChatColor.GRAY + "" + ChatColor.BOLD + "MP ", membership.coloredChatPrefix());
    }

    @Test
    void knightTitlePrefixIsWhite() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KNIGHT, TitleStyle.MASCULINE);

        assertEquals(ChatColor.WHITE + "" + ChatColor.BOLD + "KNIGHT ", membership.coloredChatPrefix());
    }

    @Test
    void dameUsesKnightDisplay() {
        PlayerMembership membership = new PlayerMembership(playerId, "northmarch");
        membership.assignTitle(NobleRank.KNIGHT, TitleStyle.FEMININE);

        assertEquals(ChatColor.WHITE + "" + ChatColor.BOLD + "DAME ", membership.coloredChatPrefix());
    }

    @Test
    void hierarchyOrdersPremierAboveDuke() {
        assertTrue(NobleRank.PREMIER.hierarchyOrder() < NobleRank.DUKE.hierarchyOrder());
        assertTrue(NobleRank.DUKE.hierarchyOrder() < NobleRank.MP.hierarchyOrder());
        assertTrue(NobleRank.MP.hierarchyOrder() < NobleRank.KNIGHT.hierarchyOrder());
    }
}
