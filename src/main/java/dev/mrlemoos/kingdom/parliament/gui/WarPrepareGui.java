package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Small declaration form. The target is typed once, while the political choices stay clickable. */
public final class WarPrepareGui implements InventoryHolder {

    public static final String TITLE = inventoryTitle(false);
    static final int SLOT_TERRITORY = 11;
    static final int SLOT_CAPITAL = 12;
    static final int SLOT_ANNEXATION = 14;
    static final int SLOT_TRIBUTE = 15;
    static final int SLOT_DAY_ONE = 20;
    static final int SLOT_DAY_THREE = 21;
    static final int SLOT_DAY_SEVEN = 22;
    static final int SLOT_CONFIRM = 31;
    static final int SLOT_CANCEL = 49;

    private final String kingdomId;
    private final String targetKingdomId;
    private final WarAim aim;
    private final WarOutcome outcome;
    private final int deadlineDays;
    private final boolean counterWar;
    private Inventory inventory;

    private WarPrepareGui(
            String kingdomId,
            String targetKingdomId,
            WarAim aim,
            WarOutcome outcome,
            int deadlineDays,
            boolean counterWar) {
        this.kingdomId = kingdomId;
        this.targetKingdomId = targetKingdomId;
        this.aim = aim;
        this.outcome = outcome;
        this.deadlineDays = deadlineDays;
        this.counterWar = counterWar;
    }

    public static String inventoryTitle(boolean counterWar) {
        return counterWar ? c("&4Prepare counter-war bill") : c("&4Prepare war bill");
    }

    public static String confirmLabel(boolean counterWar) {
        return counterWar ? c("&aTable counter-war bill") : c("&aTable war bill");
    }

    public static WarPrepareGui create(
            String kingdomId, String targetKingdomId, WarAim aim, WarOutcome outcome, int deadlineDays) {
        return create(kingdomId, targetKingdomId, aim, outcome, deadlineDays, false);
    }

    public static WarPrepareGui create(
            String kingdomId,
            String targetKingdomId,
            WarAim aim,
            WarOutcome outcome,
            int deadlineDays,
            boolean counterWar) {
        WarPrepareGui gui = new WarPrepareGui(kingdomId, targetKingdomId, aim, outcome, deadlineDays, counterWar);
        gui.inventory = Bukkit.createInventory(gui, 54, inventoryTitle(counterWar));
        gui.populate();
        return gui;
    }

    public String kingdomId() { return kingdomId; }
    public String targetKingdomId() { return targetKingdomId; }
    public WarAim aim() { return aim; }
    public WarOutcome outcome() { return outcome; }
    public int deadlineDays() { return deadlineDays; }
    public boolean counterWar() { return counterWar; }

    public Action actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_TERRITORY -> Action.TERRITORY;
            case SLOT_CAPITAL -> Action.CAPITAL;
            case SLOT_ANNEXATION -> Action.ANNEXATION;
            case SLOT_TRIBUTE -> Action.TRIBUTE;
            case SLOT_DAY_ONE -> Action.DAY_ONE;
            case SLOT_DAY_THREE -> Action.DAY_THREE;
            case SLOT_DAY_SEVEN -> Action.DAY_SEVEN;
            case SLOT_CONFIRM -> Action.CONFIRM;
            case SLOT_CANCEL -> Action.CANCEL;
            default -> null;
        };
    }

    @Override public Inventory getInventory() { return inventory; }

    private void populate() {
        inventory.setItem(4, ItemBuilder.labelled(Material.MAP, c("&6Target: " + targetKingdomId), "Choose terms below"));
        choice(SLOT_TERRITORY, Material.GRASS_BLOCK, "Territory threshold", aim == WarAim.TERRITORY_THRESHOLD);
        choice(SLOT_CAPITAL, Material.BEACON, "Capital fall", aim == WarAim.CAPITAL_FALL);
        choice(SLOT_ANNEXATION, Material.IRON_SWORD, "Annexation", outcome == WarOutcome.ANNEXATION);
        choice(SLOT_TRIBUTE, Material.GOLD_INGOT, "War tribute", outcome == WarOutcome.WAR_TRIBUTE);
        deadline(SLOT_DAY_ONE, 1); deadline(SLOT_DAY_THREE, 3); deadline(SLOT_DAY_SEVEN, 7);
        inventory.setItem(SLOT_CONFIRM, ItemBuilder.labelled(Material.LIME_CONCRETE, confirmLabel(counterWar), "Put this declaration before the House"));
        inventory.setItem(SLOT_CANCEL, ItemBuilder.labelled(Material.BARRIER, c("&cCancel"), "Close without tabling"));
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE));
    }

    private void choice(int slot, Material material, String label, boolean chosen) {
        inventory.setItem(slot, ItemBuilder.labelled(material, c((chosen ? "&a" : "&7") + label), chosen ? "Selected" : "Click to select"));
    }

    private void deadline(int slot, int days) {
        inventory.setItem(slot, ItemBuilder.labelled(Material.CLOCK, c((deadlineDays == days ? "&a" : "&7") + days + " day muster"), deadlineDays == days ? "Selected" : "Click to select"));
    }

    public enum Action { TERRITORY, CAPITAL, ANNEXATION, TRIBUTE, DAY_ONE, DAY_THREE, DAY_SEVEN, CONFIRM, CANCEL }
}
