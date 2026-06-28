package dev.mrlemoos.kingdom.display;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NoblePrefixDisplay {

    private static final String TEAM_PREFIX = "k";

    private final KingdomService service;

    public NoblePrefixDisplay(KingdomService service) {
        this.service = service;
    }

    public void refresh(Player player) {
        String prefix = service.nobleChatPrefix(player.getUniqueId());
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamNameFor(player.getUniqueId());
        Team team = board.getTeam(teamName);

        if (prefix.isEmpty()) {
            if (team != null) {
                team.removeEntry(player.getName());
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
            return;
        }

        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.setPrefix(service.coloredNobleChatPrefix(player.getUniqueId()));
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    /** Custom entity nametag: same {@code [MP] } prefix as player scoreboard teams, plus a white suffix label. */
    public static String mpVillagerNametag(String suffixLabel) {
        return NobleRank.MP.chatColor() + "[MP] " + ChatColor.WHITE + suffixLabel;
    }

    /** Premier villager nametag: same {@code [Premier] } prefix as player scoreboard teams, plus profession label. */
    public static String premierVillagerNametag(String suffixLabel) {
        String title = NobleRank.PREMIER.displayTitle(TitleStyle.MASCULINE);
        return NobleRank.PREMIER.chatColor() + "[" + title + "] " + ChatColor.WHITE + suffixLabel;
    }

    private static String teamNameFor(UUID playerId) {
        String hex = playerId.toString().replace("-", "");
        return TEAM_PREFIX + hex.substring(0, Math.min(15, hex.length()));
    }
}
