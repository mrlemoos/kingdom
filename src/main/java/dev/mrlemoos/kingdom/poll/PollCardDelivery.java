package dev.mrlemoos.kingdom.poll;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Hands out poll cards, after the resignation-letter pattern: to every member online when a poll
 * opens, and again on joining while it stays open. A member already holding the card, or who has
 * already voted, is handed nothing.
 */
public final class PollCardDelivery {

    private final KingdomService kingdomService;
    private final PollCardItem cardItem;
    private final Set<String> announced = new HashSet<>();
    private boolean seeded;

    public PollCardDelivery(KingdomService kingdomService, PollCardItem cardItem) {
        this.kingdomService = kingdomService;
        this.cardItem = cardItem;
    }

    public PollCardItem cardItem() {
        return cardItem;
    }

    /**
     * Notices polls that have opened since the last sweep, titles their opening, and hands every
     * owed member online a card. Polls open when the sweep first runs are not titled again.
     */
    public void sweep() {
        Set<String> open = new HashSet<>();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            for (PollCardRules.OpenPoll poll : PollCardRules.openPolls(kingdom)) {
                open.add(poll.pollId());
                if (announced.contains(poll.pollId())) {
                    continue;
                }
                announced.add(poll.pollId());
                List<Player> handed = deliverToRealm(kingdom, poll);
                if (seeded) {
                    announce(kingdom, poll, handed);
                }
            }
        }
        announced.retainAll(open);
        seeded = true;
    }

    /** Hands a member every card they are owed and do not yet hold. */
    public void deliverOwed(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(membership.get().getKingdomId());
        if (kingdom.isEmpty()) {
            return;
        }
        for (PollCardRules.OpenPoll poll : PollCardRules.openPolls(kingdom.get())) {
            deliverIfOwed(player, membership.get(), kingdom.get(), poll);
        }
    }

    /** Takes back every card the member holds for that poll: spent, or stale. */
    public void consume(Player player, String pollId) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (cardItem.pollId(contents[slot]).filter(pollId::equals).isPresent()) {
                inventory.setItem(slot, null);
            }
        }
    }

    /** Takes back the member's card for the realm's open poll of that kind, once they have voted in it. */
    public void spend(Player player, String kingdomId, PollCardRules.PollKind kind) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        for (PollCardRules.OpenPoll poll : PollCardRules.openPolls(kingdom.get())) {
            if (poll.kind() == kind) {
                consume(player, poll.pollId());
            }
        }
    }

    public boolean holds(Player player, String pollId) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (cardItem.pollId(stack).filter(pollId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private List<Player> deliverToRealm(Kingdom kingdom, PollCardRules.OpenPoll poll) {
        List<Player> handed = new ArrayList<>();
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdom.getId())) {
            Optional<PlayerMembership> membership = kingdomService.getMembership(member.getUniqueId());
            if (membership.isPresent() && deliverIfOwed(member, membership.get(), kingdom, poll)) {
                handed.add(member);
            }
        }
        return handed;
    }

    private boolean deliverIfOwed(
            Player player, PlayerMembership membership, Kingdom kingdom, PollCardRules.OpenPoll poll) {
        if (!PollCardRules.owed(kingdom, membership, poll) || holds(player, poll.pollId())) {
            return false;
        }
        ItemStack card = cardItem.create(kingdom.getId(), poll.pollId(), poll.title());
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(card);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(c("&eYour inventory was full. The poll card was dropped at your feet."));
        }
        player.sendMessage(c("&b[Poll] &fYou are handed a poll card: &e" + poll.title() + "&f."));
        RealmFeedback.instruct(player, "Right-click your poll card to stand or vote.");
        return true;
    }

    /**
     * The opening of polls is titled to the whole realm; a Premier election and the Speaker's casting
     * vote are House business, titled only to those handed a card.
     */
    private void announce(Kingdom kingdom, PollCardRules.OpenPoll poll, List<Player> handed) {
        switch (poll.kind()) {
            case SPEAKER_TIE -> RealmFeedback.milestone(handed, "&bThe count is tied", "&7The Speaker holds the casting vote");
            case REFERENDUM -> RealmFeedback.realmMilestone(
                    kingdomService, kingdom.getId(), "&bPolls are open", "&7" + poll.title());
            case ELECTION -> {
                boolean premier = kingdom.getElectionState().election().type()
                        .filter(type -> type == ElectionType.PREMIER)
                        .isPresent();
                if (premier) {
                    RealmFeedback.milestone(handed, "&bPolls are open", "&7" + poll.title());
                } else {
                    RealmFeedback.realmMilestone(
                            kingdomService, kingdom.getId(), "&bPolls are open", "&7" + poll.title());
                }
            }
        }
    }
}
