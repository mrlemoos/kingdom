package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchConsentBook;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchRites;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.ChurchSiting;
import dev.mrlemoos.kingdom.church.ClericService;
import dev.mrlemoos.kingdom.church.FuneralOutcome;
import dev.mrlemoos.kingdom.church.VillagerFuneralOutcome;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.honours.SwornRoleAppointments;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /kingdom church …}: siting, the priesthood, and every rite held at the altar. */
public final class KingdomChurchHandler {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final ClericService clericService;
    private final KingdomTerritoryResolver territoryResolver;
    private final ChurchRites rites;
    private final YamlKingdomStore store;
    private final ChurchSiting churchSiting;
    private SwornRoleAppointments swornRoles;

    public KingdomChurchHandler(
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService,
            KingdomTerritoryResolver territoryResolver,
            ChurchRites rites,
            YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.churchService = churchService;
        this.clericService = clericService;
        this.territoryResolver = territoryResolver;
        this.rites = rites;
        this.store = store;
        this.churchSiting = new ChurchSiting(kingdomService, churchService, clericService, store);
    }

    /** The one road for sworn roles, shared with the golden sword. */
    public void setSwornRoles(SwornRoleAppointments swornRoles) {
        this.swornRoles = swornRoles;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(help());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleSet(sender);
            case "clear" -> handleClear(sender);
            case "swear" -> handleSwear(sender, args);
            case "unswear" -> handleUnswear(sender, args);
            case "consecrate" -> atTheCleric(sender) || handleConsecrate(sender);
            case "marry" -> atTheCleric(sender) || handleMarry(sender, args);
            case "divorce" -> atTheCleric(sender) || handleDivorce(sender, args);
            case "annul" -> atTheCleric(sender) || handleAnnul(sender, args);
            case "funeral" -> atTheCleric(sender) || handleFuneral(sender, args);
            case "info" -> handleInfo(sender);
            default -> {
                sender.sendMessage(help());
                yield true;
            }
        };
    }

    // --- siting -----------------------------------------------------------

    private boolean handleSet(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(error("The church is raised by laying its foundation stone. "
                    + "Type /kingdom and take it from The Church."));
            return true;
        }
        Optional<Player> player = asPlayer(sender, "Only players can site a church.");
        if (player.isEmpty()) {
            return true;
        }
        // The operators' escape hatch: raise the church of whichever realm owns this ground.
        Location location = player.get().getLocation();
        Optional<String> owner = territoryResolver.owningKingdomId(
                worldName(location),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        if (owner.isEmpty()) {
            sender.sendMessage(error("Stand inside a kingdom's territory to site its church."));
            return true;
        }
        ChurchSite site = ChurchSite.of(
                worldName(location),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        report(sender, churchSiting.site(owner.get(), NobleRank.KING, site));
        return true;
    }

    private boolean handleClear(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(error("The church's site is cleared from the Realm Hub. "
                    + "Type /kingdom, open The Church and right-click it."));
            return true;
        }
        Optional<Player> player = asPlayer(sender, "Only players can clear a church.");
        if (player.isEmpty()) {
            return true;
        }
        Location location = player.get().getLocation();
        Optional<String> kingdomId = territoryResolver.owningKingdomId(
                worldName(location),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        if (kingdomId.isEmpty()) {
            Optional<PlayerMembership> membership = kingdomService.getMembership(player.get().getUniqueId());
            if (membership.isPresent()) {
                kingdomId = Optional.of(membership.get().getKingdomId());
            }
        }
        if (kingdomId.isEmpty()) {
            sender.sendMessage(error("Stand inside a kingdom's territory to clear its church."));
            return true;
        }
        report(sender, churchSiting.clear(kingdomId.get(), NobleRank.KING));
        return true;
    }

    // --- the priesthood ---------------------------------------------------

    private boolean handleSwear(CommandSender sender, String[] args) {
        if (refusedUnlessOperator(sender)) {
            return true;
        }
        if (args.length < 2 || swornRoles == null) {
            sender.sendMessage(error("Usage: /kingdom church swear <player>"));
            return true;
        }
        Optional<UUID> subject = resolvePlayer(sender, args[1]);
        if (subject.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(subject.get());
        if (membership.isEmpty()) {
            sender.sendMessage(error("That player is not a member of any kingdom."));
            return true;
        }
        return reportSworn(sender, membership.get().getKingdomId(), swornRoles.swear(
                membership.get().getKingdomId(), null, NobleRank.KING, subject.get(), SwornRole.PRIEST));
    }

    private boolean handleUnswear(CommandSender sender, String[] args) {
        if (refusedUnlessOperator(sender)) {
            return true;
        }
        Optional<String> kingdomId = Optional.empty();
        if (args.length >= 2) {
            Optional<UUID> subject = resolvePlayer(sender, args[1]);
            if (subject.isEmpty()) {
                return true;
            }
            Optional<PlayerMembership> membership = kingdomService.getMembership(subject.get());
            if (membership.isPresent()) {
                kingdomId = Optional.of(membership.get().getKingdomId());
            }
        } else if (sender instanceof Player player) {
            Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
            if (membership.isPresent()) {
                kingdomId = Optional.of(membership.get().getKingdomId());
            }
        }
        if (kingdomId.isEmpty() || swornRoles == null) {
            sender.sendMessage(error("Usage: /kingdom church unswear <player>"));
            return true;
        }
        Optional<UUID> priest = churchService.priest(kingdomId.get());
        if (priest.isEmpty()) {
            sender.sendMessage(error("This kingdom has no priest."));
            return true;
        }
        return reportSworn(sender, kingdomId.get(), swornRoles.unswear(
                kingdomId.get(), null, NobleRank.KING, priest.get(), SwornRole.PRIEST));
    }

    private boolean reportSworn(CommandSender sender, String kingdomId, SwornRoleAppointments.Outcome outcome) {
        sender.sendMessage(outcome.success() ? success(outcome.message()) : error(outcome.message()));
        if (outcome.success()) {
            reconcileCleric(kingdomId);
            store.saveFrom(kingdomService);
        }
        return true;
    }

    /** The priesthood is sworn with the golden sword; the commands are the operators' escape hatch. */
    private static boolean refusedUnlessOperator(CommandSender sender) {
        if (sender.isOp()) {
            return false;
        }
        sender.sendMessage(error("The priest is sworn with the golden sword. "
                + "Strike a subject with one to open the honours window."));
        return true;
    }

    // --- rites ------------------------------------------------------------

    /** Rites are asked for at the cleric; the commands are the operators' escape hatch. */
    private static boolean atTheCleric(CommandSender sender) {
        if (sender.isOp()) {
            return false;
        }
        sender.sendMessage(error("Rites are asked for at the cleric. Right-click the cleric at the church."));
        return true;
    }

    private boolean handleConsecrate(CommandSender sender) {
        Optional<RiteContext> rite = riteContext(sender, false);
        if (rite.isEmpty()) {
            return true;
        }
        report(sender, rites.consecrate(rite.get().kingdomId(), rite.get().celebrant()));
        return true;
    }

    private boolean handleMarry(CommandSender sender, String[] args) {
        Optional<RiteContext> rite = riteContext(sender, true);
        if (rite.isEmpty()) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom church marry <player>"));
            return true;
        }
        Player other = Bukkit.getPlayerExact(args[1]);
        if (other == null) {
            sender.sendMessage(error("That player is not here."));
            return true;
        }
        String kingdomId = rite.get().kingdomId();
        if (!atChurch(kingdomId, other)) {
            sender.sendMessage(error("Both parties must stand at the church."));
            return true;
        }
        // Consent is given in the other party's window, as it is for everybody else.
        Optional<String> refusal =
                rites.propose(ChurchConsentBook.Kind.MARRIAGE, kingdomId, rite.get().player(), other);
        if (refusal.isPresent()) {
            sender.sendMessage(error(refusal.get()));
        }
        return true;
    }

    private boolean handleDivorce(CommandSender sender, String[] args) {
        Optional<RiteContext> rite = riteContext(sender, true);
        if (rite.isEmpty()) {
            return true;
        }
        String kingdomId = rite.get().kingdomId();
        UUID me = rite.get().player().getUniqueId();
        Optional<UUID> spouse = churchService.spouseOf(kingdomId, me);
        if (spouse.isEmpty()) {
            sender.sendMessage(error("You are not wed."));
            return true;
        }
        Player other = Bukkit.getPlayer(spouse.get());
        if (other == null) {
            sender.sendMessage(error("Your spouse must be here to consent. The Crown may annul instead."));
            return true;
        }
        Optional<String> refusal =
                rites.propose(ChurchConsentBook.Kind.DIVORCE, kingdomId, rite.get().player(), other);
        if (refusal.isPresent()) {
            sender.sendMessage(error(refusal.get()));
        }
        return true;
    }

    private boolean handleAnnul(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom church annul <player>"));
            return true;
        }
        Optional<UUID> subject = resolvePlayer(sender, args[1]);
        if (subject.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(subject.get());
        if (membership.isEmpty()) {
            sender.sendMessage(error("That player is sworn to no realm."));
            return true;
        }
        // The operators' escape hatch: annul in the subject's own realm with the Crown's authority.
        report(sender, rites.annul(membership.get().getKingdomId(), NobleRank.KING, subject.get()));
        return true;
    }

    private boolean handleFuneral(CommandSender sender, String[] args) {
        Optional<RiteContext> rite = riteContext(sender, true);
        if (rite.isEmpty()) {
            return true;
        }
        String kingdomId = rite.get().kingdomId();
        if (args.length > 1 && "villager".equalsIgnoreCase(args[1])) {
            return handleVillagerFuneral(sender, rite.get());
        }

        Player deceased = rite.get().player();
        if (args.length > 1) {
            Player named = Bukkit.getPlayerExact(args[1]);
            if (named == null) {
                sender.sendMessage(error("The deceased must be here for their rites."));
                return true;
            }
            deceased = named;
        }
        if (!atChurch(kingdomId, deceased)) {
            sender.sendMessage(error("The deceased must stand at the church."));
            return true;
        }
        FuneralOutcome outcome = rites.funeral(kingdomId, rite.get().celebrant(), deceased);
        report(sender, outcome.result());
        return true;
    }

    private boolean handleVillagerFuneral(CommandSender sender, RiteContext rite) {
        Optional<VillagerFuneralOutcome> outcome = rites.villagerFuneral(rite.kingdomId(), rite.celebrant());
        if (outcome.isEmpty()) {
            sender.sendMessage(error("No villager of this realm awaits its rites."));
            return true;
        }
        report(sender, outcome.get().result());
        if (outcome.get().result() instanceof ChurchResult.Success) {
            sender.sendMessage(info(tithedLine(outcome.get())));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        Optional<PlayerMembership> membership = sender instanceof Player player
                ? kingdomService.getMembership(player.getUniqueId())
                : Optional.empty();
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!churchService.hasChurch(kingdomId)) {
            sender.sendMessage(info("This realm has no church."));
            return true;
        }
        sender.sendMessage(info(churchService.isConsecrated(kingdomId)
                ? "The church stands consecrated."
                : "The church stands unconsecrated; no rite may be held."));
        if (churchService.massInSession(kingdomId)) {
            sender.sendMessage(info("Mass is being held. Come to the church for the blessing."));
        } else {
            Optional<Long> days = churchService.daysUntilMass(kingdomId);
            if (days.isPresent()) {
                sender.sendMessage(info(days.get() == 0L
                        ? "Mass falls due today."
                        : "Mass falls due in " + days.get() + " realm day(s)."));
            }
        }
        sender.sendMessage(info(churchService.priest(kingdomId)
                .map(priest -> "Priest: " + Bukkit.getOfflinePlayer(priest).getName())
                .orElse("No priest is sworn; the cleric presides.")));
        return true;
    }

    // --- helpers ----------------------------------------------------------

    /** A player standing at a consecrated church with somebody to celebrate the rite. */
    private record RiteContext(Player player, String kingdomId, Celebrant celebrant) {}

    private Optional<RiteContext> riteContext(CommandSender sender, boolean requireConsecration) {
        Optional<Player> player = asPlayer(sender, "Only players may take part in a rite.");
        if (player.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = membershipOf(sender, player.get());
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        String kingdomId = membership.get().getKingdomId();
        if (!churchService.hasChurch(kingdomId)) {
            sender.sendMessage(error("This realm has no church."));
            return Optional.empty();
        }
        if (requireConsecration && !churchService.isConsecrated(kingdomId)) {
            sender.sendMessage(error("This church has not been consecrated."));
            return Optional.empty();
        }
        if (!atChurch(kingdomId, player.get())) {
            sender.sendMessage(error("You must stand at the church."));
            return Optional.empty();
        }
        Celebrant celebrant = churchService.presidingCelebrant(kingdomId, priestAtChurch(kingdomId));
        if (celebrant == Celebrant.NONE) {
            sender.sendMessage(error("There is nobody at the altar to hold the rite."));
            return Optional.empty();
        }
        return Optional.of(new RiteContext(player.get(), kingdomId, celebrant));
    }

    private boolean priestAtChurch(String kingdomId) {
        return ChurchPresence.priestAtChurch(churchService, kingdomId);
    }

    private boolean atChurch(String kingdomId, Player player) {
        return ChurchPresence.atChurch(churchService, kingdomId, player);
    }

    private void reconcileCleric(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(clericService::reconcile);
    }

    private Optional<Player> asPlayer(CommandSender sender, String refusal) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(error(refusal));
        return Optional.empty();
    }

    private Optional<PlayerMembership> membershipOf(CommandSender sender, Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
        }
        return membership;
    }

    private Optional<UUID> resolvePlayer(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage(error("Unknown player: " + name));
            return Optional.empty();
        }
        return Optional.of(target.getUniqueId());
    }

    private void report(CommandSender sender, ChurchResult result) {
        sender.sendMessage(result instanceof ChurchResult.Failure
                ? error(result.message())
                : success(result.message()));
    }

    /** Where a villager's estate went, told to whoever asked for its rites. */
    public static String tithedLine(VillagerFuneralOutcome outcome) {
        return String.format(
                "%.2f Corona passes to the treasury; %.2f is tithed.", outcome.treasuryShare(), outcome.tithe());
    }

    private static String worldName(Location location) {
        return location.getWorld() == null ? "" : location.getWorld().getName();
    }

    private static String help() {
        return c("&6Church")
                + "\n" + c("&e/kingdom church set|clear") + c("&7 — operators; the Crown lays the stone from /kingdom")
                + "\n" + c("&e/kingdom church swear|unswear <player>") + c("&7 — operators; the Crown strikes a subject with a golden sword")
                + "\n" + c("&e/kingdom church consecrate|marry|divorce|annul|funeral")
                + c("&7 — operators; subjects right-click the cleric at the church")
                + "\n" + c("&e/kingdom church info") + c("&7 — how the church stands");
    }

    private static String error(String message) {
        return c("&c") + message;
    }

    private static String success(String message) {
        return c("&a") + message;
    }

    private static String info(String message) {
        return c("&7") + message;
    }
}
