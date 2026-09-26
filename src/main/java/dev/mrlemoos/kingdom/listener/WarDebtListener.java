package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.helpers.AmountPickGui;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.war.tribute.TributeDesk;
import dev.mrlemoos.kingdom.war.tribute.WarDebt;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import dev.mrlemoos.kingdom.war.tribute.gui.WarDebtGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** The Crown pays war debt from the Realm Hub; every payment is {@link TributeDesk}'s. */
public final class WarDebtListener implements Listener {

    private final KingdomService kingdomService;
    private final WarTributeService tribute;
    private final TributeDesk desk;
    private final EconomyService economyService;
    private final YamlEconomyStore economyStore;
    private Consumer<Player> treasurySectionOpener;

    public WarDebtListener(
            KingdomService kingdomService,
            WarTributeService tribute,
            EconomyService economyService,
            YamlEconomyStore economyStore) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.tribute = Objects.requireNonNull(tribute, "tribute");
        this.desk = new TributeDesk(tribute);
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
    }

    /** Where Back leads: the Hub's Treasury section. */
    public WarDebtListener withTreasurySectionOpener(Consumer<Player> treasurySectionOpener) {
        this.treasurySectionOpener = treasurySectionOpener;
        return this;
    }

    public void open(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !RankAuthority.canPayWarDebt(membership.get().getRank())) {
            RealmFeedback.refuse(player, "Only the King or Queen may pay war debt.");
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        List<WarDebt> owed = new ArrayList<>();
        for (WarDebt debt : tribute.allDebts()) {
            if (debt.debtorKingdomId().equals(kingdomId) && debt.amount() > 0) {
                owed.add(debt);
            }
        }
        player.openInventory(Objects.requireNonNull(
                WarDebtGui.create(kingdomId, owed, this::display).getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WarDebtGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() == WarDebtGui.SLOT_BACK) {
            if (treasurySectionOpener != null) {
                treasurySectionOpener.accept(player);
            } else {
                player.closeInventory();
            }
            return;
        }
        String creditorId = gui.creditorForSlot(event.getSlot());
        if (creditorId != null) {
            openAmount(player, gui.kingdomId(), creditorId);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WarDebtGui) {
            event.setCancelled(true);
        }
    }

    private void openAmount(Player player, String kingdomId, String creditorId) {
        double owed = tribute.debtOwed(kingdomId, creditorId);
        if (owed <= 0) {
            RealmFeedback.refuse(player, "Your realm owes that kingdom no war debt.");
            open(player);
            return;
        }
        String creditor = display(creditorId);
        List<AmountPickGui.Preset> presets = new ArrayList<>();
        presets.add(new AmountPickGui.Preset("All owed", owed));
        if (owed >= 4) {
            presets.add(new AmountPickGui.Preset("Half", Math.floor(owed / 2)));
            presets.add(new AmountPickGui.Preset("A quarter", Math.floor(owed / 4)));
        }
        player.openInventory(Objects.requireNonNull(AmountPickGui.create(
                        "&6Pay " + creditor,
                        "War debt owed to " + creditor,
                        List.of(
                                "Outstanding: " + TributeDesk.formatCorona(owed) + " Corona.",
                                "Treasury: " + TributeDesk.formatCorona(economyService.getTreasuryBalance(kingdomId))
                                        + " Corona.",
                                "Paid from the treasury, up to what it holds."),
                        presets,
                        "Type how much war debt to pay " + creditor + ", in Corona.",
                        (actor, amount) -> pay(actor, creditorId, amount),
                        this::open)
                .getInventory()));
    }

    private void pay(Player player, String creditorId, double amount) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            RealmFeedback.refuse(player, "You must join a kingdom first.");
            return;
        }
        TributeDesk.Outcome outcome = desk.pay(
                membership.get().getKingdomId(), membership.get().getRank(), creditorId, OptionalDouble.of(amount));
        if (!outcome.success()) {
            RealmFeedback.refuse(player, outcome.message());
            return;
        }
        economyStore.saveFrom(economyService);
        player.sendMessage(c("&a" + display(creditorId) + ": " + outcome.message()));
        RealmFeedback.success(player.getLocation());
    }

    private String display(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }
}
