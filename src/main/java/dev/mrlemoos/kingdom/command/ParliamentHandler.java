package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.economy.wealth.EstateBlockPlacer;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.election.VillagerPremierInauguralService;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.parliament.AssentedEnactmentResult;
import dev.mrlemoos.kingdom.parliament.DivisionBloc;
import dev.mrlemoos.kingdom.parliament.DivisionTally;
import dev.mrlemoos.kingdom.parliament.ParliamentEnactment;
import dev.mrlemoos.kingdom.parliament.RegistrarShelfWriter;
import dev.mrlemoos.kingdom.service.ChamberPresence;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParliamentHandler {

    private final ParliamentService parliamentService;
    private final KingdomService kingdomService;
    private final EconomyService economyService;
    private final YamlKingdomStore kingdomStore;
    private final YamlEconomyStore economyStore;
    private final TerritoryResolver territoryResolver;
    private final TreasuryLordService treasuryLordService;
    private final JavaPlugin plugin;
    private final VillagerPremierInauguralService villagerPremierInauguralService;
    private final WarService warService;
    private final DemobilisationService demobilisationService;
    private TreatyService treatyService;
    private dev.mrlemoos.kingdom.parliament.RoyalStandardPlacer royalStandardPlacer;
    private Consumer<Player> hubGuiOpener;
    private java.util.function.BiConsumer<Player, String> referendumBallotOpener;

    public ParliamentHandler(
            ParliamentService parliamentService,
            KingdomService kingdomService,
            EconomyService economyService,
            YamlKingdomStore kingdomStore,
            YamlEconomyStore economyStore,
            TerritoryResolver territoryResolver,
            TreasuryLordService treasuryLordService,
            JavaPlugin plugin,
            VillagerPremierInauguralService villagerPremierInauguralService) {
        this(
                parliamentService,
                kingdomService,
                economyService,
                kingdomStore,
                economyStore,
                territoryResolver,
                treasuryLordService,
                plugin,
                villagerPremierInauguralService,
                null,
                null);
    }

    public ParliamentHandler(
            ParliamentService parliamentService,
            KingdomService kingdomService,
            EconomyService economyService,
            YamlKingdomStore kingdomStore,
            YamlEconomyStore economyStore,
            TerritoryResolver territoryResolver,
            TreasuryLordService treasuryLordService,
            JavaPlugin plugin,
            VillagerPremierInauguralService villagerPremierInauguralService,
            WarService warService,
            DemobilisationService demobilisationService) {
        this.parliamentService = parliamentService;
        this.kingdomService = kingdomService;
        this.economyService = economyService;
        this.kingdomStore = kingdomStore;
        this.economyStore = economyStore;
        this.territoryResolver = territoryResolver;
        this.treasuryLordService = treasuryLordService;
        this.plugin = plugin;
        this.villagerPremierInauguralService = villagerPremierInauguralService;
        this.warService = warService;
        this.demobilisationService = demobilisationService;
    }

    /** Who raises the Royal Standard when the Lords point moves. */
    public void setRoyalStandardPlacer(dev.mrlemoos.kingdom.parliament.RoyalStandardPlacer royalStandardPlacer) {
        this.royalStandardPlacer = royalStandardPlacer;
    }

    public void setTreatyService(TreatyService treatyService) {
        this.treatyService = treatyService;
    }

    public void setHubGuiOpener(Consumer<Player> hubGuiOpener) {
        this.hubGuiOpener = hubGuiOpener;
    }

    /** How a member is shown the referendum ballot. */
    public void setReferendumBallotOpener(java.util.function.BiConsumer<Player, String> referendumBallotOpener) {
        this.referendumBallotOpener = referendumBallotOpener;
    }

    /**
     * {@code /kingdom referendum} — opens the ballot for any member while polling is open;
     * {@code call <question>} puts a question to the realm; {@code close} ends polling early.
     */
    public boolean handleReferendum(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();

        if (args.length >= 2 && args[1].equalsIgnoreCase("call")) {
            if (args.length < 3) {
                sender.sendMessage(error("Usage: /kingdom referendum call <question>"));
                return true;
            }
            String question = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            ParliamentResult result = parliamentService.callReferendum(
                    kingdomId, membership.get().getRank(), player.get().getUniqueId(), question);
            if (result instanceof ParliamentResult.Success success) {
                broadcastParliament(kingdomId, c("&e" + success.message()));
                promptRealmToVote(kingdomId);
            }
            return finish(sender, result);
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("close")) {
            ParliamentResult result = parliamentService.closePolling(kingdomId, membership.get().getRank());
            if (result instanceof ParliamentResult.Success success) {
                broadcastParliament(kingdomId, c("&e" + success.message()));
            }
            return finish(sender, result);
        }

        if (!parliamentService.isPollingOpen(kingdomId)) {
            sender.sendMessage(error("No referendum is open to the realm."));
            return true;
        }
        if (referendumBallotOpener != null) {
            referendumBallotOpener.accept(player.get(), kingdomId);
        }
        return true;
    }

    /** Tells everyone online in the realm that a question awaits their ballot. */
    public void promptRealmToVote(String kingdomId) {
        Optional<String> question = parliamentService.referendumQuestion(kingdomId);
        if (question.isEmpty() || !parliamentService.isPollingOpen(kingdomId)) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            promptIfEntitled(online, kingdomId, question.get());
        }
    }

    /** Prompts one member, used on login while polling is open. */
    public void promptIfEntitled(Player online, String kingdomId, String question) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(online.getUniqueId());
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(kingdomId)) {
            return;
        }
        online.sendMessage(c("&3[Parliament] ")+ c("&eA referendum is open: ")+ c("&f" + question));
        online.sendMessage(c("&7Use ")+ c("&e/kingdom referendum")+ c("&7 to cast your ballot."));
    }

    public ParliamentService parliamentService() {
        return parliamentService;
    }

    public KingdomService kingdomService() {
        return kingdomService;
    }

    public YamlKingdomStore kingdomStore() {
        return kingdomStore;
    }

    public YamlEconomyStore economyStore() {
        return economyStore;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && hubGuiOpener != null) {
                hubGuiOpener.accept(player);
                return true;
            }
            sender.sendMessage(help());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleSet(sender, args);
            case "status" -> handleStatus(sender);
            case "treaty" -> handleTreaty(sender, args);
            default -> {
                if (sender instanceof Player player && hubGuiOpener != null) {
                    hubGuiOpener.accept(player);
                    yield true;
                }
                sender.sendMessage(help());
                yield true;
            }
        };
    }

    private boolean handleTreaty(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) return true;
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) return true;
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom parliament treaty <kingdom> <non-aggression|trade-pact> [repeal]"));
            return true;
        }
        TreatyKind kind = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "non-aggression" -> TreatyKind.NON_AGGRESSION;
            case "trade-pact" -> TreatyKind.TRADE_PACT;
            default -> null;
        };
        if (kind == null) {
            sender.sendMessage(error("Treaty kind must be non-aggression or trade-pact."));
            return true;
        }
        boolean repeal = args.length > 3 && args[3].equalsIgnoreCase("repeal");
        return finish(sender, tableTreaty(
                membership.get().getKingdomId(), membership.get().getRank(), membership.get().getPlayerId(),
                args[1], kind, repeal, null));
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!isRoyal(membership.get().getRank())) {
            sender.sendMessage(error("Only the King or Queen may set parliamentary sites."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom parliament set commons|lords|speaker-chair|bar|registrar|mp-seat <1-8>"));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Location location = player.get().getLocation();
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "mp-seat" -> {
                if (args.length < 3) {
                    sender.sendMessage(error("Usage: /kingdom parliament set mp-seat <1-8>"));
                    yield true;
                }
                int seatIndex;
                try {
                    seatIndex = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(error("Seat index must be a number from 1 to 8."));
                    yield true;
                }
                var seatLocation = new dev.mrlemoos.kingdom.model.election.MpSeatLocation(
                        location.getWorld().getName(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch());
                ParliamentResult result = parliamentService.setMpSeat(kingdomId, seatIndex, seatLocation);
                yield finish(sender, result);
            }
            case "commons" -> {
                ParliamentResult result = parliamentService.setCommons(
                        kingdomId, ChamberSite.of(location.getWorld().getName(), location.getX(), location.getY(),
                                location.getZ()));
                yield finish(sender, result);
            }
            case "speaker-chair" -> {
                ParliamentResult result = parliamentService.setSpeakerChair(
                        kingdomId, ChamberSite.of(location.getWorld().getName(), location.getX(), location.getY(),
                                location.getZ()));
                yield finish(sender, result);
            }
            case "bar" -> {
                ParliamentResult result = parliamentService.setBar(
                        kingdomId, ChamberSite.of(location.getWorld().getName(), location.getX(), location.getY(),
                                location.getZ()));
                yield finish(sender, result);
            }
            case "lords" -> {
                Optional<Kingdom> kingdomOpt = kingdomService.getKingdom(kingdomId);
                Optional<ChamberSite> previousLords = kingdomOpt.flatMap(k -> k.getParliamentSites().lords());
                Optional<dev.mrlemoos.kingdom.model.parliament.KingdomFlag> held =
                        kingdomFlagFromHand(player.get());
                ParliamentResult result = parliamentService.setLords(
                        kingdomId, ChamberSite.of(location.getWorld().getName(), location.getX(), location.getY(),
                                location.getZ()));
                if (result instanceof ParliamentResult.Success) {
                    Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
                    if (kingdom.isPresent()) {
                        var resolved = dev.mrlemoos.kingdom.parliament.KingdomFlagResolver.resolve(
                                kingdom.get().getFlag(), held);
                        kingdom.get().setFlag(resolved);
                        if (held.isPresent()) {
                            consumeOneFromMainHand(player.get());
                        }
                    }
                }
                boolean done = finish(sender, result);
                if (result instanceof ParliamentResult.Success && royalStandardPlacer != null
                        && royalStandardPlacer.moveAndRaise(kingdomId, previousLords)) {
                    sender.sendMessage(success("The kingdom flag flies over the Lords."));
                }
                yield done;
            }
            case "registrar" -> {
                Block target = player.get().getTargetBlockExact(6);
                if (target == null || target.getType() != Material.CHISELED_BOOKSHELF) {
                    sender.sendMessage(error("Look at a chiseled bookshelf to set the registrar."));
                    yield true;
                }
                ParliamentResult result = parliamentService.setRegistrar(
                        kingdomId,
                        RegistrarSite.of(
                                target.getWorld().getName(),
                                target.getX(),
                                target.getY(),
                                target.getZ()));
                yield finish(sender, result);
            }
            default -> {
                sender.sendMessage(error("Usage: /kingdom parliament set commons|lords|speaker-chair|bar|registrar|mp-seat <1-8>"));
                yield true;
            }
        };
    }

    public boolean finish(CommandSender sender, ParliamentResult result) {
        if (result instanceof ParliamentResult.Success success) {
            sender.sendMessage(success(success.message()));
            kingdomStore.saveFrom(kingdomService);
            return true;
        }
        sender.sendMessage(error(((ParliamentResult.Failure) result).message()));
        return true;
    }

    public boolean enactAssentedBill(Player player, String kingdomId) {
        Optional<ParliamentService.AssentedActDraft> draft = parliamentService.draftForAssentedBill(kingdomId);
        if (draft.isEmpty()) {
            player.sendMessage(error("Bill assent could not be finalised."));
            return false;
        }

        int maxMints = plugin.getConfig().getInt("economy.max-mints-per-kingdom", 3);
        EstateBlockPlacer estatePlacer = (worldName, x, y, z, type) -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return false;
            }
            world.getBlockAt(x, y, z).setType(type.material());
            return true;
        };
        AssentedEnactmentResult enacted = ParliamentEnactment.enactAssented(
                draft.get(), economyService, warService, demobilisationService, treatyService, maxMints, estatePlacer);
        if (enacted instanceof AssentedEnactmentResult.Failure failure) {
            player.sendMessage(error("Royal assent recorded but enactment failed: " + failure.message()));
            kingdomStore.saveFrom(kingdomService);
            return false;
        }

        if (draft.get().payload() instanceof dev.mrlemoos.kingdom.model.parliament.BillPayload.SpendMint mint) {
            treasuryLordService.ensureLord(kingdomId, mint.mintLocation());
        }

        Optional<RegistrarSite> registrar = kingdomService
                .getKingdom(kingdomId)
                .flatMap(k -> k.getParliamentSites().registrar());
        if (registrar.isPresent()) {
            RegistrarShelfWriter.ShelfPlacement placement =
                    RegistrarShelfWriter.placeActBook(registrar.get(), draft.get().bookPages());
            parliamentService.commitArchivedAct(kingdomId, draft.get(), placement.shelf(), placement.slot());
        }

        parliamentService.clearAssentedBill(kingdomId);

        economyStore.saveFrom(economyService);
        kingdomStore.saveFrom(kingdomService);
        villagerPremierInauguralService.tablePendingBudgetAfterAssent(kingdomId);
        kingdomStore.saveFrom(kingdomService);
        player.sendMessage(success(
                ((AssentedEnactmentResult.Success) enacted).message() + " Act archived in the registrar."));
        broadcastParliament(kingdomId, c("&aRoyal assent granted: ")+ draft.get().title());
        if (warService != null
                && draft.get().payload() instanceof dev.mrlemoos.kingdom.model.parliament.BillPayload.War) {
            warService.activeWarFor(kingdomId).ifPresent(war -> Bukkit.broadcastMessage(
                    c("&4[War] " + warService.declarationMessage(war))));
        }
        RealmFeedback.royalAssent(kingdomService, kingdomId);
        return true;
    }

    public void broadcastCloseDivision(String kingdomId, String message) {
        broadcastParliament(kingdomId, message);
    }

    public void broadcastParliament(String kingdomId, String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            kingdomService.getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (membership.getKingdomId().equals(kingdomId)) {
                    online.sendMessage(c("&3[Parliament] ")+ c("&r" + message));
                }
            });
        }
    }

    public boolean inCommons(Player player, String kingdomId) {
        Optional<ChamberSite> commons = kingdomService.getKingdom(kingdomId)
                .flatMap(k -> k.getParliamentSites().commons());
        if (commons.isEmpty()) {
            return false;
        }
        Location location = player.getLocation();
        return ChamberPresence.withinChamber(
                commons.get(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                ChamberPresence.DEFAULT_RADIUS);
    }

    public boolean inLords(Player player, String kingdomId) {
        Optional<ChamberSite> lords = kingdomService.getKingdom(kingdomId).flatMap(k -> k.getParliamentSites().lords());
        if (lords.isEmpty()) {
            return false;
        }
        Location location = player.getLocation();
        return ChamberPresence.withinChamber(
                lords.get(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                ChamberPresence.DEFAULT_RADIUS);
    }

    public boolean isLecternInTerritory(Player player, Block lectern, String kingdomId) {
        Location location = lectern.getLocation();
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
        return territory.type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
    }

    public ParliamentResult tableFiscal(
            String kingdomId, NobleRank rank, UUID proposerId, FiscalRates proposed, String title) {
        return parliamentService.tableFiscal(kingdomId, rank, proposerId, proposed, title);
    }

    public ParliamentResult tableBudget(String kingdomId, NobleRank rank, UUID proposerId, double amount,
            String title) {
        return parliamentService.tableBudget(kingdomId, rank, proposerId, amount, title);
    }

    public ParliamentResult tableSpendMint(String kingdomId, NobleRank rank, UUID proposerId, String title) {
        double cost = plugin.getConfig().getDouble("economy.mint-placement-cost", 50.0);
        return parliamentService.tableSpendMint(kingdomId, rank, proposerId, cost, title);
    }

    public ParliamentResult tableSpendPublicWork(String kingdomId, NobleRank rank, UUID proposerId, String title) {
        RealmWealthRates rates = RealmWealthRates.fromPluginConfig(plugin.getConfig().getConfigurationSection("economy"));
        return parliamentService.tableSpendPublicWork(kingdomId, rank, proposerId, rates, title);
    }

    public ParliamentResult tableSpendStipend(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            UUID recipientId,
            double amount,
            String reason,
            String title) {
        return parliamentService.tableSpendStipend(
                kingdomId, rank, proposerId, recipientId, amount, reason, title);
    }

    public ParliamentResult tableWar(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            String targetKingdomId,
            WarAim aim,
            WarOutcome outcome,
            int musterDeadlineMcDays,
            String title) {
        return parliamentService.tableWar(
                kingdomId, rank, proposerId, targetKingdomId, aim, outcome, musterDeadlineMcDays, title);
    }

    public ParliamentResult tablePeace(String kingdomId, NobleRank rank, UUID proposerId, String title) {
        return parliamentService.tablePeace(kingdomId, rank, proposerId, title);
    }

    public ParliamentResult tableTreaty(
            String kingdomId, NobleRank rank, UUID proposerId, String counterpartKingdomId, TreatyKind kind,
            boolean repeal, String title) {
        return parliamentService.tableTreaty(
                kingdomId, rank, proposerId, counterpartKingdomId, kind, repeal, title);
    }

    /** Puts the confidence question and tells the House it awaits a seconder. */
    public ParliamentResult tableNoConfidenceWithBroadcast(
            Player player, String kingdomId, NobleRank rank, UUID proposerId) {
        ParliamentResult result = parliamentService.tableNoConfidence(kingdomId, rank, proposerId, null);
        if (result instanceof ParliamentResult.Success success) {
            player.sendMessage(success(success.message()));
            broadcastParliament(
                    kingdomId,
                    c("&cA motion of no confidence in the Premier has been tabled. It awaits a seconder."));
            kingdomStore.saveFrom(kingdomService);
            return result;
        }
        player.sendMessage(error(((ParliamentResult.Failure) result).message()));
        return result;
    }

    /** Seconds the motion before the House, opening the way to a division. */
    public ParliamentResult secondNoConfidenceWithBroadcast(
            Player player, String kingdomId, NobleRank rank, UUID seconderId) {
        ParliamentResult result = parliamentService.secondNoConfidence(kingdomId, rank, seconderId);
        if (result instanceof ParliamentResult.Success success) {
            player.sendMessage(success(success.message()));
            broadcastParliament(
                    kingdomId,
                    c("&cThe motion of no confidence has been seconded. The House may now divide."));
            kingdomStore.saveFrom(kingdomService);
            return result;
        }
        player.sendMessage(error(((ParliamentResult.Failure) result).message()));
        return result;
    }

    public ParliamentResult closeDivisionWithBroadcast(Player player, String kingdomId, NobleRank rank) {
        boolean motion = parliamentService.currentBill(kingdomId)
                .filter(bill -> bill.type() == dev.mrlemoos.kingdom.model.parliament.BillType.NO_CONFIDENCE)
                .isPresent();
        ParliamentResult result = parliamentService.closeDivision(kingdomId, rank);
        if (result instanceof ParliamentResult.Success success) {
            player.sendMessage(success(success.message()));
            if (motion) {
                broadcastParliament(kingdomId, c("&c" + success.message()));
                RealmFeedback.billFailed(kingdomService, kingdomId);
                broadcastDivisionBlocs(kingdomId);
                kingdomStore.saveFrom(kingdomService);
                return result;
            }
            if (success.message().contains("passed")) {
                broadcastParliament(
                        kingdomId,
                        c("&aA bill passed the Commons: ")+ parliamentService.currentBill(kingdomId).map(Bill::title).orElse("bill"));
                RealmFeedback.billPassed(kingdomService, kingdomId);
            } else if (success.message().contains("failed")) {
                broadcastParliament(kingdomId, c("&cA bill failed the Commons division."));
                RealmFeedback.billFailed(kingdomService, kingdomId);
            }
            broadcastDivisionBlocs(kingdomId);
            kingdomStore.saveFrom(kingdomService);
            return result;
        }
        player.sendMessage(error(((ParliamentResult.Failure) result).message()));
        return result;
    }

    /** Reads the division out bench by bench: each party, then the independents, then each bloc. */
    public void broadcastDivisionBlocs(String kingdomId) {
        List<DivisionBloc> blocs = parliamentService.lastDivisionBlocs(kingdomId);
        if (blocs.isEmpty()) {
            return;
        }
        broadcastParliament(kingdomId, c("&7The House divided:"));
        for (String line : DivisionTally.renderLines(blocs)) {
            broadcastParliament(kingdomId, c("&7 " + line));
        }
    }

    public ParliamentResult rejectWithBroadcast(Player player, String kingdomId, NobleRank rank) {
        ParliamentResult result = parliamentService.reject(kingdomId, rank);
        if (result instanceof ParliamentResult.Success success) {
            player.sendMessage(success(success.message()));
            villagerPremierInauguralService.clearPendingBudgetOnBillFailure(kingdomId);
            broadcastParliament(kingdomId, c("&cRoyal assent withheld. Bill rejected."));
            RealmFeedback.billFailed(kingdomService, kingdomId);
            kingdomStore.saveFrom(kingdomService);
            return result;
        }
        player.sendMessage(error(((ParliamentResult.Failure) result).message()));
        return result;
    }

    private boolean handleStatus(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        sender.sendMessage(info("Parliament status for " + kingdom.get().getDisplayName() + ":"));
        var sites = kingdom.get().getParliamentSites();
        sender.sendMessage(siteLine("Commons", sites.commons()));
        sender.sendMessage(siteLine("Lords", sites.lords()));
        sites.registrar()
                .ifPresentOrElse(
                        registrar -> sender.sendMessage(c("&7Registrar: ")+ c("&f" + registrar.worldName()) + " @ " + registrar.blockX() + ", "
                                + registrar.blockY() + ", " + registrar.blockZ()),
                        () -> sender.sendMessage(c("&7Registrar: ")+ c("&fnot set")));

        parliamentService.currentBill(kingdomId).ifPresentOrElse(
                bill -> {
                    sender.sendMessage(c("&7Current bill: ")+ c("&f" + bill.title()));
                    sender.sendMessage(c("&7State: ")+ c("&f" + bill.state().name().toLowerCase(Locale.ROOT)));
                },
                () -> sender.sendMessage(c("&7No bill is before Parliament.")));
        return true;
    }

    public Block findLecternBlock(Player player) {
        Block atFeet = player.getLocation().getBlock();
        if (atFeet.getType() == Material.LECTERN) {
            return atFeet;
        }
        Block below = atFeet.getRelative(0, -1, 0);
        if (below.getType() == Material.LECTERN) {
            return below;
        }
        Block target = player.getTargetBlockExact(5);
        if (target != null && target.getType() == Material.LECTERN) {
            return target;
        }
        return null;
    }

    static VoteChoice parseVote(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "aye", "yes", "y" -> VoteChoice.AYE;
            case "nay", "no", "n" -> VoteChoice.NAY;
            case "abstain" -> VoteChoice.ABSTAIN;
            default -> null;
        };
    }

    static String parseOptionalTitleFromEnd(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return null;
    }

    private static Optional<dev.mrlemoos.kingdom.model.parliament.KingdomFlag> kingdomFlagFromHand(Player player) {
        return dev.mrlemoos.kingdom.parliament.KingdomFlagItems.fromItem(player.getInventory().getItemInMainHand());
    }

    private static void consumeOneFromMainHand(Player player) {
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(error("Only players may use this command."));
        return Optional.empty();
    }

    public Optional<PlayerMembership> requireMembership(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        return membership;
    }

    private static boolean isRoyal(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    public String help() {
        return info("Parliament:")
                + "\n" + c("&e/kingdom parliament") + c("&7 — open the parliamentary hub (in Commons or Lords)")
                + "\n" + c("&e/kingdom parliament set commons|lords|speaker-chair|bar|registrar")
                + c("&7 — set chamber sites (monarch; hold a banner when setting lords to define the kingdom flag)")
                + "\n" + c("&e/kingdom parliament status") + c("&7 — view parliamentary state")
                + "\n" + c("&e/kingdom parliament treaty <kingdom> <non-aggression|trade-pact> [repeal]")
                + c("&7 — table a treaty bill (Crown)");
    }

    public String success(String message) {
        return c("&a" + message);
    }

    public String error(String message) {
        return c("&c" + message);
    }

    private String info(String message) {
        return c("&b" + message);
    }

    private String siteLine(String label, Optional<ChamberSite> site) {
        return site.map(chamber -> c("&7" + label) + ": " + c("&f" + chamber.worldName())
                + " @ " + String.format(Locale.UK, "%.1f, %.1f, %.1f", chamber.x(), chamber.y(), chamber.z()))
                .orElse(c("&7" + label) + ": " + c("&fnot set"));
    }
}
