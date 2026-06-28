package dev.leo.kingdom.parliament.gui;

import dev.leo.kingdom.helpers.ItemBuilder;
import dev.leo.kingdom.economy.model.MintLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class MintPrepareGui implements InventoryHolder {

    public static final String TITLE = ChatColor.DARK_GREEN + "Prepare mint";

    static final int SLOT_CONFIRM = 11;
    static final int SLOT_LECTERN = 13;
    static final int SLOT_REPLACE = 15;
    static final int SLOT_CANCEL = 22;

    private final String kingdomId;
    private final Optional<MintLocation> preparedLocation;
    private Inventory inventory;

    public MintPrepareGui(String kingdomId, Optional<MintLocation> preparedLocation) {
        this.kingdomId = kingdomId;
        this.preparedLocation = preparedLocation != null ? preparedLocation : Optional.empty();
    }

    public String kingdomId() {
        return kingdomId;
    }

    public Optional<MintLocation> preparedLocation() {
        return preparedLocation;
    }

    public boolean hasPreparedLocation() {
        return preparedLocation.isPresent();
    }

    public static MintPrepareGui create(String kingdomId, Optional<MintLocation> preparedLocation) {
        MintPrepareGui gui = new MintPrepareGui(kingdomId, preparedLocation);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, preparedLocation);
        return gui;
    }

    static void populate(Inventory inventory, Optional<MintLocation> preparedLocation) {
        inventory.clear();
        inventory.setItem(SLOT_LECTERN, lecternInfoItem(preparedLocation));
        inventory.setItem(
                SLOT_CONFIRM,
                ItemBuilder.labelled(
                        Material.LIME_CONCRETE,
                        ChatColor.GREEN + "Confirm",
                        preparedLocation.isPresent()
                                ? "Keep this prepared mint location"
                                : "Confirm the lectern you are facing"));
        if (preparedLocation.isPresent()) {
            inventory.setItem(
                    SLOT_REPLACE,
                    ItemBuilder.labelled(Material.ORANGE_CONCRETE, ChatColor.GOLD + "Replace", "Choose a different lectern"));
        }
        inventory.setItem(
                SLOT_CANCEL, ItemBuilder.labelled(Material.BARRIER, ChatColor.RED + "Cancel", "Close without saving"));
        fillBackground(inventory);
    }

    public MintPrepareAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_CONFIRM -> MintPrepareAction.CONFIRM;
            case SLOT_REPLACE -> preparedLocation.isPresent() ? MintPrepareAction.REPLACE : null;
            case SLOT_CANCEL -> MintPrepareAction.CANCEL;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public enum MintPrepareAction {
        CONFIRM,
        REPLACE,
        CANCEL
    }

    private static ItemStack lecternInfoItem(Optional<MintLocation> preparedLocation) {
        ItemBuilder builder = new ItemBuilder(Material.LECTERN).displayAs(ChatColor.GOLD + "Mint location");
        List<String> lore = new ArrayList<>();
        if (preparedLocation.isPresent()) {
            MintLocation location = preparedLocation.get();
            lore.add(ChatColor.GREEN + "Prepared");
            lore.add(ChatColor.GRAY + String.format(
                    Locale.UK,
                    "%s %d, %d, %d",
                    location.worldName(),
                    location.x(),
                    location.y(),
                    location.z()));
        } else {
            lore.add(ChatColor.YELLOW + "Not yet prepared");
            lore.add(ChatColor.GRAY + "Face a lectern and confirm");
        }
        return builder.lore(lore).build();
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
