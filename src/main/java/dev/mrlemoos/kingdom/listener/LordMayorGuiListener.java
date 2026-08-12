package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.city.gui.PermitApplicantStatus;
import dev.mrlemoos.kingdom.city.gui.PermitApplyGui;
import dev.mrlemoos.kingdom.city.gui.PermitRegisterGui;
import dev.mrlemoos.kingdom.city.gui.PermitRegisterLayout;
import dev.mrlemoos.kingdom.city.gui.PermitRevokeConfirmGui;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * The city hall counter. Right-clicking the Lord Mayor opens the permit application; the monarch
 * or a prince shift-right-clicking opens the permit register instead.
 */
public final class LordMayorGuiListener implements Listener {

    private final LordMayorService lordMayorService;
    private final CityService cityService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public LordMayorGuiListener(
            LordMayorService lordMayorService,
            CityService cityService,
            KingdomService kingdomService,
            YamlKingdomStore store) {
        this.lordMayorService = Objects.requireNonNull(lordMayorService, "lordMayorService");
        this.cityService = Objects.requireNonNull(cityService, "cityService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractMayor(PlayerInteractEntityEvent event) {
        if (!lordMayorService.isLordMayor(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<String> kingdomId = lordMayorService.kingdomIdOf(event.getRightClicked());
        if (kingdomId.isEmpty()) {
            player.sendMessage(c("&cThis Lord Mayor serves no kingdom."));
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId.get());
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cThis Lord Mayor serves no kingdom."));
            return;
        }

        if (player.isSneaking() && isRegistrarOf(kingdomId.get(), player.getUniqueId())) {
            openRegister(player, kingdomId.get(), 0);
            return;
        }
        openApply(player, kingdom.get());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onApplyClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PermitApplyGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (!gui.isApplySlot(event.getSlot())) {
            return;
        }

        UUID applicantId = player.getUniqueId();
        PermitApplicantStatus status = statusOf(gui.kingdomId(), applicantId);
        switch (status) {
            case FOREIGNER -> player.sendMessage(
                    c("&cYou are not of this kingdom; foreigners may hold no build permit here."));
            case PRISONER -> player.sendMessage(
                    c("&cYou are serving a prison sentence; no permit may be issued to you."));
            case EXEMPT -> player.sendMessage(
                    c("&eThe Crown builds by right. You need no build permit here."));
            case LICENSED -> player.sendMessage(c("&eYou already hold a build permit."));
            case ELIGIBLE -> {
                CityResult result = cityService.grantPermit(gui.kingdomId(), applicantId);
                if (result instanceof CityResult.Success) {
                    player.sendMessage(c("&a" + result.message()));
                    save();
                } else {
                    player.sendMessage(c("&c" + result.message()));
                }
            }
        }
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegisterClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PermitRegisterGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (!isRegistrarOf(gui.kingdomId(), player.getUniqueId())) {
            return;
        }

        int slot = event.getSlot();
        if (slot == PermitRegisterLayout.SLOT_PREVIOUS) {
            openRegister(player, gui.kingdomId(), gui.page() - 1);
            return;
        }
        if (slot == PermitRegisterLayout.SLOT_NEXT) {
            openRegister(player, gui.kingdomId(), gui.page() + 1);
            return;
        }
        UUID holderId = gui.holderForSlot(slot);
        if (holderId == null) {
            return;
        }
        player.openInventory(PermitRevokeConfirmGui
                .create(gui.kingdomId(), holderId, nameOf(holderId), gui.page())
                .getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PermitRevokeConfirmGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (!isRegistrarOf(gui.kingdomId(), player.getUniqueId())) {
            return;
        }

        if (gui.isBackSlot(event.getSlot())) {
            openRegister(player, gui.kingdomId(), gui.registerPage());
            return;
        }
        if (!gui.isRevokeSlot(event.getSlot())) {
            return;
        }

        CityResult result = cityService.revokePermit(gui.kingdomId(), gui.holderId());
        if (result instanceof CityResult.Success) {
            player.sendMessage(c("&a" + result.message()));
            save();
            notifyHolder(gui.holderId());
        } else {
            player.sendMessage(c("&c" + result.message()));
        }
        openRegister(player, gui.kingdomId(), gui.registerPage());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder instanceof PermitApplyGui
                || holder instanceof PermitRegisterGui
                || holder instanceof PermitRevokeConfirmGui) {
            event.setCancelled(true);
        }
    }

    private void openApply(Player player, Kingdom kingdom) {
        PermitApplicantStatus status = statusOf(kingdom.getId(), player.getUniqueId());
        player.openInventory(
                PermitApplyGui.create(kingdom.getId(), kingdom.getDisplayName(), status).getInventory());
    }

    private void openRegister(Player player, String kingdomId, int page) {
        List<PermitRegisterGui.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Long> permit : cityService.permitsView(kingdomId).entrySet()) {
            entries.add(new PermitRegisterGui.Entry(permit.getKey(), permit.getValue()));
        }
        entries.sort(Comparator.comparingLong(PermitRegisterGui.Entry::grantedAtMs));
        player.openInventory(PermitRegisterGui
                .create(kingdomId, entries, page, System.currentTimeMillis())
                .getInventory());
    }

    private PermitApplicantStatus statusOf(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return PermitApplicantStatus.FOREIGNER;
        }
        NobleRank rank = membership.get().getRank();
        if (rank != null && CityService.isRoyalExempt(rank)) {
            return PermitApplicantStatus.EXEMPT;
        }
        if (cityService.hasPermit(kingdomId, playerId)) {
            return PermitApplicantStatus.LICENSED;
        }
        if (cityService.isUnderPrisonSentence(playerId)) {
            return PermitApplicantStatus.PRISONER;
        }
        return PermitApplicantStatus.ELIGIBLE;
    }

    /** Only the King, Queen or a Prince/Princess of this kingdom may read the register. */
    private boolean isRegistrarOf(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return false;
        }
        NobleRank rank = membership.get().getRank();
        return rank != null && CityService.isRoyalExempt(rank);
    }

    private void notifyHolder(UUID holderId) {
        Player holder = Bukkit.getPlayer(holderId);
        if (holder != null && holder.isOnline()) {
            holder.sendMessage(c("&cYour build permit has been revoked by the Crown."));
        }
    }

    private String nameOf(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name == null ? playerId.toString() : name;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }
}
