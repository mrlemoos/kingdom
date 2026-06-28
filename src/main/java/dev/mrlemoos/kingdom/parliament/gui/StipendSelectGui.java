package dev.mrlemoos.kingdom.parliament.gui;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class StipendSelectGui implements InventoryHolder {

    public static final String TITLE = ChatColor.DARK_GREEN + "Stipend recipient";

    static final int SLOT_OTHER = 49;

    private final String kingdomId;
    private final List<UUID> memberIds;
    private Inventory inventory;

    public StipendSelectGui(String kingdomId, List<UUID> memberIds) {
        this.kingdomId = kingdomId;
        this.memberIds = List.copyOf(memberIds);
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static StipendSelectGui create(String kingdomId, List<UUID> onlineMemberIds) {
        StipendSelectGui gui = new StipendSelectGui(kingdomId, onlineMemberIds);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, onlineMemberIds);
        return gui;
    }

    private static void populate(Inventory inventory, List<UUID> memberIds) {
        inventory.clear();
        int slot = 0;
        for (UUID memberId : memberIds) {
            if (slot >= 45) {
                break;
            }
            OfflinePlayer player = Bukkit.getOfflinePlayer(memberId);
            inventory.setItem(slot++, playerHead(player.getUniqueId(), player.getName()));
        }
        inventory.setItem(
                SLOT_OTHER,
                ItemBuilder.labelled(
                        Material.NAME_TAG,
                        ChatColor.AQUA + "Other player",
                        "Type a player name in chat"));
        fillBackground(inventory);
    }

    public UUID memberIdForSlot(int slot) {
        if (slot < 0 || slot >= memberIds.size() || slot >= 45) {
            return null;
        }
        return memberIds.get(slot);
    }

    public boolean isOtherPlayerSlot(int slot) {
        return slot == SLOT_OTHER;
    }

    private static ItemStack playerHead(UUID uuid, String name) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(uuid)
                .displayAs(ChatColor.WHITE + name)
                .lore(ChatColor.GRAY + "Table stipend for this member")
                .build();
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static List<UUID> onlineKingdomMembers(String kingdomId, dev.mrlemoos.kingdom.service.KingdomService kingdomService) {
        List<UUID> members = new ArrayList<>();
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            kingdomService.getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (membership.getKingdomId().equals(kingdomId)) {
                    members.add(online.getUniqueId());
                }
            });
        }
        return members;
    }
}
