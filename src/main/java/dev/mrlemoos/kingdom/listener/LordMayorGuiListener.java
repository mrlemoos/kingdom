package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.AllegianceOath;
import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.LordMayorCounter;
import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.city.gui.CityStatistics;
import dev.mrlemoos.kingdom.city.gui.OathGui;
import dev.mrlemoos.kingdom.city.gui.PermitApplicantStatus;
import dev.mrlemoos.kingdom.city.gui.PermitApplyGui;
import dev.mrlemoos.kingdom.city.gui.PermitRegisterGui;
import dev.mrlemoos.kingdom.city.gui.PermitRegisterLayout;
import dev.mrlemoos.kingdom.city.gui.PermitRevokeConfirmGui;
import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The city hall counter. Unaffiliated players swear the oath of allegiance; members apply for a
 * build permit. The monarch or a prince opens the permit register.
 */
public final class LordMayorGuiListener implements Listener {

    private final LordMayorService lordMayorService;
    private final CityService cityService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final EconomyService economyService;
    private final RealmWealthRates realmWealthRates;
    private final NoblePrefixDisplay nobleDisplay;

    public LordMayorGuiListener(
            LordMayorService lordMayorService,
            CityService cityService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            EconomyService economyService,
            RealmWealthRates realmWealthRates) {
        this(lordMayorService, cityService, kingdomService, store, economyService, realmWealthRates, null);
    }

