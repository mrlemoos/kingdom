package dev.mrlemoos.kingdom.church.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** A list of heads to choose from: a partner to marry, or a marriage for the Crown to annul. */
public final class RitePickGui implements InventoryHolder {

    public enum Purpose {
        PARTNER("&6Choose your partner"),
        ANNUL("&6Choose a marriage to annul");

        private final String title;

        Purpose(String title) {
            this.title = title;
        }
    }

    /** One head: the player it names, and the lines under it. */
    public record Pick(UUID playerId, String label, List<String> lore) {}

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;
    private static final int CAPACITY = 45;

    private final Purpose purpose;
    private final String kingdomId;
    private final Map<Integer, UUID> picks = new HashMap<>();
    private Inventory inventory;

    private RitePickGui(Purpose purpose, String kingdomId) {
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public static RitePickGui create(Purpose purpose, String kingdomId, List<Pick> offered) {
        RitePickGui gui = new RitePickGui(purpose, kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, SIZE, component(purpose.title));
        gui.inventory = inventory;
        int slot = 0;
        for (Pick pick : offered) {
            if (slot >= CAPACITY) {
                break;
            }
            ItemBuilder head = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(pick.playerId())
                    .displayAs(c("&e" + pick.label()));
            for (String line : pick.lore()) {
                head.lore(c("&7" + line));
            }
            inventory.setItem(slot, head.build());
            gui.picks.put(slot, pick.playerId());
            slot++;
        }
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW).displayAs(c("&7Back to the rites")).build());
        return gui;
    }

    public Purpose purpose() {
        return purpose;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public Optional<UUID> pickAt(int slot) {
        return Optional.ofNullable(picks.get(slot));
    }

    public boolean isBackSlot(int slot) {
        return slot == SLOT_BACK;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
