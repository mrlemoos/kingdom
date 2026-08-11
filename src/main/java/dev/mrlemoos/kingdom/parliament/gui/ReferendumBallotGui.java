package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The ballot every member of the realm may cast in a <b>referendum</b>. Shaped like the division
 * ballot, but open to the whole membership and from anywhere—no chamber is required.
 */
public final class ReferendumBallotGui implements InventoryHolder {

    public static final String TITLE = c("&2Referendum");

    static final int SLOT_AYE = 11;
    static final int SLOT_QUESTION = 13;
    static final int SLOT_NAY = 15;
    static final int SLOT_ABSTAIN = 22;

    private final String kingdomId;
    private final String question;
    private Inventory inventory;

    public ReferendumBallotGui(String kingdomId, String question) {
        this.kingdomId = kingdomId;
        this.question = question;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public String question() {
        return question;
    }

    public static ReferendumBallotGui create(String kingdomId, String question, int votesCast, int electorate) {
        ReferendumBallotGui gui = new ReferendumBallotGui(kingdomId, question);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, question, votesCast, electorate);
        return gui;
    }

    static void populate(Inventory inventory, String question, int votesCast, int electorate) {
        inventory.clear();
        inventory.setItem(SLOT_AYE, ItemBuilder.labelled(Material.LIME_CONCRETE, c("&aAye"), "Answer aye"));
        inventory.setItem(SLOT_NAY, ItemBuilder.labelled(Material.RED_CONCRETE, c("&cNay"), "Answer nay"));
        inventory.setItem(
                SLOT_ABSTAIN, ItemBuilder.labelled(Material.YELLOW_CONCRETE, c("&eAbstain"), "Record no answer"));
        inventory.setItem(SLOT_QUESTION, questionItem(question, votesCast, electorate));
        fillBackground(inventory);
    }

    public ParliamentHubAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_AYE -> ParliamentHubAction.VOTE_AYE;
            case SLOT_NAY -> ParliamentHubAction.VOTE_NAY;
            case SLOT_ABSTAIN -> ParliamentHubAction.VOTE_ABSTAIN;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack questionItem(String question, int votesCast, int electorate) {
        List<String> lore = new ArrayList<>();
        lore.add(c("&7Put to every member of the realm"));
        lore.add(c("&7Turnout so far: &f" + votesCast + c("&7 of &f") + electorate));
        lore.add(c("&8The result is advisory."));
        return new ItemBuilder(Material.PAPER)
                .displayAs(c("&6" + question))
                .lore(lore.toArray(new String[0]))
                .build();
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
