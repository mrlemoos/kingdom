package dev.mrlemoos.kingdom.treaty.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.city.gui.PermitRegisterLayout;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.treaty.TreatyRegister.Row;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The paginated roll of a realm's treaties. A view: the Crown's click only tables a bill. */
public final class TreatyRegisterGui implements InventoryHolder {

    public static final Component TITLE = component("&6Treaty Register");

    private final String kingdomId;
    private final int page;
    private final List<Row> pageRows;
    private Inventory inventory;

    private TreatyRegisterGui(String kingdomId, int page, List<Row> pageRows) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
        this.pageRows = List.copyOf(pageRows);
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public static TreatyRegisterGui create(
            String kingdomId, List<Row> rows, int requestedPage, boolean crown, Function<String, String> names) {
        int total = rows.size();
        int page = PermitRegisterLayout.clampPage(requestedPage, total);
        List<Row> slice = PermitRegisterLayout.pageSlice(rows, page);
        TreatyRegisterGui gui = new TreatyRegisterGui(kingdomId, page, slice);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        int slot = 0;
        for (Row row : slice) {
            inventory.setItem(slot++, item(row, crown, names.apply(row.counterpartId())));
        }
        if (PermitRegisterLayout.hasPrevious(page)) {
            inventory.setItem(PermitRegisterLayout.SLOT_PREVIOUS,
                    ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (PermitRegisterLayout.hasNext(page, total)) {
            inventory.setItem(PermitRegisterLayout.SLOT_NEXT,
                    ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(PermitRegisterLayout.SLOT_PAGE, ItemBuilder.labelled(
                Material.BOOK,
                c("&6Page " + (page + 1) + " of " + PermitRegisterLayout.pageCount(total)),
                total + " treat" + (total == 1 ? "y" : "ies")));
        fill(inventory, PermitRegisterLayout.PAGE_SIZE);
        return gui;
    }

    /** The row occupying this slot on the page shown, or null. */
    public Row rowForSlot(int slot) {
        if (!PermitRegisterLayout.isHeadSlot(slot) || slot >= pageRows.size()) {
            return null;
        }
        return pageRows.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    static String kindLabel(TreatyKind kind) {
        return kind == TreatyKind.TRADE_PACT ? "Trade pact" : "Non-aggression treaty";
    }

    static ItemStack item(Row row, boolean crown, String counterpart) {
        ItemBuilder builder = new ItemBuilder(row.kind() == TreatyKind.TRADE_PACT ? Material.EMERALD : Material.SHIELD)
                .displayAs(c("&f" + kindLabel(row.kind()) + " with " + counterpart));
        String lapses = "Lapses in " + row.daysLeft() + " in-game day" + (row.daysLeft() == 1 ? "" : "s");
        switch (row.status()) {
            case ACTIVE -> builder.lore(c("&aIn force"));
            case AWAITING_US -> builder.lore(c("&e" + counterpart + " proposes; our Crown has not assented"))
                    .lore(c("&7" + lapses));
            case AWAITING_THEM -> builder.lore(c("&7Awaiting the Crown of " + counterpart)).lore(c("&7" + lapses));
            case REPEAL_AWAITING_US -> builder.lore(c("&e" + counterpart + " seeks repeal")).lore(c("&7" + lapses));
            case REPEAL_AWAITING_THEM -> builder.lore(c("&7Repeal awaits the Crown of " + counterpart))
                    .lore(c("&7" + lapses));
        }
        if (crown && row.crownMayAnswer()) {
            builder.lore(c(row.answerIsRepeal()
                    ? "&6Click to table a repeal bill"
                    : "&6Click to table this treaty in the Commons"));
        }
        return builder.build();
    }

    private static void fill(Inventory inventory, int from) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = from; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    /** Table or step back: the Crown's second thought before a treaty bill goes on the order paper. */
    public static final class Confirm implements InventoryHolder {

        public static final Component TITLE = component("&6Table treaty bill");

        private final String kingdomId;
        private final Row row;
        private final int registerPage;
        private Inventory inventory;

        private Confirm(String kingdomId, Row row, int registerPage) {
            this.kingdomId = kingdomId;
            this.row = row;
            this.registerPage = registerPage;
        }

        public static Confirm create(String kingdomId, Row row, int registerPage, String counterpart) {
            Confirm gui = new Confirm(kingdomId, row, registerPage);
            gui.inventory = Bukkit.createInventory(gui, 27, TITLE);
            gui.inventory.setItem(PermitRegisterLayout.SLOT_CONFIRM_REVOKE, ItemBuilder.labelled(
                    Material.LIME_WOOL,
                    c(row.answerIsRepeal() ? "&aTable repeal bill" : "&aTable treaty bill"),
                    "The Commons divides; the Crown then assents"));
            gui.inventory.setItem(PermitRegisterLayout.SLOT_CONFIRM_HOLDER, item(row, false, counterpart));
            gui.inventory.setItem(PermitRegisterLayout.SLOT_CONFIRM_BACK,
                    ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the register"));
            fill(gui.inventory, 0);
            return gui;
        }

        public String kingdomId() {
            return kingdomId;
        }

        public Row row() {
            return row;
        }

        public int registerPage() {
            return registerPage;
        }

        public boolean isTableSlot(int slot) {
            return slot == PermitRegisterLayout.SLOT_CONFIRM_REVOKE;
        }

        public boolean isBackSlot(int slot) {
            return slot == PermitRegisterLayout.SLOT_CONFIRM_BACK;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
