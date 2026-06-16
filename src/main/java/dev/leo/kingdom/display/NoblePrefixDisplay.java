package dev.leo.kingdom.display;

import dev.leo.kingdom.service.KingdomService;
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
        team.setPrefix(ChatColor.GOLD + prefix);
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private static String teamNameFor(UUID playerId) {
        String hex = playerId.toString().replace("-", "");
        return TEAM_PREFIX + hex.substring(0, Math.min(15, hex.length()));
    }
}
