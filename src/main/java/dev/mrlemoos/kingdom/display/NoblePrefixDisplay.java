package dev.mrlemoos.kingdom.display;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NoblePrefixDisplay {

    private static final String TEAM_PREFIX = "k";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PlayerPrefixComposer prefixComposer;

    public NoblePrefixDisplay(dev.mrlemoos.kingdom.service.KingdomService kingdomService) {
        this(new PlayerPrefixComposer(kingdomService));
    }

    public NoblePrefixDisplay(PlayerPrefixComposer prefixComposer) {
        this.prefixComposer = Objects.requireNonNull(prefixComposer, "prefixComposer");
    }

    public void refresh(Player player) {
        String prefix = prefixComposer.fullColouredPrefix(player.getUniqueId());
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
        team.prefix(LEGACY.deserialize(prefixComposer.fullColouredPrefix(player.getUniqueId())));
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

    private static String teamNameFor(UUID playerId) {
        String hex = playerId.toString().replace("-", "");
        return TEAM_PREFIX + hex.substring(0, Math.min(15, hex.length()));
    }
}
