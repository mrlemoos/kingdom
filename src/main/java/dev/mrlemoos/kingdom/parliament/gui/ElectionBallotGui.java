package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.poll.PollCardOffer;
import dev.mrlemoos.kingdom.poll.PollCardRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The window a <b>poll card</b> opens during an election: standing as a candidate, the ballot of
 * candidates' heads, or — after a tied count — the Speaker's casting vote among the tied.
 */
public final class ElectionBallotGui implements InventoryHolder {

    public static final Component TITLE = component("&bPoll Card");

    static final int SLOT_INFO = 4;
    static final int FIRST_CANDIDATE_SLOT = 9;
    static final int MAX_CANDIDATES = 36;
    /** Stands as an independent, declaring nothing. */
    static final int SLOT_STAND = 48;
    /** Stands with a party, colour and manifesto typed in chat. */
    static final int SLOT_STAND_DECLARED = 50;

    private final String kingdomId;
    private final String pollId;
    private final PollCardOffer offer;
    private final List<UUID> ballot;
    private Inventory inventory;

    public ElectionBallotGui(String kingdomId, String pollId, PollCardOffer offer) {
        this.kingdomId = kingdomId;
        this.pollId = pollId;
        this.offer = offer;
        List<UUID> listed = offer.mayVote() ? offer.candidates() : List.of();
        this.ballot = listed.size() > MAX_CANDIDATES ? List.copyOf(listed.subList(0, MAX_CANDIDATES)) : listed;
    }

    /**
     * @param candidateLabels each candidate's name and the party they declared, for the ballot heads
     * @param who who may stand and vote in this poll
     */
    public static ElectionBallotGui create(
            String kingdomId,
            String pollId,
            String pollTitle,
            PollCardOffer offer,
            Map<UUID, String> candidateLabels,
            String who) {
        ElectionBallotGui gui = new ElectionBallotGui(kingdomId, pollId, offer);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        gui.populate(pollTitle, candidateLabels, who);
        return gui;
    }

    private void populate(String pollTitle, Map<UUID, String> candidateLabels, String who) {
        inventory.clear();
        inventory.setItem(SLOT_INFO, infoItem(pollTitle, who));
        for (int i = 0; i < ballot.size(); i++) {
            UUID candidate = ballot.get(i);
            String label = candidateLabels.getOrDefault(candidate, "A candidate");
            inventory.setItem(FIRST_CANDIDATE_SLOT + i, new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(candidate)
                    .displayAs(c("&f" + label))
                    .lore(c(speakerVote() ? "&eClick to cast the Speaker's vote" : "&eClick to vote for this candidate"))
                    .build());
        }
        if (offer.mayStand()) {
            inventory.setItem(SLOT_STAND, ItemBuilder.labelled(
                    Material.BOOK, c("&aStand as independent"), "Put your name on the ballot, declaring no party"));
            inventory.setItem(SLOT_STAND_DECLARED, ItemBuilder.labelled(
                    Material.WRITABLE_BOOK,
                    c("&aStand with a declaration"),
                    "Type your party, its colour and a manifesto in chat"));
        } else if (offer.standing()) {
            inventory.setItem(SLOT_STAND + 1, ItemBuilder.labelled(
                    Material.BOOK, c("&7You are standing"), "Your name is on the ballot"));
        }
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack infoItem(String pollTitle, String who) {
        List<String> lore = new ArrayList<>();
        if (speakerVote()) {
            lore.add(c("&7The count is tied for the last seat."));
            lore.add(c("&7Click a head to break the tie."));
            lore.add(c("&8The Speaker alone may."));
        } else {
            lore.add(c(ballot.isEmpty() ? "&7No candidate has stood yet." : "&7Click a head to cast your vote."));
            lore.add(c(offer.mayStand() ? "&7Or stand for election below." : "&7Your card is spent once you vote."));
            lore.add(c("&8" + who));
        }
        return new ItemBuilder(Material.PAPER)
                .displayAs(c("&6" + pollTitle))
                .lore(lore.toArray(new String[0]))
                .build();
    }

    public String kingdomId() {
        return kingdomId;
    }

    public String pollId() {
        return pollId;
    }

    public boolean speakerVote() {
        return offer.kind() == PollCardRules.PollKind.SPEAKER_TIE;
    }

    public Optional<UUID> candidateForSlot(int slot) {
        int index = slot - FIRST_CANDIDATE_SLOT;
        if (index < 0 || index >= ballot.size()) {
            return Optional.empty();
        }
        return Optional.of(ballot.get(index));
    }

    public boolean isStandSlot(int slot) {
        return slot == SLOT_STAND && offer.mayStand();
    }

    public boolean isStandDeclaredSlot(int slot) {
        return slot == SLOT_STAND_DECLARED && offer.mayStand();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
