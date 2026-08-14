package dev.mrlemoos.kingdom.display;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Hangs the realm's board at a subject's right hand while they stand upon a kingdom's linked territory, and
 * takes it down the moment they step off it.
 *
 * <p>The board must live on a scoreboard of the player's own, so the noble prefix teams of the main board are
 * mirrored onto it; the main board remains the one place prefixes are written.
 */
public final class RealmSidebarService {

    private static final String OBJECTIVE_NAME = "kingdom_realm";

    private final KingdomService kingdomService;
    private final EconomyService economyService;
    private final RealmCalendarService calendarService;
    private final TerritoryResolver territoryResolver;
    private final MoraleService moraleService;

    public RealmSidebarService(
            KingdomService kingdomService,
            EconomyService economyService,
            RealmCalendarService calendarService,
            TerritoryResolver territoryResolver,
            MoraleService moraleService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.calendarService = Objects.requireNonNull(calendarService, "calendarService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.moraleService = moraleService;
    }

    public void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        Optional<RealmSidebar> sidebar = sidebarFor(player);
        if (sidebar.isEmpty()) {
            hide(player);
            return;
        }
        show(player, sidebar.get());
    }

    private Optional<RealmSidebar> sidebarFor(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        Optional<String> owner = territoryResolver.owningKingdomId(
                world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (owner.isEmpty()) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(owner.get());
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        return RealmSidebar.of(
                kingdom.get().getDisplayName(),
                calendarService.currentSeason(),
                economyService.getWalletBalance(player.getUniqueId()),
                moraleLabel(player.getUniqueId()));
    }

    private String moraleLabel(UUID playerId) {
        if (moraleService == null) {
            return "Not open";
        }
        Optional<MoraleTier> tier = moraleService.tierOf(playerId);
        if (tier.isEmpty()) {
            return "Not open";
        }
        return switch (tier.get()) {
            case STEADFAST -> "Steadfast";
            case SHAKEN -> "Shaken";
            case BREAKING -> "Breaking";
            case ROUT -> "Rout";
        };
    }

    private void show(Player player, RealmSidebar sidebar) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard board = player.getScoreboard();
        if (board == main) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        mirrorTeams(main, board);

        Objective existing = board.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            existing.unregister();
        }
        Objective objective =
                board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, component(sidebar.title()));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = sidebar.lines();
        for (int i = 0; i < lines.size(); i++) {
            objective.getScore(c(lines.get(i))).setScore(lines.size() - i);
        }
    }

    private void hide(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective(OBJECTIVE_NAME) == null) {
            return;
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /** Carries the noble prefix teams of the main board onto a player's own, so tab and nametags read the same. */
    private static void mirrorTeams(Scoreboard main, Scoreboard target) {
        for (Team stale : target.getTeams()) {
            if (main.getTeam(stale.getName()) == null) {
                stale.unregister();
            }
        }
        for (Team team : main.getTeams()) {
            Team mirrored = target.getTeam(team.getName());
            if (mirrored == null) {
                mirrored = target.registerNewTeam(team.getName());
            }
            mirrored.prefix(team.prefix());
            mirrored.suffix(team.suffix());
            for (String entry : team.getEntries()) {
                if (!mirrored.hasEntry(entry)) {
                    mirrored.addEntry(entry);
                }
            }
        }
    }
}
