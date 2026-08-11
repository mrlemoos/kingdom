package dev.mrlemoos.kingdom.police.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Secret trial-jury ballot: Guilty / Not guilty only. */
public final class TrialJuryBallotGui implements InventoryHolder {

    public static final String TITLE = c("&4Trial Jury");

    private final String kingdomId;
    private final UUID accusedId;
    private final String accusedName;
    private Inventory inventory;

    public TrialJuryBallotGui(String kingdomId, UUID accusedId, String accusedName) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.accusedId = Objects.requireNonNull(accusedId, "accusedId");
        this.accusedName = Objects.requireNonNull(accusedName, "accusedName");
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID accusedId() {
        return accusedId;
    }

    public String accusedName() {
        return accusedName;
    }

    public static TrialJuryBallotGui create(
            String kingdomId, UUID accusedId, String accusedName, long remainingMs) {
        TrialJuryBallotGui gui = new TrialJuryBallotGui(kingdomId, accusedId, accusedName);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, accusedName, remainingMs);
        return gui;
    }

    static void populate(Inventory inventory, String accusedName, long remainingMs) {
        inventory.clear();
        inventory.setItem(
                TrialJuryBallotLayout.SLOT_GUILTY,
                ItemBuilder.labelled(Material.LIME_CONCRETE, c("&aGuilty"), "Vote guilty"));
        inventory.setItem(
                TrialJuryBallotLayout.SLOT_NOT_GUILTY,
                ItemBuilder.labelled(Material.RED_CONCRETE, c("&cNot guilty"), "Vote not guilty"));
        List<String> lore = TrialJuryBallotLayout.infoLore(accusedName, remainingMs);
        String[] coloured = lore.stream().map(line -> c("&7" + line)).toArray(String[]::new);
        inventory.setItem(
                TrialJuryBallotLayout.SLOT_INFO,
                new ItemBuilder(Material.PAPER)
                        .displayAs(c("&6Trial of " + accusedName))
                        .lore(coloured)
                        .build());
        fillBackground(inventory);
    }

    public TrialJuryBallotAction actionForSlot(int slot) {
        return TrialJuryBallotLayout.actionForSlot(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
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
