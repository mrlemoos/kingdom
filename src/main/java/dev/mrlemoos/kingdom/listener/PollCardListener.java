package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.election.ElectionState;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.parliament.gui.ElectionBallotGui;
import dev.mrlemoos.kingdom.poll.PollCardDelivery;
import dev.mrlemoos.kingdom.poll.PollCardItem;
import dev.mrlemoos.kingdom.poll.PollCardOffer;
import dev.mrlemoos.kingdom.poll.PollCardRules;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The <b>poll card</b> in use: handed over again on joining while polls stay open; right-clicked, it
 * opens the election window or the referendum ballot; spent once its holder has voted.
 */
public final class PollCardListener implements Listener {

    private final KingdomService kingdomService;
    private final PollCardDelivery delivery;
    private final PollCardItem cardItem;
    private final ElectionHandler electionHandler;
    private final BiConsumer<Player, String> referendumBallotOpener;
    private final Consumer<Player> declarationPrompt;

    /**
     * @param declarationPrompt asks the player to type their party declaration in chat, then stands them
     */
    public PollCardListener(
            KingdomService kingdomService,
            PollCardDelivery delivery,
            ElectionHandler electionHandler,
            BiConsumer<Player, String> referendumBallotOpener,
            Consumer<Player> declarationPrompt) {
        this.kingdomService = kingdomService;
        this.delivery = delivery;
        this.cardItem = delivery.cardItem();
        this.electionHandler = electionHandler;
        this.referendumBallotOpener = referendumBallotOpener;
        this.declarationPrompt = declarationPrompt;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        delivery.deliverOwed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.getItem();
        Optional<String> pollId = cardItem.pollId(held);
        if (pollId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<String> kingdomId = cardItem.kingdomId(held);
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (kingdomId.isEmpty()
                || membership.isEmpty()
                || !kingdomId.get().equals(membership.get().getKingdomId())) {
            RealmFeedback.refuse(player, "That poll card is not for your realm.");
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId.get());
        if (kingdom.isEmpty()) {
            delivery.consume(player, pollId.get());
            return;
        }
        PollCardOffer offer = PollCardRules.offer(kingdom.get(), membership.get(), pollId.get());
        if (offer.stale()) {
            delivery.consume(player, pollId.get());
            RealmFeedback.refuse(player, "Those polls have closed. The card is spent.");
            return;
        }
        if (offer.voted()) {
            delivery.consume(player, pollId.get());
            RealmFeedback.refuse(player, "You have already voted. The card is spent.");
            return;
        }
        if (!offer.hasBusiness()) {
            delivery.consume(player, pollId.get());
            RealmFeedback.refuse(player, "This poll asks nothing of you.");
            return;
        }
        if (offer.kind() == PollCardRules.PollKind.REFERENDUM) {
            referendumBallotOpener.accept(player, kingdomId.get());
            return;
        }
        openBallot(player, kingdom.get(), pollId.get(), offer);
    }

    private void openBallot(Player player, Kingdom kingdom, String pollId, PollCardOffer offer) {
        Optional<PollCardRules.OpenPoll> poll = PollCardRules.openPoll(kingdom, pollId);
        String title = poll.isPresent() ? poll.get().title() : "Election";
        ElectionState election = kingdom.getElectionState().election();
        Map<UUID, String> labels = new LinkedHashMap<>();
        for (UUID candidate : offer.candidates()) {
            String name = Bukkit.getOfflinePlayer(candidate).getName();
            CandidateDeclaration declared = election.declaration(candidate);
            labels.put(candidate, (name == null ? "Unknown" : name) + c(" &7(" + declared.partyLabel() + ")"));
        }
        boolean premier = election.type().filter(type -> type == ElectionType.PREMIER).isPresent();
        String who = premier ? "Seated MPs may stand and vote." : "Members may vote; untitled citizens may stand.";
        player.openInventory(ElectionBallotGui.create(kingdom.getId(), pollId, title, offer, labels, who).getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBallotClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof ElectionBallotGui ballot)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        if (ballot.isStandSlot(slot)) {
            ElectionResult result = electionHandler.standForElection(player, CandidateDeclaration.blank());
            if (answered(player, result)) {
                player.sendMessage(c("&a" + result.message()));
                RealmFeedback.pollAnswered(player, "Your name is on the ballot");
                player.closeInventory();
            }
            return;
        }
        if (ballot.isStandDeclaredSlot(slot)) {
            player.closeInventory();
            declarationPrompt.accept(player);
            return;
        }
        Optional<UUID> candidate = ballot.candidateForSlot(slot);
        if (candidate.isEmpty()) {
            return;
        }
        ElectionResult result = ballot.speakerVote()
                ? electionHandler.castSpeakerVote(player, candidate.get())
                : electionHandler.castVote(player, candidate.get());
        if (answered(player, result)) {
            delivery.consume(player, ballot.pollId());
            player.sendMessage(c("&a" + result.message()));
            RealmFeedback.pollAnswered(player, "Your vote is cast");
            player.closeInventory();
        }
    }

    private static boolean answered(Player player, ElectionResult result) {
        if (result instanceof ElectionResult.Success) {
            return true;
        }
        RealmFeedback.refuse(player, result.message());
        return false;
    }
}
