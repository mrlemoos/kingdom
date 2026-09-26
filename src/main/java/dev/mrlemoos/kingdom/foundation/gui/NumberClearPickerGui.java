package dev.mrlemoos.kingdom.foundation.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Which numbered place to clear — an MP seat or a cell: the Crown picks one of those set before the
 * confirmation. Numbers sit from the first slot, lowest first; beyond what the window holds, the
 * highest are left for the operators' command.
 */
public final class NumberClearPickerGui implements InventoryHolder {

    private static final int SMALL = 27;
    private static final int LARGE = 54;

    private final FoundationStone kind;
    private final String kingdomId;
    private final List<Integer> numbers;
    private Inventory inventory;

    private NumberClearPickerGui(FoundationStone kind, String kingdomId, List<Integer> numbers) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.numbers = List.copyOf(numbers.subList(0, Math.min(numbers.size(), LARGE - 9)));
    }

    public FoundationStone kind() {
        return kind;
    }

    public String kingdomId() {
        return kingdomId;
    }

    /** The Back button: the middle of the bottom row. */
    public int backSlot() {
        return inventory.getSize() - 5;
    }

    /** The number shown in a slot, if any. */
    public OptionalInt numberAt(int slot) {
        if (slot < 0 || slot >= numbers.size()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(numbers.get(slot));
    }

    /** @param numbers the places set, lowest first */
    public static NumberClearPickerGui create(FoundationStone kind, String kingdomId, List<Integer> numbers) {
        NumberClearPickerGui gui = new NumberClearPickerGui(kind, kingdomId, numbers);
        int size = gui.numbers.size() <= SMALL - 9 ? SMALL : LARGE;
        Inventory inventory = Bukkit.createInventory(
                gui, size, component(kind == FoundationStone.CELL ? "&4Clear which cell?" : "&4Clear which seat?"));
        gui.inventory = inventory;
        for (int slot = 0; slot < gui.numbers.size(); slot++) {
            inventory.setItem(slot, gui.icon(gui.numbers.get(slot)));
        }
        inventory.setItem(gui.backSlot(), ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the Hub"));
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    private ItemStack icon(int number) {
        return kind == FoundationStone.CELL
                ? ItemBuilder.labelled(Material.IRON_BARS, c("&6Cell " + number), "Click to clear this cell")
                : ItemBuilder.labelled(Material.SPRUCE_STAIRS, c("&6MP seat " + number), "Click to clear this seat");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
