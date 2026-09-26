package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.honours.HonoursGui;
import dev.mrlemoos.kingdom.honours.SwornRoleAppointments;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.service.KingdomResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

/**
 * The sword of honour: the Crown touching a subject with a golden sword opens the honours list, from
 * which titles are bestowed and the sworn roles sworn and unsworn.
 */
public final class HonoursGuiListener implements Listener {

    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final NoblePrefixDisplay nobleDisplay;
    private dev.mrlemoos.kingdom.church.ChurchService churchService;
    private dev.mrlemoos.kingdom.church.ClericService clericService;
    private SwornRoleAppointments swornRoles;

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

    /** Wires the sworn-roles row; the cleric is stood down or recalled as the priesthood changes. */
    public void setSwornRoles(
            SwornRoleAppointments swornRoles, dev.mrlemoos.kingdom.church.ClericService clericService) {
        this.swornRoles = swornRoles;
        this.clericService = clericService;
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
        crownBearer.openInventory(HonoursGui.create(
                        target.getUniqueId(),
                        target.getName(),
                        subject.get().getRank(),
                        swornRolesOf(subject.get().getKingdomId(), target.getUniqueId()))
                .getInventory());
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

        SwornRole swornRole = HonoursGui.swornRoleForSlot(event.getSlot());
        if (swornRole != null) {
            toggleSwornRole(crownBearer, crown.get(), gui, swornRole);
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

    private void toggleSwornRole(Player crownBearer, PlayerMembership crown, HonoursGui gui, SwornRole role) {
        if (swornRoles == null) {
            return;
        }
        String kingdomId = crown.getKingdomId();
        UUID targetId = gui.targetId();
        SwornRoleAppointments.Outcome outcome = swornRoles.toggle(
                kingdomId, crownBearer.getUniqueId(), crown.getRank(), targetId, role);
        if (!outcome.success()) {
            RealmFeedback.refuse(crownBearer, outcome.message());
            return;
        }
        if (store != null) {
            store.saveFrom(kingdomService);
        }
        if (role == SwornRole.PRIEST && clericService != null) {
            kingdomService.getKingdom(kingdomId).ifPresent(clericService::reconcile);
        }

        String label = SwornRoleAppointments.label(role);
        String realm = realmName(kingdomId);
        Player target = Bukkit.getPlayer(targetId);
        String targetName = target != null ? target.getName() : nameOf(targetId);
        String record = outcome.sworn()
                ? "&6" + targetName + " is sworn " + label + " of " + realm + "."
                : "&6" + targetName + " is released from the office of " + label + ".";
        crownBearer.sendMessage(c(record));
        RealmFeedback.actionBar(crownBearer, outcome.sworn()
                ? "&a" + targetName + " sworn " + label
                : "&e" + targetName + " released as " + label);
        if (target != null) {
            target.sendMessage(c(record));
            if (outcome.sworn()) {
                RealmFeedback.milestone(List.of(target), "&6Sworn " + label, "&eof " + realm);
                RealmFeedback.success(target.getLocation());
            }
            if (nobleDisplay != null) {
                nobleDisplay.refresh(target);
            }
        }
        Optional<PlayerMembership> subject = kingdomService.getMembership(targetId);
        HonoursGui.populate(
                gui.getInventory(),
                targetName,
                subject.isPresent() ? subject.get().getRank() : null,
                swornRolesOf(kingdomId, targetId));
    }

    private Set<SwornRole> swornRolesOf(String kingdomId, UUID playerId) {
        return swornRoles == null ? Set.of() : swornRoles.heldBy(kingdomId, playerId);
    }

    private String realmName(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }

    private static String nameOf(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? "That subject" : name;
    }

    private static boolean mayBestowHonours(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }
}
