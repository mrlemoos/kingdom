package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.honours.HonoursGui;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.service.KingdomResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/** The sword of honour: the Crown touching a subject with a golden sword opens the honours list. */
public final class HonoursGuiListener implements Listener {

    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final NoblePrefixDisplay nobleDisplay;
    private dev.mrlemoos.kingdom.church.ChurchService churchService;

    public HonoursGuiListener(
            KingdomService kingdomService, YamlKingdomStore store, NoblePrefixDisplay nobleDisplay) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = store;
        this.nobleDisplay = nobleDisplay;
    }

    /** Wires the coronation gate: an uncrowned monarch grants no honours. */
    public void setChurchService(dev.mrlemoos.kingdom.church.ChurchService churchService) {
        this.churchService = churchService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDubSubject(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        Player crownBearer = event.getPlayer();
        if (crownBearer.getInventory().getItemInMainHand().getType() != Material.GOLDEN_SWORD) {
            return;
        }
        if (crownBearer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        Optional<PlayerMembership> crown = kingdomService.getMembership(crownBearer.getUniqueId());
        if (crown.isEmpty() || !mayBestowHonours(crown.get().getRank())) {
            return;
        }
        Optional<PlayerMembership> subject = kingdomService.getMembership(target.getUniqueId());
        event.setCancelled(true);
        if (subject.isEmpty() || !crown.get().getKingdomId().equals(subject.get().getKingdomId())) {
            crownBearer.sendMessage(c("&cThat player is no subject of your realm."));
            return;
        }
        crownBearer.openInventory(
                HonoursGui.create(target.getUniqueId(), target.getName(), subject.get().getRank()).getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHonoursClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HonoursGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player crownBearer)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Optional<PlayerMembership> crown = kingdomService.getMembership(crownBearer.getUniqueId());
        if (crown.isEmpty() || !mayBestowHonours(crown.get().getRank())) {
            return;
        }
        Optional<PlayerMembership> subject = kingdomService.getMembership(gui.targetId());
        if (subject.isEmpty() || !crown.get().getKingdomId().equals(subject.get().getKingdomId())) {
            crownBearer.sendMessage(c("&cThat player is no subject of your realm."));
            crownBearer.closeInventory();
            return;
        }

        if (churchService != null) {
            Optional<String> uncrowned = churchService.ceremonialRefusal(
                    crown.get().getKingdomId(), crownBearer.getUniqueId(), crown.get().getRank());
            if (uncrowned.isPresent()) {
                crownBearer.sendMessage(c("&c" + uncrowned.get()));
                crownBearer.closeInventory();
                return;
            }
        }

        int slot = event.getSlot();
        if (HonoursGui.isStripSlot(slot)) {
            apply(crownBearer, gui.targetId(), kingdomService.clearTitle(gui.targetId()), null);
            return;
        }
        NobleRank rank = HonoursGui.rankForSlot(slot);
        if (rank == null) {
            return;
        }
        TitleStyle style = event.isRightClick() ? TitleStyle.FEMININE : TitleStyle.MASCULINE;
        apply(crownBearer, gui.targetId(), kingdomService.assignTitle(gui.targetId(), rank, style),
                rank.displayTitle(style));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HonoursGui) {
            event.setCancelled(true);
        }
    }

    private void apply(Player monarch, UUID targetId, KingdomResult result, String rankLabel) {
        monarch.closeInventory();
        if (!(result instanceof KingdomResult.Success success)) {
            monarch.sendMessage(c("&c" + ((KingdomResult.Failure) result).message()));
            return;
        }
        monarch.sendMessage(c("&a" + success.message()));
        if (store != null) {
            store.saveFrom(kingdomService);
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(monarch.getUniqueId());
        String kingdomId = membership.isPresent() ? membership.get().getKingdomId() : null;
        RealmFeedback.titleChanged(kingdomService, kingdomId, targetId, rankLabel);
        Player target = Bukkit.getPlayer(targetId);
        if (target != null && nobleDisplay != null) {
            nobleDisplay.refresh(target);
        }
    }

    private static boolean mayBestowHonours(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN
                || rank == NobleRank.PRINCE || rank == NobleRank.PRINCESS;
    }
}
