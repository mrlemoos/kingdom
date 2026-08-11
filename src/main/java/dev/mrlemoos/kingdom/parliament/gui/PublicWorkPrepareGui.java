package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class PublicWorkPrepareGui implements InventoryHolder {

    public static final String TITLE = c("&2Prepare public work");

    static final int SLOT_BEACON = 11;
    static final int SLOT_CONDUIT = 13;
    static final int SLOT_LODESTONE = 15;
    static final int SLOT_INFO = 4;
    static final int SLOT_CANCEL = 22;

    private final String kingdomId;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private Inventory inventory;

    public PublicWorkPrepareGui(String kingdomId, String worldName, int x, int y, int z) {
        this.kingdomId = kingdomId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public static PublicWorkPrepareGui create(String kingdomId, String worldName, int x, int y, int z) {
        PublicWorkPrepareGui gui = new PublicWorkPrepareGui(kingdomId, worldName, x, y, z);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, worldName, x, y, z);
        return gui;
    }

    static void populate(Inventory inventory, String worldName, int x, int y, int z) {
        inventory.clear();
        inventory.setItem(SLOT_INFO, siteInfoItem(worldName, x, y, z));
        inventory.setItem(
                SLOT_BEACON,
                ItemBuilder.labelled(
                        Material.BEACON, c("&bBeacon"), "Estate worth from realm-wealth config"));
        inventory.setItem(
                SLOT_CONDUIT,
                ItemBuilder.labelled(
                        Material.CONDUIT, c("&bConduit"), "Estate worth from realm-wealth config"));
        inventory.setItem(
                SLOT_LODESTONE,
                ItemBuilder.labelled(
                        Material.LODESTONE, c("&bLodestone"), "Estate worth from realm-wealth config"));
        inventory.setItem(
                SLOT_CANCEL, ItemBuilder.labelled(Material.BARRIER, c("&cCancel"), "Close without saving"));
        fillBackground(inventory);
    }

    public Optional<WealthBlockType> typeForSlot(int slot) {
        return switch (slot) {
            case SLOT_BEACON -> Optional.of(WealthBlockType.BEACON);
            case SLOT_CONDUIT -> Optional.of(WealthBlockType.CONDUIT);
            case SLOT_LODESTONE -> Optional.of(WealthBlockType.LODESTONE);
            default -> Optional.empty();
        };
    }

    public boolean isCancelSlot(int slot) {
        return slot == SLOT_CANCEL;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack siteInfoItem(String worldName, int x, int y, int z) {
        List<String> lore = new ArrayList<>();
        lore.add(c("&7" + String.format(Locale.UK, "%s %d, %d, %d", worldName, x, y, z)));
        lore.add(c("&7Choose an estate block to place here"));
        return new ItemBuilder(Material.MAP).displayAs(c("&6Public work site")).lore(lore).build();
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
