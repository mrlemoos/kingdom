package dev.mrlemoos.kingdom.church.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.church.Rite;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The rites window: every rite the clicker may ask the cleric for, and the Crown's coronation. */
public final class RitesGui implements InventoryHolder {

    public static final Component TITLE = component("&6The Rites");
    public static final int SLOT_LEAVE = 22;
    private static final int FIRST_SLOT = 10;

    /** A button in the window: a rite, or one of the cleric's older books. */
    public enum Choice {
        CORONATION,
        OATH,
        CONSECRATE,
        MARRY,
        DIVORCE,
        ANNUL,
        FUNERAL,
        VILLAGER_FUNERAL;

        public static Choice of(Rite rite) {
            return valueOf(rite.name());
        }
    }

    private final String kingdomId;
    private final Map<Integer, Choice> choices = new HashMap<>();
    private Inventory inventory;

    private RitesGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    /**
     * @param villagersAwaiting how many villagers of the realm await their rites
     */
    public static RitesGui create(String kingdomId, String realmName, List<Choice> offered, int villagersAwaiting) {
        RitesGui gui = new RitesGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        int slot = FIRST_SLOT;
        for (Choice choice : offered) {
            inventory.setItem(slot, button(choice, realmName, villagersAwaiting));
            gui.choices.put(slot, choice);
            slot++;
        }
        inventory.setItem(SLOT_LEAVE, new ItemBuilder(Material.BARRIER).displayAs(c("&cLeave the church")).build());
        fillBackground(inventory);
        return gui;
    }

    public Optional<Choice> choiceAt(int slot) {
        return Optional.ofNullable(choices.get(slot));
    }

    public boolean isLeaveSlot(int slot) {
        return slot == SLOT_LEAVE;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack button(Choice choice, String realmName, int villagersAwaiting) {
        return switch (choice) {
            case CORONATION -> new ItemBuilder(Material.GOLDEN_HELMET)
                    .displayAs(c("&6Coronation"))
                    .lore(c("&7The crowning of " + realmName + "'s monarch."))
                    .build();
            case OATH -> new ItemBuilder(Material.IRON_SWORD)
                    .displayAs(c("&aOath of service"))
                    .lore(c("&7Open your military morale before the cleric."))
                    .build();
            case CONSECRATE -> new ItemBuilder(Material.CANDLE)
                    .displayAs(c("&aConsecrate the church"))
                    .lore(c("&7Bring the church into use; no rite is held until then."))
                    .build();
            case MARRY -> new ItemBuilder(Material.POPPY)
                    .displayAs(c("&aMarriage"))
                    .lore(c("&7Choose a subject standing at the church."))
                    .lore(c("&7They have a minute to accept."))
                    .build();
            case DIVORCE -> new ItemBuilder(Material.SHEARS)
                    .displayAs(c("&eDivorce"))
                    .lore(c("&7Your spouse must consent at the church."))
                    .build();
            case ANNUL -> new ItemBuilder(Material.WRITABLE_BOOK)
                    .displayAs(c("&6Annulment"))
                    .lore(c("&7The Crown's remedy where a spouse will not consent."))
                    .build();
            case FUNERAL -> new ItemBuilder(Material.SKELETON_SKULL)
                    .displayAs(c("&7Your funeral"))
                    .lore(c("&7Half your held experience is returned to you."))
                    .build();
            case VILLAGER_FUNERAL -> new ItemBuilder(Material.WITHER_ROSE)
                    .displayAs(c("&7A villager's funeral"))
                    .lore(c("&7" + villagersAwaiting + " villager(s) await their rites."))
                    .lore(c("&7The longest waiting is buried first;"))
                    .lore(c("&7its wallet passes to the treasury, less the tithe."))
                    .build();
        };
    }

    static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
