package dev.mrlemoos.kingdom.church.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Yes or no at the church: asking for a divorce, annulling a marriage, or answering another's
 * request for consent to a marriage or divorce.
 */
public final class RiteConfirmGui implements InventoryHolder {

    public enum Purpose {
        DIVORCE("&6Ask for a divorce?", "&aAsk", "&cNot now"),
        ANNUL("&6Annul this marriage?", "&aAnnul", "&cLeave it stand"),
        CONSENT("&6Your consent is asked", "&aAccept", "&cRefuse");

        private final String title;
        private final String yes;
        private final String no;

        Purpose(String title, String yes, String no) {
            this.title = title;
            this.yes = yes;
            this.no = no;
        }
    }

    public static final int SLOT_YES = 11;
    public static final int SLOT_SUBJECT = 13;
    public static final int SLOT_NO = 15;

    private final Purpose purpose;
    private final String kingdomId;
    private final UUID subject;
    private Inventory inventory;

    private RiteConfirmGui(Purpose purpose, String kingdomId, UUID subject) {
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    /**
     * @param subject whom the question is about: the spouse, the annulled party, or the one asking
     * @param lines what is being asked, one line each
     */
    public static RiteConfirmGui create(Purpose purpose, String kingdomId, UUID subject, List<String> lines) {
        RiteConfirmGui gui = new RiteConfirmGui(purpose, kingdomId, subject);
        Inventory inventory = Bukkit.createInventory(gui, 27, component(purpose.title));
        gui.inventory = inventory;
        ItemBuilder about = new ItemBuilder(Material.PLAYER_HEAD).skullOwner(subject).displayAs(c(purpose.title));
        for (String line : lines) {
            about.lore(c("&7" + line));
        }
        inventory.setItem(SLOT_SUBJECT, about.build());
        inventory.setItem(SLOT_YES, new ItemBuilder(Material.LIME_WOOL).displayAs(c(purpose.yes)).build());
        inventory.setItem(SLOT_NO, new ItemBuilder(Material.RED_WOOL).displayAs(c(purpose.no)).build());
        RitesGui.fillBackground(inventory);
        return gui;
    }

    public Purpose purpose() {
        return purpose;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID subject() {
        return subject;
    }

    public boolean isYesSlot(int slot) {
        return slot == SLOT_YES;
    }

    public boolean isNoSlot(int slot) {
        return slot == SLOT_NO;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
