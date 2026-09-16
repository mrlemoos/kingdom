package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.command.ParliamentHandler;
import dev.mrlemoos.kingdom.command.ResignCommand;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.parliament.ParliamentChatSessions;
import dev.mrlemoos.kingdom.parliament.gui.DivisionVoteGui;
import dev.mrlemoos.kingdom.parliament.gui.MintPrepareGui;
import dev.mrlemoos.kingdom.parliament.gui.ParliamentHubAction;
import dev.mrlemoos.kingdom.parliament.gui.ParliamentHubGui;
import dev.mrlemoos.kingdom.parliament.gui.ParliamentHubView;
import dev.mrlemoos.kingdom.parliament.gui.PublicWorkPrepareGui;
import dev.mrlemoos.kingdom.parliament.gui.ReferendumBallotGui;
import dev.mrlemoos.kingdom.parliament.gui.ResignationReviewGui;
import dev.mrlemoos.kingdom.parliament.gui.StipendSelectGui;
import dev.mrlemoos.kingdom.parliament.gui.WarPrepareGui;
import dev.mrlemoos.kingdom.model.parliament.PreparedPublicWork;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.resignation.ResignationSummaries;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ParliamentGuiListener implements Listener {

    private final ParliamentHandler handler;
    private final ParliamentService parliamentService;
    private final ResignCommand resignCommand;
    private dev.mrlemoos.kingdom.church.ChurchService churchService;
    private final ParliamentChatSessions chatSessions = new ParliamentChatSessions();
    private final Map<UUID, MintLocation> pendingMintLocations = new java.util.concurrent.ConcurrentHashMap<>();

    /** Wires the coronation gate; without it an uncrowned monarch may still grant assent. */
    public void setChurchService(dev.mrlemoos.kingdom.church.ChurchService churchService) {
        this.churchService = churchService;
    }

    public ParliamentGuiListener(ParliamentHandler handler, ResignCommand resignCommand) {
        this.handler = handler;
        this.parliamentService = handler.parliamentService();
        this.resignCommand = resignCommand;
    }

    public void openHubGui(Player player) {
        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        boolean commons = handler.inCommons(player, kingdomId);
        boolean lords = handler.inLords(player, kingdomId);
        if (!commons && !lords) {
            player.sendMessage(handler.error("You must be in the House of Commons or House of Lords."));
            return;
        }
        ParliamentHubView view = buildHubView(membership.get(), kingdomId, commons, lords);
        ParliamentHubGui gui = ParliamentHubGui.create(kingdomId, view);
        player.openInventory(gui.getInventory());
    }

    /** Opens the referendum ballot. Members may answer from anywhere—no chamber is required. */
    public void openReferendumBallotGui(Player player, String kingdomId) {
        Optional<String> question = parliamentService.referendumQuestion(kingdomId);
        if (question.isEmpty() || !parliamentService.isPollingOpen(kingdomId)) {
            player.sendMessage(handler.error("No referendum is open to the realm."));
            return;
        }
        int votesCast = parliamentService.currentReferendum(kingdomId)
                .map(referendum -> referendum.votesView().size())
                .orElse(0);
        ReferendumBallotGui gui = ReferendumBallotGui.create(
                kingdomId, question.get(), votesCast, parliamentService.electorate(kingdomId));
        player.openInventory(gui.getInventory());
    }

    public void openDivisionVoteGui(Player player, String kingdomId) {
        Optional<Bill> bill = parliamentService.currentBill(kingdomId);
        if (bill.isEmpty()
                || bill.get().state() != BillState.DIVISION_OPEN
                || bill.get().type() == dev.mrlemoos.kingdom.model.parliament.BillType.REFERENDUM) {
            return;
        }
        DivisionVoteGui gui = DivisionVoteGui.create(
                kingdomId, bill.get().title(), parliamentService.divisionBlocs(kingdomId));
        player.openInventory(gui.getInventory());
    }

    public void openMintPrepareGui(Player player, String kingdomId, MintLocation location) {
        pendingMintLocations.put(player.getUniqueId(), location);
        MintPrepareGui gui = MintPrepareGui.create(kingdomId, Optional.of(location));
        player.openInventory(gui.getInventory());
    }

    private ParliamentHubView buildHubView(
            PlayerMembership membership, String kingdomId, boolean inCommons, boolean inLords) {
        Optional<Bill> bill = parliamentService.currentBill(kingdomId);
        BillState billState = bill.map(Bill::state).orElse(null);
        Optional<String> billTitle = bill.map(Bill::title);

        boolean divisionTied = false;
        boolean castingVoteSet = false;
        if (bill.isPresent() && bill.get().state() == BillState.DIVISION_OPEN) {
            int aye = 0;
            int nay = 0;
            for (VoteChoice choice : bill.get().votesView().values()) {
                if (choice == VoteChoice.AYE) {
                    aye++;
                } else if (choice == VoteChoice.NAY) {
                    nay++;
                }
            }
            divisionTied = aye == nay;
            castingVoteSet = bill.get().speakerCastingVote().isPresent();
        }

        boolean hasPreparedMint = handler.kingdomService()
                .getKingdom(kingdomId)
                .flatMap(k -> k.getParliamentState().preparedMint())
                .isPresent();
        boolean hasPreparedPublicWork = handler.kingdomService()
                .getKingdom(kingdomId)
                .flatMap(k -> k.getParliamentState().preparedPublicWork())
                .isPresent();
        boolean electionActive = parliamentService.isPremierBlockedByElection(kingdomId);
        var pendingResignation = handler.kingdomService()
                .getKingdom(kingdomId)
                .flatMap(k -> k.getElectionState().pendingResignation());
        boolean canResolveResignation = ResignationAuthority.canResolveResignation(kingdomId, handler.kingdomService(),
                membership.getRank());

        return new ParliamentHubView(
                membership.getRank(),
                billState,
                inCommons,
                inLords,
                divisionTied,
                castingVoteSet,
                hasPreparedMint,
                hasPreparedPublicWork,
                electionActive,
                pendingResignation.isPresent(),
                canResolveResignation,
                billTitle,
                pendingResignation.map(ResignationSummaries::describe),
                parliamentService.canTableNoConfidence(kingdomId, membership.getRank(), membership.getPlayerId()),
                parliamentService.canSecondNoConfidence(kingdomId, membership.getRank(), membership.getPlayerId()),
                parliamentService.canTableWar(kingdomId, membership.getRank()),
                parliamentService.canTablePeace(kingdomId, membership.getRank()),
                parliamentService.canTableTreaty(kingdomId, membership.getRank()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof ParliamentHubGui hub) {
            handleHubClick(event, player, hub);
        } else if (event.getInventory().getHolder() instanceof DivisionVoteGui voteGui) {
            handleDivisionVoteClick(event, player, voteGui);
        } else if (event.getInventory().getHolder() instanceof ReferendumBallotGui ballotGui) {
            handleReferendumBallotClick(event, player, ballotGui);
        } else if (event.getInventory().getHolder() instanceof MintPrepareGui mintGui) {
            handleMintPrepareClick(event, player, mintGui);
        } else if (event.getInventory().getHolder() instanceof PublicWorkPrepareGui publicWorkGui) {
            handlePublicWorkPrepareClick(event, player, publicWorkGui);
        } else if (event.getInventory().getHolder() instanceof WarPrepareGui warGui) {
            handleWarPrepareClick(event, player, warGui);
        } else if (event.getInventory().getHolder() instanceof StipendSelectGui stipendGui) {
            handleStipendSelectClick(event, player, stipendGui);
        } else if (event.getInventory().getHolder() instanceof ResignationReviewGui resignationGui) {
            handleResignationReviewClick(event, player, resignationGui);
        }
    }

    private void handleHubClick(InventoryClickEvent event, Player player, ParliamentHubGui hub) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            player.closeInventory();
            return;
        }

        ParliamentHubAction action = hub.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }

        if (!hub.view().isEnabled(action)) {
            if (action == ParliamentHubAction.CLOSE_DIVISION && hub.view().closeDivisionBlocked()) {
                player.sendMessage(handler.error("Division is tied. Cast your vote before closing."));
            } else if (action == ParliamentHubAction.TABLE_SPEND_MINT) {
                player.sendMessage(handler.error("Prepare a mint location at a lectern first."));
            } else if (action == ParliamentHubAction.TABLE_SPEND_PUBLIC_WORK) {
                openPublicWorkPrepareFromLook(player, membership.get());
            }
            return;
        }

        String kingdomId = membership.get().getKingdomId();
        switch (action) {
            case TABLE_FISCAL -> startFiscalPrompt(player, kingdomId);
            case TABLE_BUDGET -> {
            }
            case CUSTOM_AMOUNT -> startBudgetCustomPrompt(player, kingdomId);
            case TABLE_SPEND_MINT -> tableMintBill(player, membership.get());
            case TABLE_SPEND_PUBLIC_WORK -> tablePublicWorkBill(player, membership.get());
            case TABLE_WAR -> startWarTargetPrompt(player, membership.get().getKingdomId());
            case TABLE_PEACE -> tablePeaceBill(player, membership.get());
            case TABLE_TREATY -> startTreatyPrompt(player, kingdomId);
            case TABLE_SPEND_STIPEND -> openStipendSelect(player, kingdomId);
            case STIPEND_OTHER -> startStipendOtherPrompt(player, kingdomId);
            case BUDGET_PRESET -> hub.budgetPresetAmountForSlot(event.getRawSlot())
                    .ifPresent(amount -> tableBudget(player, membership.get(), amount, null));
            case OPEN_DIVISION -> openDivision(player, membership.get());
            case CLOSE_DIVISION -> closeDivision(player, hub, membership.get());
            case CAST_AYE -> castSpeakerVote(player, membership.get(), VoteChoice.AYE);
            case CAST_NAY -> castSpeakerVote(player, membership.get(), VoteChoice.NAY);
            case VOTE_AYE -> castMpVote(player, membership.get(), VoteChoice.AYE);
            case VOTE_NAY -> castMpVote(player, membership.get(), VoteChoice.NAY);
            case VOTE_ABSTAIN -> castMpVote(player, membership.get(), VoteChoice.ABSTAIN);
            case ASSENT -> grantAssent(player, membership.get());
            case REJECT -> withholdAssent(player, membership.get());
            case TABLE_NO_CONFIDENCE -> tableNoConfidence(player, membership.get());
            case SECOND_NO_CONFIDENCE -> secondNoConfidence(player, membership.get());
            case REVIEW_RESIGNATION -> openResignationReview(player, hub.kingdomId());
            default -> {
            }
        }
    }

    private void tableNoConfidence(Player player, PlayerMembership membership) {
        handler.tableNoConfidenceWithBroadcast(player, membership.getKingdomId(), membership.getRank(),
                membership.getPlayerId());
        player.closeInventory();
    }

    private void secondNoConfidence(Player player, PlayerMembership membership) {
        handler.secondNoConfidenceWithBroadcast(player, membership.getKingdomId(), membership.getRank(),
                membership.getPlayerId());
        player.closeInventory();
    }

    private void openResignationReview(Player player, String kingdomId) {
        handler.kingdomService()
                .getKingdom(kingdomId)
                .flatMap(k -> k.getElectionState().pendingResignation())
                .ifPresent(pending -> {
                    ResignationReviewGui gui = ResignationReviewGui.create(kingdomId,
                            ResignationSummaries.describe(pending));
                    player.openInventory(gui.getInventory());
                });
    }

    private void handleResignationReviewClick(
            InventoryClickEvent event, Player player, ResignationReviewGui resignationGui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ParliamentHubAction action = resignationGui.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }

        switch (action) {
            case ACCEPT_RESIGNATION -> {
                resignCommand.accept(player, resignationGui.kingdomId());
                player.closeInventory();
            }
            case REJECT_RESIGNATION -> {
                resignCommand.reject(player, resignationGui.kingdomId());
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    private void handleStipendSelectClick(InventoryClickEvent event, Player player, StipendSelectGui stipendGui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            player.closeInventory();
            return;
        }

        if (stipendGui.isOtherPlayerSlot(event.getRawSlot())) {
            player.closeInventory();
            startStipendOtherPrompt(player, stipendGui.kingdomId());
            return;
        }

        UUID recipientId = stipendGui.memberIdForSlot(event.getRawSlot());
        if (recipientId == null) {
            return;
        }
        OfflinePlayer recipient = Bukkit.getOfflinePlayer(recipientId);
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.STIPEND_AMOUNT,
                stipendGui.kingdomId(),
                player.getUniqueId())
                .withStipendRecipient(recipientId, recipient.getName()));
        player.sendMessage(c("&bType the stipend amount for ") + recipient.getName() + " in chat (or 'cancel'):");
    }

    private void openStipendSelect(Player player, String kingdomId) {
        StipendSelectGui gui = StipendSelectGui.create(
                kingdomId, StipendSelectGui.onlineKingdomMembers(kingdomId, handler.kingdomService()));
        player.openInventory(gui.getInventory());
    }

    private void startStipendOtherPrompt(Player player, String kingdomId) {
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.STIPEND_PLAYER, kingdomId, player.getUniqueId()));
        player.sendMessage(c("&bType the recipient's player name in chat (or 'cancel'):"));
    }

    private void handleDivisionVoteClick(InventoryClickEvent event, Player player, DivisionVoteGui voteGui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ParliamentHubAction action = voteGui.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }

        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            player.closeInventory();
            return;
        }
        if (!handler.inCommons(player, membership.get().getKingdomId())) {
            player.sendMessage(handler.error("You must be in the House of Commons to vote."));
            return;
        }

        VoteChoice choice = switch (action) {
            case VOTE_AYE -> VoteChoice.AYE;
            case VOTE_NAY -> VoteChoice.NAY;
            case VOTE_ABSTAIN -> VoteChoice.ABSTAIN;
            default -> null;
        };
        if (choice == null) {
            return;
        }
        castMpVote(player, membership.get(), choice);
        player.closeInventory();
    }

    private void handleReferendumBallotClick(
            InventoryClickEvent event, Player player, ReferendumBallotGui ballotGui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ParliamentHubAction action = ballotGui.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }
        VoteChoice choice = switch (action) {
            case VOTE_AYE -> VoteChoice.AYE;
            case VOTE_NAY -> VoteChoice.NAY;
            case VOTE_ABSTAIN -> VoteChoice.ABSTAIN;
            default -> null;
        };
        if (choice == null) {
            return;
        }
        handler.finish(player, parliamentService.castBallot(ballotGui.kingdomId(), player.getUniqueId(), choice));
        player.closeInventory();
    }

    private void handleMintPrepareClick(InventoryClickEvent event, Player player, MintPrepareGui mintGui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            player.closeInventory();
            return;
        }

        MintPrepareGui.MintPrepareAction action = mintGui.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }

        switch (action) {
            case CANCEL -> {
                pendingMintLocations.remove(player.getUniqueId());
                player.closeInventory();
            }
            case REPLACE -> {
                pendingMintLocations.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(c("&bRight-click a lectern in your kingdom to choose a new location."));
            }
            case CONFIRM -> confirmMintPrepare(player, membership.get(), mintGui);
            default -> {
            }
        }
    }

    private void confirmMintPrepare(Player player, PlayerMembership membership, MintPrepareGui mintGui) {
        if (membership.getRank() != NobleRank.PREMIER) {
            player.sendMessage(handler.error("Only the Premier may prepare a mint location."));
            player.closeInventory();
            return;
        }

        MintLocation location = mintGui.preparedLocation()
                .or(() -> Optional.ofNullable(pendingMintLocations.get(player.getUniqueId())))
                .orElse(null);
        if (location == null) {
            player.sendMessage(handler.error("No lectern location selected."));
            player.closeInventory();
            return;
        }

        ParliamentResult result = parliamentService.prepareMint(membership.getKingdomId(), NobleRank.PREMIER, location);
        pendingMintLocations.remove(player.getUniqueId());
        handler.finish(player, result);
        player.closeInventory();
    }

    private void startFiscalPrompt(Player player, String kingdomId) {
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.FISCAL, kingdomId, player.getUniqueId()));
        player.sendMessage(
                c("&bType fiscal rates: ") + c("&fbase foreign transferFee crossFee interest tariff [title]") + c("&7 (or 'cancel')"));
    }

    private void startBudgetCustomPrompt(Player player, String kingdomId) {
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.BUDGET_CUSTOM, kingdomId, player.getUniqueId()));
        player.sendMessage(c("&bType the budget amount in chat (or 'cancel'):"));
    }

    private void tableBudget(Player player, PlayerMembership membership, double amount, String title) {
        ParliamentResult result = handler.tableBudget(
                membership.getKingdomId(), membership.getRank(), membership.getPlayerId(), amount, title);
        handler.finish(player, result);
        player.closeInventory();
    }

    private void tableMintBill(Player player, PlayerMembership membership) {
        ParliamentResult result = handler.tableSpendMint(
                membership.getKingdomId(), membership.getRank(), membership.getPlayerId(), null);
        handler.finish(player, result);
        player.closeInventory();
    }

    private void tablePublicWorkBill(Player player, PlayerMembership membership) {
        ParliamentResult result = handler.tableSpendPublicWork(
                membership.getKingdomId(), membership.getRank(), membership.getPlayerId(), null);
        handler.finish(player, result);
        player.closeInventory();
    }

    private void startWarTargetPrompt(Player player, String kingdomId) {
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.WAR_TARGET, kingdomId, player.getUniqueId()));
        player.sendMessage(c("&bType the target kingdom id in chat (or 'cancel'):"));
    }

    private void startTreatyPrompt(Player player, String kingdomId) {
        player.closeInventory();
        chatSessions.start(new ParliamentChatSessions.Session(
                ParliamentChatSessions.SessionType.TREATY_COUNTERPART, kingdomId, player.getUniqueId()));
        player.sendMessage(c("&bType counterpart kingdom id in chat (or 'cancel'):"));
    }

    private void tablePeaceBill(Player player, PlayerMembership membership) {
        handler.finish(player, handler.tablePeace(
                membership.getKingdomId(), membership.getRank(), membership.getPlayerId(), null));
        player.closeInventory();
    }

    private void handleWarPrepareClick(InventoryClickEvent event, Player player, WarPrepareGui gui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) { player.closeInventory(); return; }
        WarPrepareGui.Action action = gui.actionForSlot(event.getRawSlot());
        if (action == null) return;
        if (action == WarPrepareGui.Action.CANCEL) { player.closeInventory(); return; }
        if (action == WarPrepareGui.Action.CONFIRM) {
            handler.finish(player, handler.tableWar(
                    gui.kingdomId(), membership.get().getRank(), membership.get().getPlayerId(), gui.targetKingdomId(),
                    gui.aim(), gui.outcome(), gui.deadlineDays(), null));
            player.closeInventory();
            return;
        }
        WarAim aim = switch (action) {
            case TERRITORY -> WarAim.TERRITORY_THRESHOLD;
            case CAPITAL -> WarAim.CAPITAL_FALL;
            default -> gui.aim();
        };
        WarOutcome outcome = switch (action) {
            case ANNEXATION -> WarOutcome.ANNEXATION;
            case TRIBUTE -> WarOutcome.WAR_TRIBUTE;
            default -> gui.outcome();
        };
        int deadline = switch (action) {
            case DAY_ONE -> 1;
            case DAY_THREE -> 3;
            case DAY_SEVEN -> 7;
            default -> gui.deadlineDays();
        };
        player.openInventory(WarPrepareGui.create(
                        gui.kingdomId(), gui.targetKingdomId(), aim, outcome, deadline, gui.counterWar())
                .getInventory());
    }

    private void openPublicWorkPrepareFromLook(Player player, PlayerMembership membership) {
        if (membership.getRank() != NobleRank.PREMIER) {
            player.sendMessage(handler.error("Only the Premier may prepare a public work."));
            return;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(handler.error("Look at a block in your territory to prepare a public work."));
            return;
        }
        if (!handler.isLecternInTerritory(player, target, membership.getKingdomId())) {
            player.sendMessage(handler.error("Public work must be inside your kingdom's linked territory."));
            return;
        }
        player.closeInventory();
        PublicWorkPrepareGui gui = PublicWorkPrepareGui.create(
                membership.getKingdomId(),
                target.getWorld().getName(),
                target.getX(),
                target.getY(),
                target.getZ());
        player.openInventory(gui.getInventory());
    }

    private void handlePublicWorkPrepareClick(InventoryClickEvent event, Player player, PublicWorkPrepareGui gui) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            player.closeInventory();
            return;
        }
        if (gui.isCancelSlot(event.getRawSlot())) {
            player.closeInventory();
            return;
        }
        Optional<WealthBlockType> type = gui.typeForSlot(event.getRawSlot());
        if (type.isEmpty()) {
            return;
        }
        if (membership.get().getRank() != NobleRank.PREMIER) {
            player.sendMessage(handler.error("Only the Premier may prepare a public work."));
            player.closeInventory();
            return;
        }
        PreparedPublicWork work = new PreparedPublicWork(
                type.get(), gui.worldName(), gui.x(), gui.y(), gui.z());
        ParliamentResult result =
                parliamentService.preparePublicWork(membership.get().getKingdomId(), NobleRank.PREMIER, work);
        handler.finish(player, result);
        player.closeInventory();
    }

    private void openDivision(Player player, PlayerMembership membership) {
        ParliamentResult result = parliamentService.openDivision(membership.getKingdomId(), membership.getRank());
        if (result instanceof ParliamentResult.Success success) {
            player.sendMessage(handler.success(success.message()));
            handler.kingdomStore().saveFrom(handler.kingdomService());
            autoOpenDivisionForMps(membership.getKingdomId());
            handler.broadcastParliament(
                    membership.getKingdomId(), c("&eDivision open — MPs may vote."));
            openHubGui(player);
            return;
        }
        player.sendMessage(handler.error(((ParliamentResult.Failure) result).message()));
    }

    private void autoOpenDivisionForMps(String kingdomId) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            handler.kingdomService().getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (membership.getKingdomId().equals(kingdomId) && membership.getRank() == NobleRank.MP) {
                    if (handler.inCommons(online, kingdomId)) {
                        openDivisionVoteGui(online, kingdomId);
                    }
                }
            });
        }
    }

    private void closeDivision(Player player, ParliamentHubGui hub, PlayerMembership membership) {
        if (!hub.view().isEnabled(ParliamentHubAction.CLOSE_DIVISION)) {
            player.sendMessage(handler.error("Division is tied. Cast your vote before closing."));
            return;
        }
        handler.closeDivisionWithBroadcast(player, membership.getKingdomId(), membership.getRank());
        player.closeInventory();
    }

    private void castSpeakerVote(Player player, PlayerMembership membership, VoteChoice choice) {
        ParliamentResult result = parliamentService.castSpeakerVote(membership.getKingdomId(), membership.getRank(),
                choice);
        if (result instanceof ParliamentResult.Success) {
            handler.finish(player, result);
            openHubGui(player);
            return;
        }
        player.sendMessage(handler.error(((ParliamentResult.Failure) result).message()));
    }

    private void castMpVote(Player player, PlayerMembership membership, VoteChoice choice) {
        ParliamentResult result = parliamentService.castVote(
                membership.getKingdomId(), membership.getRank(), membership.getPlayerId(), choice);
        handler.finish(player, result);
    }

    private void grantAssent(Player player, PlayerMembership membership) {
        if (churchService != null) {
            Optional<String> uncrowned = churchService.ceremonialRefusal(
                    membership.getKingdomId(), membership.getPlayerId(), membership.getRank());
            if (uncrowned.isPresent()) {
                player.sendMessage(handler.error(uncrowned.get()));
                return;
            }
        }
        ParliamentResult result = parliamentService.assent(membership.getKingdomId(), membership.getRank());
        if (result instanceof ParliamentResult.Failure failure) {
            player.sendMessage(handler.error(failure.message()));
            return;
        }
        handler.enactAssentedBill(player, membership.getKingdomId());
        player.closeInventory();
    }

    private void withholdAssent(Player player, PlayerMembership membership) {
        handler.rejectWithBroadcast(player, membership.getKingdomId(), membership.getRank());
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatSessions.has(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            chatSessions.cancel(player.getUniqueId());
            player.sendMessage(c("&7Parliament input cancelled."));
            return;
        }

        ParliamentChatSessions.Session session = chatSessions.get(player.getUniqueId()).orElseThrow();
        Bukkit.getScheduler().runTask(handler.plugin(), () -> handleChatInput(player, session, message));
    }

    private void handleChatInput(Player player, ParliamentChatSessions.Session session, String message) {
        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            chatSessions.cancel(player.getUniqueId());
            return;
        }

        switch (session.type()) {
            case FISCAL -> handleFiscalChat(player, session, message, membership.get());
            case BUDGET_CUSTOM -> handleBudgetCustomChat(player, session, message, membership.get());
            case STIPEND_PLAYER -> handleStipendPlayerChat(player, session, message, membership.get());
            case STIPEND_AMOUNT -> handleStipendAmountChat(player, session, message, membership.get());
            case STIPEND_REASON -> handleStipendReasonChat(player, session, message, membership.get());
            case WAR_TARGET -> handleWarTargetChat(player, session, message);
            case TREATY_COUNTERPART -> handleTreatyCounterpartChat(player, session, message);
            case TREATY_KIND -> handleTreatyKindChat(player, session, message, membership.get());
        }
    }

    private void handleFiscalChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        String[] parts = message.split("\\s+");
        if (parts.length < 6) {
            player.sendMessage(handler.error("Need six numbers: base foreign transferFee crossFee interest tariff [title]"));
            return;
        }

        String title = null;
        int rateCount = parts.length;
        if (parts.length > 6) {
            try {
                Double.parseDouble(parts[parts.length - 1]);
            } catch (NumberFormatException ex) {
                String last = parts[parts.length - 1];
                title = last.startsWith("\"") && last.endsWith("\"") && last.length() > 1
                        ? last.substring(1, last.length() - 1)
                        : last;
                rateCount = parts.length - 1;
            }
        }
        if (rateCount < 6) {
            player.sendMessage(handler.error("Need six numbers: base foreign transferFee crossFee interest tariff [title]"));
            return;
        }

        int offset = rateCount - 6;
        try {
            FiscalRates proposed = new FiscalRates(
                    Double.parseDouble(parts[offset]),
                    Double.parseDouble(parts[offset + 1]),
                    Double.parseDouble(parts[offset + 2]),
                    Double.parseDouble(parts[offset + 3]),
                    Double.parseDouble(parts[offset + 4]),
                    Double.parseDouble(parts[offset + 5]),
                    FiscalRates.defaults().rankModifiers());
            ParliamentResult result = handler.tableFiscal(
                    session.kingdomId(),
                    membership.getRank(),
                    membership.getPlayerId(),
                    proposed,
                    title);
            chatSessions.cancel(player.getUniqueId());
            handler.finish(player, result);
        } catch (NumberFormatException ex) {
            player.sendMessage(handler.error("All fiscal rates must be numbers."));
        }
    }

    private void handleBudgetCustomChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        try {
            double amount = Double.parseDouble(message);
            chatSessions.cancel(player.getUniqueId());
            tableBudget(player, membership, amount, null);
        } catch (NumberFormatException ex) {
            player.sendMessage(handler.error("Budget amount must be a number."));
        }
    }

    private void handleStipendPlayerChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(message);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(handler.error("Unknown player: " + message));
            return;
        }
        chatSessions.advance(
                player.getUniqueId(),
                session.withStipendRecipient(target.getUniqueId(), target.getName())
                        .next(ParliamentChatSessions.SessionType.STIPEND_AMOUNT));
        player.sendMessage(c("&bType the stipend amount for ") + target.getName() + " in chat (or 'cancel'):");
    }

    private void handleStipendAmountChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        try {
            double amount = Double.parseDouble(message);
            if (amount <= 0) {
                player.sendMessage(handler.error("Spend amount must be positive."));
                return;
            }
            chatSessions.advance(
                    player.getUniqueId(),
                    session.withStipendAmount(amount).next(ParliamentChatSessions.SessionType.STIPEND_REASON));
            player.sendMessage(c("&bType a reason for the stipend (or 'skip' for none):"));
        } catch (NumberFormatException ex) {
            player.sendMessage(handler.error("Spend amount must be a number."));
        }
    }

    private void handleStipendReasonChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        String reason = message.equalsIgnoreCase("skip") ? null : message;
        UUID recipientId = session.stipendRecipientId();
        if (recipientId == null) {
            chatSessions.cancel(player.getUniqueId());
            player.sendMessage(handler.error("Stipend recipient missing."));
            return;
        }
        ParliamentResult result = handler.tableSpendStipend(
                session.kingdomId(),
                membership.getRank(),
                membership.getPlayerId(),
                recipientId,
                session.stipendAmount(),
                reason,
                session.optionalTitle());
        chatSessions.cancel(player.getUniqueId());
        handler.finish(player, result);
    }

    private void handleWarTargetChat(Player player, ParliamentChatSessions.Session session, String message) {
        chatSessions.cancel(player.getUniqueId());
        player.openInventory(WarPrepareGui.create(
                        session.kingdomId(),
                        message,
                        WarAim.TERRITORY_THRESHOLD,
                        WarOutcome.ANNEXATION,
                        3,
                        parliamentService.isCounterWarEligible(session.kingdomId(), message))
                .getInventory());
    }

    private void handleTreatyCounterpartChat(Player player, ParliamentChatSessions.Session session, String message) {
        chatSessions.advance(player.getUniqueId(), session.withTreatyCounterpart(message)
                .next(ParliamentChatSessions.SessionType.TREATY_KIND));
        player.sendMessage(c("&bType non-aggression or trade-pact, then optional repeal (or 'cancel'):"));
    }

    private void handleTreatyKindChat(
            Player player, ParliamentChatSessions.Session session, String message, PlayerMembership membership) {
        String[] parts = message.split("\\s+");
        TreatyKind kind = parts.length == 0 ? null : switch (parts[0].toLowerCase(java.util.Locale.ROOT)) {
            case "non-aggression" -> TreatyKind.NON_AGGRESSION;
            case "trade-pact" -> TreatyKind.TRADE_PACT;
            default -> null;
        };
        if (kind == null) {
            player.sendMessage(handler.error("Treaty kind must be non-aggression or trade-pact."));
            return;
        }
        chatSessions.cancel(player.getUniqueId());
        handler.finish(player, handler.tableTreaty(session.kingdomId(), membership.getRank(), membership.getPlayerId(),
                session.treatyCounterpartId(), kind, parts.length > 1 && parts[1].equalsIgnoreCase("repeal"), null));
    }

    /** Members are reminded of an open referendum as they log in, wherever they happen to be. */
    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Optional<PlayerMembership> membership =
                handler.kingdomService().getMembership(event.getPlayer().getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!parliamentService.isPollingOpen(kingdomId)) {
            return;
        }
        Optional<String> question = parliamentService.referendumQuestion(kingdomId);
        if (question.isEmpty()) {
            return;
        }
        handler.promptIfEntitled(event.getPlayer(), kingdomId, question.get());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatSessions.cancel(event.getPlayer().getUniqueId());
        pendingMintLocations.remove(event.getPlayer().getUniqueId());
    }
}
