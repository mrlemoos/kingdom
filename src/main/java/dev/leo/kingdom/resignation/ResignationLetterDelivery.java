package dev.leo.kingdom.resignation;

import dev.leo.kingdom.model.election.PendingResignation;
import dev.leo.kingdom.service.KingdomService;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ResignationLetterDelivery {

    private final KingdomService kingdomService;
    private final ResignationService resignationService;
    private final ResignationLetterItem letterItem;

    public ResignationLetterDelivery(
            KingdomService kingdomService,
            ResignationService resignationService,
            ResignationLetterItem letterItem) {
        this.kingdomService = kingdomService;
        this.resignationService = resignationService;
        this.letterItem = letterItem;
    }

    public void deliverPendingLetter(String kingdomId) {
        resignationService.pendingResignation(kingdomId).ifPresent(pending -> deliverToCrown(kingdomId, pending));
    }

    public void deliverToCrown(String kingdomId, PendingResignation pending) {
        ResignationAuthority.monarchOrRegent(kingdomId, kingdomService).ifPresent(approverId -> {
            Player approver = Bukkit.getPlayer(approverId);
            if (approver != null) {
                giveLetter(approver, kingdomId, ResignationSummaries.describe(pending));
            }
        });
    }

    public void deliverIfMissingOnJoin(Player player) {
        kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> {
            String kingdomId = membership.getKingdomId();
            if (!ResignationAuthority.canResolveResignation(kingdomId, kingdomService, membership.getRank())) {
                return;
            }
            if (resignationService.pendingResignation(kingdomId).isEmpty()) {
                return;
            }
            if (hasLetter(player, kingdomId)) {
                return;
            }
            deliverPendingLetter(kingdomId);
        });
    }

    public void giveLetter(Player player, String kingdomId, String summary) {
        removeLetters(player, kingdomId);
        ItemStack letter = letterItem.create(kingdomId, summary);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(letter);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full. The resignation letter was dropped at your feet.");
        }
        player.sendMessage(ChatColor.GOLD + "A resignation letter has been delivered.");
        player.sendMessage(ChatColor.GRAY + "Right-click the letter to review it.");
    }

    public void removeLetters(Player player, String kingdomId) {
        removeLetters(player.getInventory(), kingdomId);
    }

    public boolean hasLetter(Player player, String kingdomId) {
        return hasLetter(player.getInventory(), kingdomId);
    }

    private boolean hasLetter(PlayerInventory inventory, String kingdomId) {
        for (ItemStack stack : inventory.getContents()) {
            if (letterItem.kingdomId(stack).filter(kingdomId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void removeLetters(PlayerInventory inventory, String kingdomId) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (letterItem.kingdomId(stack).filter(kingdomId::equals).isPresent()) {
                inventory.setItem(slot, null);
            }
        }
    }
}
