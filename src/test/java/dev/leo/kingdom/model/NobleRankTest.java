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
}
