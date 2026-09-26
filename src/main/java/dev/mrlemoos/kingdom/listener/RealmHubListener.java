package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubSection;
import dev.mrlemoos.kingdom.hub.RealmHubSnapshot;
import dev.mrlemoos.kingdom.hub.RealmHubSnapshotFactory;
import dev.mrlemoos.kingdom.hub.RealmHubView;
import dev.mrlemoos.kingdom.hub.gui.RealmHubGui;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private Consumer<Player> treatyRegisterOpener;
    private Consumer<Player> warrantRegisterOpener;
    private Consumer<Player> warDebtOpener;
    private BiConsumer<Player, FoundationStone> stoneTaker;
    private BiConsumer<Player, FoundationStone> siteClearer;

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

    public RealmHubListener withTreatyRegisterOpener(Consumer<Player> treatyRegisterOpener) {
        this.treatyRegisterOpener = treatyRegisterOpener;
        return this;
    }

    public RealmHubListener withWarrantRegisterOpener(Consumer<Player> warrantRegisterOpener) {
        this.warrantRegisterOpener = warrantRegisterOpener;
        return this;
    }

    public RealmHubListener withWarDebtOpener(Consumer<Player> warDebtOpener) {
        this.warDebtOpener = warDebtOpener;
        return this;
    }

    /** Hands out a place's foundation stone, and offers to clear its site on a right-click. */
    public RealmHubListener withFoundationStones(
            BiConsumer<Player, FoundationStone> stoneTaker, BiConsumer<Player, FoundationStone> siteClearer) {
        this.stoneTaker = stoneTaker;
        this.siteClearer = siteClearer;
        return this;
    }

    /** Opens the hub at its front page: standing, live business and a door to each section. */
    public void openHub(Player player) {
        RealmHubSnapshot snapshot = snapshots.of(player);
        Map<RealmHubSection, List<RealmHubEntry>> sections = new EnumMap<>(RealmHubSection.class);
        for (RealmHubSection section : RealmHubSection.values()) {
            sections.put(section, RealmHubView.section(snapshot, section));
        }
        player.openInventory(Objects.requireNonNull(
                RealmHubGui.frontPage(RealmHubView.frontPage(snapshot), sections).getInventory()));
    }

    /** Opens one hub section at the given page. */
    public void openSection(Player player, RealmHubSection section, int page) {
        List<RealmHubEntry> entries = RealmHubView.section(snapshots.of(player), section);
        player.openInventory(Objects.requireNonNull(RealmHubGui.section(section, entries, page).getInventory()));
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
        Optional<RealmHubSection> shown = gui.section();
        if (shown.isEmpty()) {
            RealmHubSection door = gui.doorForSlot(slot);
            if (door != null) {
                openSection(player, door, 0);
                return;
            }
        } else {
            RealmHubSection section = shown.get();
            if (slot == RealmHubLayout.SLOT_BACK) {
                openHub(player);
                return;
            }
            if (slot == RealmHubLayout.SLOT_PREVIOUS && gui.hasPrevious()) {
                openSection(player, section, gui.page() - 1);
                return;
            }
            if (slot == RealmHubLayout.SLOT_NEXT && gui.hasNext()) {
                openSection(player, section, gui.page() + 1);
                return;
            }
        }
        RealmHubEntry entry = gui.entryForSlot(slot);
        if (entry == null) {
            return;
        }
        if (!entry.usable()) {
            RealmFeedback.refuse(player, entry.refusal());
            return;
        }
        if (entry.action() == RealmHubAction.TAKE_FOUNDATION_STONE) {
            foundationStone(player, entry, event.isRightClick());
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
            case OPEN_TREATY_REGISTER -> open(player, treatyRegisterOpener, "The treaty register is not available.");
            case OPEN_WARRANT_REGISTER -> open(player, warrantRegisterOpener, "The warrant register is not available.");
            case OPEN_WAR_DEBT -> open(player, warDebtOpener, "War debt is not available.");
            case NONE, TAKE_FOUNDATION_STONE -> {
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

    private void foundationStone(Player player, RealmHubEntry entry, boolean rightClick) {
        Optional<FoundationStone> kind = FoundationStone.forTopic(entry.topic());
        BiConsumer<Player, FoundationStone> handler = rightClick ? siteClearer : stoneTaker;
        if (kind.isEmpty() || handler == null) {
            player.sendMessage(c("&cThat stone is not available."));
            return;
        }
        handler.accept(player, kind.get());
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
