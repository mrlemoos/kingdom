package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchConsentBook;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.ClericService;
import dev.mrlemoos.kingdom.church.FuneralOutcome;
import dev.mrlemoos.kingdom.church.VillagerFuneralOutcome;
import dev.mrlemoos.kingdom.city.CapitalSitingPolicy;
import dev.mrlemoos.kingdom.city.CapitalSitingPolicy.Verdict;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
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
    private final EconomyService economyService;
    private final YamlKingdomStore store;
    private final ChurchConsentBook consentBook = new ChurchConsentBook();

    public KingdomChurchHandler(
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService,
            KingdomTerritoryResolver territoryResolver,
            EconomyService economyService,
            YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.churchService = churchService;
        this.clericService = clericService;
        this.territoryResolver = territoryResolver;
        this.economyService = economyService;
        this.store = store;
    }

    public ChurchConsentBook consentBook() {
        return consentBook;
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
            case "unswear" -> handleUnswear(sender);
            case "consecrate" -> handleConsecrate(sender);
            case "marry" -> handleMarry(sender, args);
            case "divorce" -> handleDivorce(sender, args);
            case "annul" -> handleAnnul(sender, args);
            case "funeral" -> handleFuneral(sender, args);
            case "crown" -> handleCrown(sender);
            case "info" -> handleInfo(sender);
            default -> {
                sender.sendMessage(help());
                yield true;
            }
        };
    }

    // --- siting -----------------------------------------------------------

    private boolean handleSet(CommandSender sender) {
        Optional<Player> player = asPlayer(sender, "Only players can site a church.");
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = membershipOf(sender, player.get());
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Location location = player.get().getLocation();
        Optional<String> owner = territoryResolver.owningKingdomId(
                worldName(location),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        Verdict verdict = CapitalSitingPolicy.evaluate(membership.get().getRank(), kingdomId, owner);
        if (verdict != Verdict.ALLOWED) {
            sender.sendMessage(error(switch (verdict) {
                case NOT_THE_CROWN -> "Only the King or Queen may site a church.";
                case NO_KINGDOM -> "You must join a kingdom first.";
                case UNCLAIMED_LAND -> "A church must stand inside your kingdom's territory.";
                case FOREIGN_TERRITORY -> "You may not site a church in another realm's territory.";
                case ALLOWED -> "";
            }));
            return true;
        }

        ChurchSite site = ChurchSite.of(
                worldName(location),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        ChurchResult result = churchService.setChurch(kingdomId, membership.get().getRank(), site);
        if (result instanceof ChurchResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }
        // Moving the church moves the cleric with it; reconcile alone would leave it at the old altar.
        if (churchService.clericWanted(kingdomId)) {
            kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> clericService.spawn(kingdom, site));
        }
        store.saveFrom(kingdomService);
        sender.sendMessage(success(result.message()));
        return true;
    }

    private boolean handleClear(CommandSender sender) {
        Optional<PlayerMembership> membership = crownMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        // Despawn first: clearing the church forgets the cleric's id, and a forgotten cleric
        // stands at the old altar forever. Same order as dismissing the Town Crier.
        kingdomService.getKingdom(kingdomId).ifPresent(clericService::despawn);
        ChurchResult result = churchService.clearChurch(kingdomId, membership.get().getRank());
        if (result instanceof ChurchResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }
        store.saveFrom(kingdomService);
        sender.sendMessage(success(result.message()));
        return true;
    }

    // --- the priesthood ---------------------------------------------------

    private boolean handleSwear(CommandSender sender, String[] args) {
        Optional<PlayerMembership> membership = crownMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom church swear <player>"));
            return true;
        }
        Optional<String> uncrowned = ceremonialRefusal(sender, membership.get());
        if (uncrowned.isPresent()) {
            sender.sendMessage(error(uncrowned.get()));
            return true;
        }
        Optional<UUID> subject = resolvePlayer(sender, args[1]);
        if (subject.isEmpty()) {
            return true;
        }
        ChurchResult result = churchService.swearPriest(
                membership.get().getKingdomId(), membership.get().getRank(), subject.get());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            reconcileCleric(membership.get().getKingdomId());
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleUnswear(CommandSender sender) {
        Optional<PlayerMembership> membership = crownMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Optional<UUID> priest = churchService.priest(kingdomId);
        if (priest.isEmpty()) {
            sender.sendMessage(error("This kingdom has no priest."));
            return true;
        }
        ChurchResult result =
                churchService.unswearPriest(kingdomId, membership.get().getRank(), priest.get());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            reconcileCleric(kingdomId);
            store.saveFrom(kingdomService);
        }
        return true;
    }

    // --- rites ------------------------------------------------------------

    private boolean handleConsecrate(CommandSender sender) {
        Optional<RiteContext> rite = riteContext(sender, false);
        if (rite.isEmpty()) {
            return true;
        }
        ChurchResult result = churchService.consecrate(rite.get().kingdomId(), rite.get().celebrant());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            store.saveFrom(kingdomService);
        }
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
        UUID me = rite.get().player().getUniqueId();
        if (!consentBook.offerWedding(me, other.getUniqueId())) {
            sender.sendMessage(success("Your offer of marriage stands. It waits on their answer."));
            other.sendMessage(info(rite.get().player().getName()
                    + " offers you marriage. Answer with /kingdom church marry "
                    + rite.get().player().getName()));
            return true;
        }
        ChurchResult result = churchService.wed(kingdomId, rite.get().celebrant(), me, other.getUniqueId());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            other.sendMessage(success(result.message()));
            store.saveFrom(kingdomService);
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
        if (!consentBook.offerDivorce(me, spouse.get())) {
            sender.sendMessage(success("Your offer of divorce stands. It waits on their answer."));
            other.sendMessage(info(rite.get().player().getName()
                    + " asks the church to dissolve your marriage. Answer with /kingdom church divorce"));
            return true;
        }
        ChurchResult result = churchService.divorce(kingdomId, rite.get().celebrant(), me);
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            other.sendMessage(info(result.message()));
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleAnnul(CommandSender sender, String[] args) {
        Optional<PlayerMembership> membership = crownMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom church annul <player>"));
            return true;
        }
        Optional<UUID> subject = resolvePlayer(sender, args[1]);
        if (subject.isEmpty()) {
            return true;
        }
        ChurchResult result = churchService.annul(
                membership.get().getKingdomId(), membership.get().getRank(), subject.get());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            store.saveFrom(kingdomService);
        }
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
        FuneralOutcome outcome =
                churchService.funeral(kingdomId, rite.get().celebrant(), deceased.getUniqueId());
        report(sender, outcome.result());
        if (outcome.result() instanceof ChurchResult.Success) {
            deceased.giveExp(outcome.experience());
            deceased.sendMessage(success("The rites return " + outcome.experience() + " experience to you."));
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleVillagerFuneral(CommandSender sender, RiteContext rite) {
        Optional<UUID> longestWaiting = churchService.nextVillagerAwaitingRites(rite.kingdomId());
        if (longestWaiting.isEmpty()) {
            sender.sendMessage(error("No villager of this realm awaits its rites."));
            return true;
        }
        VillagerFuneralOutcome outcome =
                churchService.villagerFuneral(rite.kingdomId(), rite.celebrant(), longestWaiting.get());
        report(sender, outcome.result());
        if (outcome.result() instanceof ChurchResult.Success) {
            economyService.creditTreasury(rite.kingdomId(), outcome.treasuryShare());
            Optional<UUID> priest = churchService.priest(rite.kingdomId());
            if (outcome.titheToTreasury() || priest.isEmpty()) {
                economyService.creditTreasury(rite.kingdomId(), outcome.tithe());
            } else {
                // The tithe is the priest's living, not a fee for whoever asked for the rite.
                economyService.creditWalletDirect(priest.get(), outcome.tithe());
            }
            sender.sendMessage(info(String.format(
                    "%.2f Corona passes to the treasury; %.2f is tithed.",
                    outcome.treasuryShare(), outcome.tithe())));
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleCrown(CommandSender sender) {
        Optional<RiteContext> rite = riteContext(sender, true);
        if (rite.isEmpty()) {
            return true;
        }
        String kingdomId = rite.get().kingdomId();
        Optional<UUID> monarch = kingdomService.findMonarch(kingdomId).map(PlayerMembership::getPlayerId);
        if (monarch.isEmpty()) {
            sender.sendMessage(error("This realm has no monarch to crown."));
            return true;
        }
        Player crowned = Bukkit.getPlayer(monarch.get());
        if (crowned == null || !atChurch(kingdomId, crowned)) {
            sender.sendMessage(error("The monarch must stand at the church to be crowned."));
            return true;
        }
        ChurchResult result = churchService.crown(kingdomId, rite.get().celebrant(), monarch.get());
        report(sender, result);
        if (result instanceof ChurchResult.Success) {
            Bukkit.broadcastMessage(c("&6")
                    + kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId)
                    + " has crowned its monarch.");
            store.saveFrom(kingdomService);
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

    private Optional<PlayerMembership> crownMembership(CommandSender sender) {
        Optional<Player> player = asPlayer(sender, "Only the Crown may command the church.");
        if (player.isEmpty()) {
            return Optional.empty();
        }
        return membershipOf(sender, player.get());
    }

    /** The coronation gate: swearing the priesthood is a ceremonial power like any other. */
    private Optional<String> ceremonialRefusal(CommandSender sender, PlayerMembership membership) {
        if (!(sender instanceof Player player)) {
            return Optional.empty();
        }
        return churchService.ceremonialRefusal(
                membership.getKingdomId(), player.getUniqueId(), membership.getRank());
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

    private static String worldName(Location location) {
        return location.getWorld() == null ? "" : location.getWorld().getName();
    }

    private static String help() {
        return c("&6Church")
                + "\n" + c("&e/kingdom church set|clear") + c("&7 — King or Queen, inside your territory")
                + "\n" + c("&e/kingdom church swear|unswear <player>") + c("&7 — the priesthood")
                + "\n" + c("&e/kingdom church consecrate") + c("&7 — bring a new church into use")
                + "\n" + c("&e/kingdom church marry <player>") + c("&7 — both parties must ask")
                + "\n" + c("&e/kingdom church divorce") + c("&7 — both parties must ask")
                + "\n" + c("&e/kingdom church annul <player>") + c("&7 — the Crown's remedy")
                + "\n" + c("&e/kingdom church funeral [player|villager]") + c("&7 — the rites of the dead")
                + "\n" + c("&e/kingdom church crown") + c("&7 — crown the rightful monarch")
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
