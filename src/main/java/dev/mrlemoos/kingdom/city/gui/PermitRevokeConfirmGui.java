package dev.mrlemoos.kingdom.city.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Revoke or step back: the Crown's second thought before striking a holder from the register. */
public final class PermitRevokeConfirmGui implements InventoryHolder {

    public static final Component TITLE = component("&4Revoke permit");

    private final String kingdomId;
    private final UUID holderId;
    private final int registerPage;
    private Inventory inventory;

    public PermitRevokeConfirmGui(String kingdomId, UUID holderId, int registerPage) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.holderId = Objects.requireNonNull(holderId, "holderId");
        this.registerPage = registerPage;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID holderId() {
        return holderId;
    }

    public int registerPage() {
        return registerPage;
    }

    public static PermitRevokeConfirmGui create(
            String kingdomId, UUID holderId, String holderName, int registerPage) {
        PermitRevokeConfirmGui gui = new PermitRevokeConfirmGui(kingdomId, holderId, registerPage);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(
                PermitRegisterLayout.SLOT_CONFIRM_REVOKE,
                ItemBuilder.labelled(
                        Material.RED_WOOL, c("&cRevoke permit"), "Strike " + holderName + " from the register"));
        inventory.setItem(
                PermitRegisterLayout.SLOT_CONFIRM_HOLDER,
                new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(holderId)
                        .displayAs(c("&f" + holderName))
                        .lore(c("&7Holds a build permit"))
                        .build());
        inventory.setItem(
                PermitRegisterLayout.SLOT_CONFIRM_BACK,
                ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the register"));
        fillBackground(inventory);
        return gui;
    }

    public boolean isRevokeSlot(int slot) {
        return slot == PermitRegisterLayout.SLOT_CONFIRM_REVOKE;
    }

    public boolean isBackSlot(int slot) {
        return slot == PermitRegisterLayout.SLOT_CONFIRM_BACK;
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