    public LordMayorGuiListener(
            LordMayorService lordMayorService,
            CityService cityService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            EconomyService economyService,
            RealmWealthRates realmWealthRates,
            NoblePrefixDisplay nobleDisplay) {
        this.lordMayorService = Objects.requireNonNull(lordMayorService, "lordMayorService");
        this.cityService = Objects.requireNonNull(cityService, "cityService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = store;
        this.economyService = economyService;
        this.realmWealthRates = realmWealthRates;
        this.nobleDisplay = nobleDisplay;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractMayor(PlayerInteractEntityEvent event) {
        // The off-hand event fires in the same tick and would re-open the freshly opened GUI, which
        // the client reads as an immediate close.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
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

        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        switch (LordMayorCounter.action(membership, kingdomId.get())) {
            case REGISTER -> openRegister(player, kingdomId.get(), 0);
            case OATH -> openOath(player, kingdom.get());
            case FOREIGN_MEMBER -> player.sendMessage(
                    c("&cYou already belong to a kingdom. Ask an operator to move you."));
            case APPLY -> openApply(player, kingdom.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOathClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OathGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isDeclineSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        if (!gui.isSwearSlot(event.getSlot())) {
            return;
        }

        Optional<Kingdom> kingdom = kingdomService.getKingdom(gui.kingdomId());
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cThis Lord Mayor serves no kingdom."));
            player.closeInventory();
            return;
        }
        String addressee = addresseeOf(kingdom.get());
        CityResult result = cityService.swearAllegiance(gui.kingdomId(), player.getUniqueId());
        player.closeInventory();
        if (!(result instanceof CityResult.Success)) {
            player.sendMessage(c("&c" + result.message()));
            return;
        }
        String displayName = kingdom.get().getDisplayName();
        player.sendMessage(c("&a" + AllegianceOath.swearerMessage(addressee, displayName)));
        String announcement = "&6" + AllegianceOath.realmAnnouncement(player.getName(), addressee);
        for (Player member : RealmFeedback.onlineMembers(kingdomService, gui.kingdomId())) {
            if (!member.getUniqueId().equals(player.getUniqueId())) {
                member.sendMessage(c(announcement));
            }
        }
        RealmFeedback.oathOfAllegiance(player);
        giveOathBook(player, addressee);
        save();
        if (nobleDisplay != null) {
            nobleDisplay.refresh(player);
        }
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
        if (!isRegistrar(gui.kingdomId(), player.getUniqueId())) {
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
        if (!isRegistrar(gui.kingdomId(), player.getUniqueId())) {
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
            // One act of the Crown: the licence and the stable are taken together.
            cityService.revokeHorsePermits(gui.holderId());
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
                || holder instanceof PermitRevokeConfirmGui
                || holder instanceof OathGui) {
            event.setCancelled(true);
        }
    }

    private void openOath(Player player, Kingdom kingdom) {
        player.openInventory(OathGui.create(kingdom.getId(), player.getName(), addresseeOf(kingdom)).getInventory());
    }

    private void openApply(Player player, Kingdom kingdom) {
        PermitApplicantStatus status = statusOf(kingdom.getId(), player.getUniqueId());
        player.openInventory(
                PermitApplyGui.create(kingdom.getId(), kingdom.getDisplayName(), status).getInventory());
    }

    private void openRegister(Player player, String kingdomId, int page) {
        List<PermitRegisterGui.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Long> permit : cityService.permitsView(kingdomId).entrySet()) {
            entries.add(new PermitRegisterGui.Entry(
                    permit.getKey(),
                    permit.getValue(),
                    cityService.horsePermitCount(kingdomId, permit.getKey())));
        }
        entries.sort(Comparator.comparingLong(PermitRegisterGui.Entry::grantedAtMs));
        player.openInventory(PermitRegisterGui
                .create(kingdomId, entries, page, System.currentTimeMillis(), statisticsOf(kingdomId, entries.size()))
                .getInventory());
    }

    /** The state of the realm as the register shows it, or null when no economy is running. */
    private CityStatistics statisticsOf(String kingdomId, int permitHolders) {
        if (economyService == null || realmWealthRates == null) {
            return null;
        }
        int members = (int) kingdomService.getMembershipsView().values().stream()
                .filter(m -> kingdomId.equals(m.getKingdomId()))
                .count();
        Map<UUID, ?> villagerWallets = economyService.villagerWallets().get(kingdomId);
        return new CityStatistics(
                members,
                permitHolders,
                villagerWallets == null ? 0 : villagerWallets.size(),
                economyService.getTreasuryBalance(kingdomId),
                economyService.getRealmWealth(kingdomId, realmWealthRates),
                economyService.getLastDailyGdp(kingdomId),
                economyService.getTotalTaxRevenue(kingdomId),
                economyService.getLastDayTradesSettled(kingdomId));
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
    private boolean isRegistrar(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent() && LordMayorCounter.isRegistrar(membership.get(), kingdomId);
    }

    private void notifyHolder(UUID holderId) {
        Player holder = Bukkit.getPlayer(holderId);
        if (holder != null && holder.isOnline()) {
            holder.sendMessage(c("&cYour build permit has been revoked by the Crown."));
        }
    }

    private String addresseeOf(Kingdom kingdom) {
        Optional<PlayerMembership> monarch = kingdomService.findMonarch(kingdom.getId());
        if (monarch.isEmpty()) {
            return AllegianceOath.addressee(null, kingdom.getDisplayName());
        }
        PlayerMembership seated = monarch.get();
        NobleRank rank = seated.getRank();
        if (rank == null) {
            return AllegianceOath.addressee(null, kingdom.getDisplayName());
        }
        TitleStyle style = seated.getTitleStyle() != null ? seated.getTitleStyle() : TitleStyle.MASCULINE;
        String titleAndName = rank.displayTitle(style) + " " + nameOf(seated.getPlayerId());
        return AllegianceOath.addressee(titleAndName, kingdom.getDisplayName());
    }

    private void giveOathBook(Player player, String addressee) {
        ItemStack book = new ItemBuilder(Material.WRITTEN_BOOK)
                .displayAs(c("&6Oath of Allegiance"))
                .book("Oath of Allegiance", player.getName(), List.of(AllegianceOath.words(player.getName(), addressee)))
                .build();
        for (ItemStack leftover : player.getInventory().addItem(book).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
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
