package dev.mrlemoos.kingdom.display;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NoblePrefixDisplay {

    private static final String TEAM_PREFIX = "k";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final KingdomService service;
    private final PoliceService policeService;

    public NoblePrefixDisplay(KingdomService service) {
        this(service, null);
    }

    public NoblePrefixDisplay(KingdomService service, PoliceService policeService) {
        this.service = service;
        this.policeService = policeService;
    }

    public void refresh(Player player) {
        String prefix = fullColouredPrefix(player.getUniqueId());
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
        team.prefix(LEGACY.deserialize(fullColouredPrefix(player.getUniqueId())));
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
        return NobleRank.MP.chatColor() + "[MP] " + c("&f" + suffixLabel);
    }

    /** Premier villager nametag: same {@code [Premier] } prefix as player scoreboard teams, plus profession label. */
    public static String premierVillagerNametag(String suffixLabel) {
        String title = NobleRank.PREMIER.displayTitle(TitleStyle.MASCULINE);
        return NobleRank.PREMIER.chatColor() + "[" + title + "] " + c("&f" + suffixLabel);
    }

    /** Speaker villager nametag: the prefix alone—an impartial Chair carries no profession label. */
    public static String speakerVillagerNametag() {
        String title = NobleRank.SPEAKER.displayTitle(TitleStyle.MASCULINE);
        return NobleRank.SPEAKER.chatColor() + "[" + title + "]";
    }

    private String fullColouredPrefix(UUID playerId) {
        String sworn = "";
        if (policeService != null) {
            Optional<PlayerMembership> membership = service.getMembership(playerId);
            if (membership.isPresent()) {
                sworn = policeService.colouredSwornChatPrefix(membership.get().getKingdomId(), playerId);
            }
        }
        return sworn + service.colouredNobleChatPrefix(playerId);
    }

    private static String teamNameFor(UUID playerId) {
        String hex = playerId.toString().replace("-", "");
        return TEAM_PREFIX + hex.substring(0, Math.min(15, hex.length()));
    }
}
