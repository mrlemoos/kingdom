package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.command.ParliamentHandler;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.treaty.TreatyRegister;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import dev.mrlemoos.kingdom.treaty.gui.TreatyRegisterGui;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Opens the treaty register and carries the Crown's click through to a treaty bill in the Commons. */
public final class TreatyRegisterListener implements Listener {

    private final KingdomService kingdomService;
    private final TreatyService treatyService;
    private final ParliamentHandler parliamentHandler;
    private final LongSupplier mcDayClock;

    public TreatyRegisterListener(
            KingdomService kingdomService, TreatyService treatyService, ParliamentHandler parliamentHandler,
            LongSupplier mcDayClock) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.treatyService = Objects.requireNonNull(treatyService, "treatyService");
        this.parliamentHandler = Objects.requireNonNull(parliamentHandler, "parliamentHandler");
        this.mcDayClock = Objects.requireNonNull(mcDayClock, "mcDayClock");
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int page) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(c("&cYou are not a member of any kingdom."));
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        List<TreatyRegister.Row> rows =
                TreatyRegister.rows(kingdomId, treatyService.treatiesView(), mcDayClock.getAsLong());
        player.openInventory(Objects.requireNonNull(TreatyRegisterGui.create(
                kingdomId, rows, page, RankAuthority.canTableTreaty(membership.get().getRank()), this::nameOf)
                .getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegisterClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TreatyRegisterGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == dev.mrlemoos.kingdom.city.gui.PermitRegisterLayout.SLOT_PREVIOUS) {
            open(player, gui.page() - 1);
            return;
        }
        if (slot == dev.mrlemoos.kingdom.city.gui.PermitRegisterLayout.SLOT_NEXT) {
            open(player, gui.page() + 1);
            return;
        }
        TreatyRegister.Row row = gui.rowForSlot(slot);
        if (row == null || !row.crownMayAnswer() || !isCrownOf(player, gui.kingdomId())) {
            return;
        }
        player.openInventory(Objects.requireNonNull(TreatyRegisterGui.Confirm
                .create(gui.kingdomId(), row, gui.page(), nameOf(row.counterpartId()))
                .getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TreatyRegisterGui.Confirm gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isBackSlot(event.getSlot())) {
            open(player, gui.registerPage());
            return;
        }
        if (!gui.isTableSlot(event.getSlot())) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !gui.kingdomId().equals(membership.get().getKingdomId())) {
            player.closeInventory();
            return;
        }
        // ParliamentService re-checks rank, session, and the order paper.
        parliamentHandler.finish(player, parliamentHandler.tableTreaty(
                gui.kingdomId(), membership.get().getRank(), player.getUniqueId(),
                gui.row().counterpartId(), gui.row().kind(), gui.row().answerIsRepeal(), null));
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TreatyRegisterGui
                || event.getInventory().getHolder() instanceof TreatyRegisterGui.Confirm) {
            event.setCancelled(true);
        }
    }

    private boolean isCrownOf(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        return membership.isPresent()
                && kingdomId.equals(membership.get().getKingdomId())
                && RankAuthority.canTableTreaty(membership.get().getRank());
    }

    private String nameOf(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }
}
