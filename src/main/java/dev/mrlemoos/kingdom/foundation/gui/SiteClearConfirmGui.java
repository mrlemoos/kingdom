package dev.mrlemoos.kingdom.foundation.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Clear or step back: the Crown's second thought before a site is cleared from the Realm Hub. */
public final class SiteClearConfirmGui implements InventoryHolder {

    public static final Component TITLE = component("&4Clear site");
    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_SITE = 13;
    public static final int SLOT_BACK = 15;

    private final FoundationStone kind;
    private final String kingdomId;
    private final MintLocation mint;
    private final int number;
    private Inventory inventory;

    private SiteClearConfirmGui(FoundationStone kind, String kingdomId, MintLocation mint, int number) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.mint = mint;
        this.number = number;
    }

    public FoundationStone kind() {
        return kind;
    }

    public String kingdomId() {
        return kingdomId;
    }

    /** The mint to take down, when the site is one of several mints. */
    public Optional<MintLocation> mint() {
        return Optional.ofNullable(mint);
    }

    /** The MP seat or cell to clear, when the site is one of a numbered run. */
    public OptionalInt number() {
        return number > 0 ? OptionalInt.of(number) : OptionalInt.empty();
    }

    public static SiteClearConfirmGui create(FoundationStone kind, String kingdomId) {
        return open(kind, kingdomId, null, 0, "What stands there is taken down.");
    }

    public static SiteClearConfirmGui forSeat(String kingdomId, int seat) {
        return open(
                FoundationStone.MP_SEAT,
                kingdomId,
                null,
                seat,
                "MP seat " + seat + " is cleared; the next seat stone laid sets it again.");
    }

    public static SiteClearConfirmGui forCell(String kingdomId, int cell) {
        return open(
                FoundationStone.CELL,
                kingdomId,
                null,
                cell,
                "Cell " + cell + " is cleared; the next cell stone laid sets it again.");
    }

    /** The confirmation for one of a numbered run: an MP seat or a cell. */
    public static SiteClearConfirmGui forNumber(FoundationStone kind, String kingdomId, int number) {
        return kind == FoundationStone.CELL ? forCell(kingdomId, number) : forSeat(kingdomId, number);
    }

    public static SiteClearConfirmGui forMint(String kingdomId, MintLocation mint) {
        return open(
                FoundationStone.MINT,
                kingdomId,
                Objects.requireNonNull(mint, "mint"),
                0,
                "The mint at " + mint.worldName() + " " + mint.x() + ", " + mint.y() + ", " + mint.z()
                        + " and its Lord of the Treasury are taken down.");
    }

    private static SiteClearConfirmGui open(
            FoundationStone kind, String kingdomId, MintLocation mint, int number, String what) {
        SiteClearConfirmGui gui = new SiteClearConfirmGui(kind, kingdomId, mint, number);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(
                SLOT_CONFIRM, ItemBuilder.labelled(Material.RED_WOOL, c("&cClear site"), "Clear " + kind.site()));
        inventory.setItem(SLOT_SITE, ItemBuilder.labelled(Material.CHISELED_STONE_BRICKS, c("&6" + kind.title()), what));
        inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the Hub"));
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
