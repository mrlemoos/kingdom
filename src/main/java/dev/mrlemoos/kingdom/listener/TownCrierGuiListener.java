package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.calendar.PollingDay;
import dev.mrlemoos.kingdom.calendar.RealmCalendar;
import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.city.CapitalSitingPolicy;
import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.CurfewPresets;
import dev.mrlemoos.kingdom.city.GazetteService;
import dev.mrlemoos.kingdom.city.TownCrierService;
import dev.mrlemoos.kingdom.city.gui.ComposeGui;
import dev.mrlemoos.kingdom.city.gui.GazetteGui;
import dev.mrlemoos.kingdom.city.gui.GazetteLayout;
import dev.mrlemoos.kingdom.city.gui.GazetteLiveState;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Right-click the Town Crier to read the Gazette; the Crown holding a signed book opens compose.
 */
public final class TownCrierGuiListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final TownCrierService townCrierService;
    private final GazetteService gazetteService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final EconomyService economyService;
    private final MechanicalJusticeService justiceService;
    private final RealmCalendarService calendarService;
    private final PollingDay pollingDay;

    public TownCrierGuiListener(
            TownCrierService townCrierService,
            GazetteService gazetteService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            EconomyService economyService,
            MechanicalJusticeService justiceService,
            RealmCalendarService calendarService,
            PollingDay pollingDay) {
        this.townCrierService = Objects.requireNonNull(townCrierService, "townCrierService");
        this.gazetteService = Objects.requireNonNull(gazetteService, "gazetteService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = store;
        this.economyService = economyService;
        this.justiceService = justiceService;
        this.calendarService = calendarService;
        this.pollingDay = pollingDay;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractCrier(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!townCrierService.isTownCrier(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<String> kingdomId = townCrierService.kingdomIdOf(event.getRightClicked());
        if (kingdomId.isEmpty()) {
            player.sendMessage(c("&cThis Town Crier serves no kingdom."));
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId.get());
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cThis Town Crier serves no kingdom."));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isSignedBook(hand) && isCrownOf(kingdomId.get(), player.getUniqueId())) {
            Optional<BookContent> book = readBook(hand);
            if (book.isEmpty()) {
                player.sendMessage(c("&cThe book needs a title and pages."));
                return;
            }
            player.openInventory(Objects.requireNonNull(
                    ComposeGui.kind(kingdomId.get(), book.get().title(), book.get().body()).getInventory()));
            return;
        }

        openGazette(player, kingdom.get(), 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComposeClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ComposeGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ComposeGui.Choice choice = gui.choiceForSlot(event.getSlot());
        if (choice == null) {
            return;
        }
        switch (choice) {
            case CANCEL -> player.closeInventory();
            case BACK -> player.openInventory(Objects.requireNonNull(
                    ComposeGui.kind(gui.kingdomId(), gui.bookTitle(), gui.bookBody()).getInventory()));
            case ANNOUNCEMENT -> publishAnnouncement(player, gui);
            case DECREE -> player.openInventory(Objects.requireNonNull(
                    ComposeGui.curfew(gui.kingdomId(), gui.bookTitle(), gui.bookBody()).getInventory()));
            case CURFEW_DUSK_DAWN -> publishDecree(player, gui, Optional.of(CurfewPresets.duskDawn()));
            case CURFEW_NIGHTFALL_MIDNIGHT -> publishDecree(
                    player, gui, Optional.of(CurfewPresets.nightfallMidnight()));
            case CURFEW_LIFT -> publishDecree(player, gui, Optional.of(CurfewPresets.lifted()));
            case CURFEW_NONE -> publishDecree(player, gui, Optional.empty());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGazetteClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GazetteGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(gui.kingdomId());
        if (kingdom.isEmpty()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == GazetteLayout.SLOT_PREVIOUS) {
            openGazette(player, kingdom.get(), gui.page() - 1);
            return;
        }
        if (slot == GazetteLayout.SLOT_NEXT) {
            openGazette(player, kingdom.get(), gui.page() + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder instanceof GazetteGui || holder instanceof ComposeGui) {
            event.setCancelled(true);
        }
    }

    private void publishAnnouncement(Player player, ComposeGui gui) {
        long mcDay = mcDayOf(player);
        CityResult result = gazetteService.publishAnnouncement(
                gui.kingdomId(), player.getUniqueId(), gui.bookTitle(), gui.bookBody(), mcDay);
        finishPublish(player, result);
    }

    private void publishDecree(
            Player player, ComposeGui gui, Optional<CurfewEnforcementConfig> curfew) {
        long mcDay = mcDayOf(player);
        CityResult result = gazetteService.publishDecree(
                gui.kingdomId(),
                player.getUniqueId(),
                gui.bookTitle(),
                gui.bookBody(),
                mcDay,
                curfew);
        finishPublish(player, result);
    }

    private void finishPublish(Player player, CityResult result) {
        if (result instanceof CityResult.Success) {
            consumeSignedBook(player);
            player.sendMessage(c("&a" + result.message()));
            save();
        } else {
            player.sendMessage(c("&c" + result.message()));
        }
        player.closeInventory();
    }

    private void openGazette(Player player, Kingdom kingdom, int page) {
        GazetteLiveState live = liveStateOf(kingdom);
        player.openInventory(Objects.requireNonNull(GazetteGui.create(
                        kingdom.getId(), kingdom.getCityState().gazettePostsView(), live, page)
                .getInventory()));
    }

    private GazetteLiveState liveStateOf(Kingdom kingdom) {
        String openBill = "";
        Optional<Bill> bill = kingdom.getParliamentState().currentBill();
        if (bill.isPresent()) {
            openBill = bill.get().title();
        }
        String nextElection = nextElectionLabel(kingdom);
        int wanted = 0;
        if (justiceService != null) {
            for (Warrant warrant : justiceService.warrantsView()) {
                if (!kingdom.getId().equals(warrant.kingdomId())) {
                    continue;
                }
                WarrantStatus status = warrant.status();
                if (status == WarrantStatus.ACTIVE || status == WarrantStatus.PENDING_CROWN) {
                    wanted++;
                }
            }
        }
        double treasury = economyService == null ? 0d : economyService.getTreasuryBalance(kingdom.getId());
        return new GazetteLiveState(
                openBill,
                nextElection,
                wanted,
                kingdom.getCityState().permitCount(),
                treasury);
    }

    private String nextElectionLabel(Kingdom kingdom) {
        var election = kingdom.getElectionState().election();
        if (election.isActive()) {
            return "election in progress (" + election.phase().name().toLowerCase() + ")";
        }
        if (calendarService == null || pollingDay == null) {
            return "none proclaimed";
        }
        long realmDay = calendarService.currentRealmDay();
        var today = RealmCalendar.dateOf(realmDay);
        long thisYear = pollingDay.dayInYear(today.realmYear());
        long next = realmDay <= thisYear ? thisYear : pollingDay.dayInYear(today.realmYear() + 1);
        var date = RealmCalendar.dateOf(next);
        return date.format() + ", Realm Year " + date.realmYear();
    }

    private boolean isCrownOf(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return false;
        }
        NobleRank rank = membership.get().getRank();
        return rank != null && CapitalSitingPolicy.isCrown(rank);
    }

    private static boolean isSignedBook(ItemStack stack) {
        return stack != null
                && stack.getType() == Material.WRITTEN_BOOK
                && stack.getItemMeta() instanceof BookMeta;
    }

    private static Optional<BookContent> readBook(ItemStack stack) {
        if (!(stack.getItemMeta() instanceof BookMeta meta)) {
            return Optional.empty();
        }
        String title = meta.getTitle();
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        List<String> pages = new ArrayList<>();
        for (var page : meta.pages()) {
            String plain = PLAIN.serialize(page).trim();
            if (!plain.isEmpty()) {
                pages.add(plain);
            }
        }
        if (pages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BookContent(title.trim(), String.join("\n", pages)));
    }

    private static void consumeSignedBook(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSignedBook(hand)) {
            return;
        }
        int amount = hand.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amount - 1);
        }
    }

    private long mcDayOf(Player player) {
        if (player.getWorld() == null) {
            return 0L;
        }
        return player.getWorld().getFullTime() / 24_000L;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }

    private record BookContent(String title, String body) {}
}
