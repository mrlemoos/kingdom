package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubSnapshotFactory;
import dev.mrlemoos.kingdom.hub.RealmHubView;
import dev.mrlemoos.kingdom.hub.gui.RealmHubGui;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Opens the Realm Hub on a bare {@code /kingdom} and carries its clicks through to the menus that
 * already exist. Nothing here decides a power: the hub only shows what {@code RealmHubView} chose.
 */
public final class RealmHubListener implements Listener {

    private final KingdomService kingdomService;
    private final RealmHubSnapshotFactory snapshots;
    private Consumer<Player> loyaltyOpener;
    private Consumer<Player> gazetteOpener;
    private Consumer<Player> parliamentOpener;
    private BiConsumer<Player, String> referendumOpener;
    private Consumer<Player> musterOpener;

    public RealmHubListener(KingdomService kingdomService, RealmHubSnapshotFactory snapshots) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    public RealmHubListener withLoyaltyOpener(Consumer<Player> loyaltyOpener) {
        this.loyaltyOpener = loyaltyOpener;
        return this;
    }

    public RealmHubListener withGazetteOpener(Consumer<Player> gazetteOpener) {
        this.gazetteOpener = gazetteOpener;
        return this;
    }

    public RealmHubListener withParliamentOpener(Consumer<Player> parliamentOpener) {
        this.parliamentOpener = parliamentOpener;
        return this;
    }

    public RealmHubListener withReferendumOpener(BiConsumer<Player, String> referendumOpener) {
        this.referendumOpener = referendumOpener;
        return this;
    }

    public RealmHubListener withMusterOpener(Consumer<Player> musterOpener) {
        this.musterOpener = musterOpener;
        return this;
    }

    /** Opens the hub at its first page. */
    public void openHub(Player player) {
        openHub(player, 0);
    }

    public void openHub(Player player, int page) {
        List<RealmHubEntry> entries = RealmHubView.entries(snapshots.of(player));
        player.openInventory(Objects.requireNonNull(RealmHubGui.create(entries, page).getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHubClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RealmHubGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == RealmHubLayout.SLOT_PREVIOUS) {
            openHub(player, gui.page() - 1);
            return;
        }
        if (slot == RealmHubLayout.SLOT_NEXT) {
            openHub(player, gui.page() + 1);
            return;
        }
        RealmHubEntry entry = gui.entryForSlot(slot);
        if (entry == null) {
            return;
        }
        if (!entry.usable()) {
            player.sendMessage(c("&c" + entry.refusal()));
            return;
        }
        act(player, entry.action());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RealmHubGui) {
            event.setCancelled(true);
        }
    }

    private void act(Player player, RealmHubAction action) {
        switch (action) {
            case OPEN_LOYALTY_LEDGER -> open(player, loyaltyOpener, "The loyalty ledger is not available.");
            case OPEN_GAZETTE -> open(player, gazetteOpener, "The Gazette is not available.");
            case OPEN_PARLIAMENT_HUB -> open(player, parliamentOpener, "Parliament is not available.");
            case OPEN_REFERENDUM_BALLOT -> openBallot(player);
            case OPEN_MUSTER -> open(player, musterOpener, "Muster is not available.");
            case NONE -> {
                // The lore names the command to type; nothing to open.
            }
        }
    }

    private void open(Player player, Consumer<Player> opener, String unavailable) {
        if (opener == null) {
            player.sendMessage(c("&c" + unavailable));
            return;
        }
        player.closeInventory();
        opener.accept(player);
    }

    private void openBallot(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (referendumOpener == null || membership.isEmpty()) {
            player.sendMessage(c("&cNo referendum is open to the realm."));
            return;
        }
        player.closeInventory();
        referendumOpener.accept(player, membership.get().getKingdomId());
    }
}
