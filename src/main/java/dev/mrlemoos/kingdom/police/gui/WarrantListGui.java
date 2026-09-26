package dev.mrlemoos.kingdom.police.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.AmountPickGui;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.police.ArrestReward;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.police.WarrantRegister;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * A page of the realm's active warrants, one head per suspect. Read at the court to post an arrest
 * reward, and by the Crown from the Realm Hub as the warrant register, to cancel one.
 */
public final class WarrantListGui implements InventoryHolder {

    /** What a click on a warrant does. */
    public enum Purpose {
        /** Post or top up an arrest reward on it, at the court. */
        REWARD,
        /** Cancel it, behind a confirmation: the Crown's warrant register. */
        CANCEL
    }

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private final Purpose purpose;
    private final String kingdomId;
    private final int page;
    private final boolean hasNext;
    private final Map<Integer, String> warrantIds;
    private Inventory inventory;

    private WarrantListGui(
            Purpose purpose, String kingdomId, int page, boolean hasNext, Map<Integer, String> warrantIds) {
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
        this.hasNext = hasNext;
        this.warrantIds = Map.copyOf(warrantIds);
    }

    public static WarrantListGui create(Purpose purpose, String kingdomId, List<Warrant> active, int requestedPage) {
        int page = WarrantRegister.clampPage(requestedPage, active.size());
        List<Warrant> shown = WarrantRegister.page(active, page);
        Map<Integer, String> ids = new HashMap<>();
        for (int i = 0; i < shown.size(); i++) {
            ids.put(i, shown.get(i).id());
        }
        WarrantListGui gui = new WarrantListGui(
                purpose, kingdomId, page, page < WarrantRegister.pageCount(active.size()) - 1, ids);
        String title = purpose == Purpose.CANCEL ? "&4The Warrant Register" : "&6Post an Arrest Reward";
        Inventory inventory = Bukkit.createInventory(gui, 54, component(title));
        gui.inventory = inventory;
        for (int i = 0; i < shown.size(); i++) {
            inventory.setItem(i, head(shown.get(i), purpose));
        }
        if (shown.isEmpty()) {
            inventory.setItem(22, ItemBuilder.labelled(
                    Material.PAPER, c("&7No warrant is out"), "The realm pursues no one."));
        }
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (gui.hasNext) {
            inventory.setItem(SLOT_NEXT, ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        if (purpose == Purpose.CANCEL) {
            inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.OAK_DOOR, c("&eBack"), "To the Police"));
        }
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = WarrantRegister.PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    private static ItemStack head(Warrant warrant, Purpose purpose) {
        Optional<ArrestReward> reward = warrant.arrestReward();
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(warrant.suspectId())
                .displayAs(c("&c" + suspectName(warrant)))
                .lore(c("&7Charge: &f" + warrant.provisionKind().name().toLowerCase(Locale.UK).replace('_', ' ')))
                .lore(c("&7Arrest reward: &f" + (reward.isPresent()
                        ? AmountPickGui.formatCorona(reward.get().amount()) + " Corona"
                        : "none")));
        builder.lore(c(purpose == Purpose.CANCEL
                ? "&eClick to cancel this warrant"
                : "&eClick to post or top up a reward"));
        return builder.build();
    }

    public static String suspectName(Warrant warrant) {
        OfflinePlayer suspect = Bukkit.getOfflinePlayer(warrant.suspectId());
        String name = suspect.getName();
        return name == null || name.isBlank() ? "A suspect of the realm" : name;
    }

    public Purpose purpose() {
        return purpose;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return hasNext;
    }

    /** The warrant on this slot, or null. */
    public String warrantForSlot(int slot) {
        return warrantIds.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
