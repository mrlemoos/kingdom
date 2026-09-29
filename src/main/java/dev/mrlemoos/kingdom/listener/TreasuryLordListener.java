package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.NuggetDeposit;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.mint.TreasuryWithdrawGui;
import dev.mrlemoos.kingdom.mint.GiltMintGui;
import dev.mrlemoos.kingdom.mint.GiltOpenConfirmGui;
import dev.mrlemoos.kingdom.city.SeasonGazette;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.tribute.WarDebtStore;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadEntityService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class TreasuryLordListener implements Listener {

    private final TreasuryLordService treasuryLordService;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final YamlKingdomStore kingdomStore;
    private final CrownSquadService crownSquads;
    private final CrownSquadEntityService crownSquadEntities;
    private final WarService warService;
    private final Map<UUID, String> pendingCustomWithdrawals = new HashMap<>();
    private final Map<UUID, String> pendingGiltPurchases = new HashMap<>();
    private JavaPlugin plugin;
    private WarDebtStore warDebtStore;
    private java.util.function.LongSupplier financeDayClock = () -> 0L;

    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }
    public void setFinanceDayClock(java.util.function.LongSupplier clock) { financeDayClock = clock; }
    public void setWarDebtStore(WarDebtStore store) { warDebtStore = store; }

    public TreasuryLordListener(
            TreasuryLordService treasuryLordService,
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore) {
        this(treasuryLordService, economyService, kingdomService, economyStore, null, null, null, null);
    }

    public TreasuryLordListener(
            TreasuryLordService treasuryLordService,
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            YamlKingdomStore kingdomStore,
            CrownSquadService crownSquads,
            CrownSquadEntityService crownSquadEntities,
            WarService warService) {
        this.treasuryLordService = treasuryLordService;
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.economyStore = economyStore;
        this.kingdomStore = kingdomStore;
        this.crownSquads = crownSquads;
        this.crownSquadEntities = crownSquadEntities;
        this.warService = warService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractLord(PlayerInteractEntityEvent event) {
        // The off-hand event fires in the same tick and would re-open the freshly opened GUI, which
        // the client reads as an immediate close.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!treasuryLordService.isLordEntity(villager)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        Optional<String> mintKingdomId = treasuryLordService.kingdomIdForLord(villager);
        if (mintKingdomId.isEmpty()) {
            player.sendMessage(error("This treasury lord is not linked to a kingdom."));
            return;
        }

        Optional<MintLocation> mint = treasuryLordService.findMintForLord(villager);
        if (mint.isEmpty()) {
            player.sendMessage(error("This mint could not be found."));
            return;
        }

        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        boolean ownMint = membership.isPresent() && membership.get().getKingdomId().equals(mintKingdomId.get());
        if (!ownMint) {
            openGilts(player, mintKingdomId.get());
            return;
        }
        if (player.isSneaking()) {
            raiseCrownSquad(player, mintKingdomId.get(), mint.get(), villager.getLocation());
            return;
        }

        sendMintBriefing(player, mintKingdomId.get());
        openWithdrawGui(player, mintKingdomId.get());
    }

    private void raiseCrownSquad(Player player, String kingdomId, MintLocation mint, org.bukkit.Location location) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (crownSquads == null || crownSquadEntities == null || warService == null || !warService.config().enabled()) {
            player.sendMessage(error("War is not enabled for this realm."));
            return;
        }
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())
                || !RankAuthority.canRaiseCrownSquads(membership.get().getRank())) {
            player.sendMessage(error("Only the King or Queen may raise crown squads."));
            return;
        }
        if (!warService.isAtWar(kingdomId)) {
            player.sendMessage(error("Crown squads may be raised only during an active war."));
            return;
        }
        WarResult result = crownSquads.purchase(kingdomId);
        if (result instanceof WarResult.Failure failure) {
            player.sendMessage(error(failure.message()));
            return;
        }
        var unit = crownSquads.unitsOf(kingdomId).get(crownSquads.countOf(kingdomId) - 1);
        if (!crownSquadEntities.spawn(unit, location)) {
            crownSquads.destroyUnit(kingdomId, unit.unitId());
            player.sendMessage(error("Crown squad could not be raised."));
            return;
        }
        economyStore.saveFrom(economyService);
        if (kingdomStore != null) kingdomStore.saveFrom(kingdomService);
        player.sendMessage(success("Crown squad raised. " + ((WarResult.Success) result).message()));
    }

    /** The balances the mint lectern used to print, now read out by the Lord itself. */
    private void sendMintBriefing(Player player, String kingdomId) {
        double walletBalance = economyService.getWalletBalance(player.getUniqueId());
        double treasuryBalance = economyService.getTreasuryBalance(kingdomId);
        player.sendMessage("              ");
        player.sendMessage(c("&6Kingdom Mint"));
        player.sendMessage(c("&eYour wallet: ") + c("&f" + formatCorona(walletBalance)));
        player.sendMessage(c("&eTreasury: ") + c("&f" + formatCorona(treasuryBalance)));
        player.sendMessage(c("&eNational debt: &f" + String.format(java.util.Locale.UK, "%.3f", economyService.getNationalDebt(kingdomId))));
        player.sendMessage(c("&7Click Deposit to pay in the Corona nuggets you carry."));
        player.sendMessage("              ");
    }

    private static String formatCorona(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1e-9) {
            return String.format("%.0f Corona", amount);
        }
        return String.format("%.2f Corona", amount);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWithdrawGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TreasuryWithdrawGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        double balance = economyService.getWalletBalance(player.getUniqueId());
        if (gui.isGiltsSlot(event.getRawSlot())) {
            openGilts(player, gui.kingdomId());
            return;
        }
        if (gui.isDepositSlot(event.getRawSlot())) {
            player.closeInventory();
            deposit(player, gui.kingdomId());
            return;
        }
        if (gui.isCustomSlot(event.getRawSlot())) {
            player.closeInventory();
            pendingCustomWithdrawals.put(player.getUniqueId(), gui.kingdomId());
            player.sendMessage(info("Type the whole Corona amount to withdraw in chat, or type "
                    + c("&ecancel") + c("&b.")));
            return;
        }

        Integer amount = gui.amountForSlot(event.getRawSlot(), balance);
        if (amount == null || amount <= 0) {
            return;
        }

        attemptWithdraw(player, gui.kingdomId(), amount);
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomWithdrawChat(AsyncPlayerChatEvent event) {
        String giltKingdom = pendingGiltPurchases.remove(event.getPlayer().getUniqueId());
        if (giltKingdom != null) {
            event.setCancelled(true);
            String amount = event.getMessage().trim();
            if (!amount.equalsIgnoreCase("cancel") && plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> buyGilt(event.getPlayer(), giltKingdom, amount));
            }
            return;
        }
        String kingdomId = pendingCustomWithdrawals.remove(event.getPlayer().getUniqueId());
        if (kingdomId == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage(info("Withdrawal cancelled."));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException ex) {
            event.getPlayer().sendMessage(error("Enter a whole number of Corona, or type cancel."));
            pendingCustomWithdrawals.put(event.getPlayer().getUniqueId(), kingdomId);
            return;
        }

        attemptWithdraw(event.getPlayer(), kingdomId, amount);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingCustomWithdrawals.remove(event.getPlayer().getUniqueId());
        pendingGiltPurchases.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGiltClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) ||
                event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (event.getInventory().getHolder() instanceof GiltMintGui gui) {
            event.setCancelled(true);
            if (event.getRawSlot() == GiltMintGui.SLOT_PREVIOUS) {
                openGilts(player, gui.kingdomId(), gui.page() - 1);
            } else if (event.getRawSlot() == GiltMintGui.SLOT_NEXT) {
                openGilts(player, gui.kingdomId(), gui.page() + 1);
            } else if (event.getRawSlot() == GiltMintGui.SLOT_BUY && event.getCurrentItem() != null) {
                pendingGiltPurchases.put(player.getUniqueId(), gui.kingdomId());
                player.closeInventory();
                player.sendMessage(info("Type a whole Corona gilt amount in chat, or cancel."));
            } else if (event.getRawSlot() == GiltMintGui.SLOT_OPEN && isCrown(player, gui.kingdomId())) {
                player.openInventory(GiltOpenConfirmGui.create(gui.kingdomId(), financeDay() + 7).getInventory());
            }
        } else if (event.getInventory().getHolder() instanceof GiltOpenConfirmGui gui) {
            event.setCancelled(true);
            if (event.getRawSlot() != GiltOpenConfirmGui.SLOT_CONFIRM || !isCrown(player, gui.kingdomId())) return;
            var result = economyService.openGiltSale(gui.kingdomId(), player.getUniqueId(), financeDay());
            if (result instanceof dev.mrlemoos.kingdom.economy.service.EconomyResult.Success accepted) {
                player.sendMessage(success(accepted.message()));
                kingdomService.getKingdom(gui.kingdomId()).ifPresent(kingdom -> kingdom.getCityState().addGazettePost(
                        new GazettePost("Gilt sale opened",
                                "The Crown has opened a gilt sale at its mint. Buyers of any realm may visit the Lord of the Treasury.",
                                SeasonGazette.REALM_AUTHOR, financeDay(), GazettePostKind.ANNOUNCEMENT,
                                Optional.empty())));
                if (kingdomStore != null) kingdomStore.saveFrom(kingdomService);
            } else if (result instanceof dev.mrlemoos.kingdom.economy.service.EconomyResult.Failure rejected) {
                player.sendMessage(error(rejected.message()));
            }
            economyStore.saveFrom(economyService);
            openGilts(player, gui.kingdomId());
        }
    }

    private void openGilts(Player player, String kingdomId) {
        openGilts(player, kingdomId, 0);
    }

    private void openGilts(Player player, String kingdomId, int page) {
        player.openInventory(GiltMintGui.create(kingdomId, player.getUniqueId(), economyService,
                isCrown(player, kingdomId), page,
                warDebtStore == null ? 0.0 : warDebtStore.totalDebtOwed(kingdomId)).getInventory());
    }

    private boolean isCrown(Player player, String kingdomId) {
        return kingdomService.getMembership(player.getUniqueId())
                .filter(member -> kingdomId.equals(member.getKingdomId()))
                .filter(member -> member.getRank() == dev.mrlemoos.kingdom.model.NobleRank.KING
                        || member.getRank() == dev.mrlemoos.kingdom.model.NobleRank.QUEEN).isPresent();
    }

    private long financeDay() {
        return financeDayClock.getAsLong();
    }

    private boolean atIssuingMint(Player player, String kingdomId) {
        var account = economyService.kingdomEconomies().get(kingdomId);
        if (account == null || player.getWorld() == null) return false;
        return account.mintLocations().stream().anyMatch(mint -> mint.worldName().equals(player.getWorld().getName())
                && Math.pow(player.getLocation().getX() - mint.x(), 2)
                        + Math.pow(player.getLocation().getY() - mint.y(), 2)
                        + Math.pow(player.getLocation().getZ() - mint.z(), 2) <= 64);
    }

    private void buyGilt(Player player, String kingdomId, String amount) {
        if (!atIssuingMint(player, kingdomId)) {
            player.sendMessage(error("Return to the issuing mint to buy gilts."));
            return;
        }
        try {
            int face = Integer.parseInt(amount);
            var result = economyService.buyGilt(kingdomId, player.getUniqueId(), face, financeDay());
            if (result instanceof dev.mrlemoos.kingdom.economy.service.EconomyResult.Success accepted) {
                player.sendMessage(success(accepted.message()));
            } else if (result instanceof dev.mrlemoos.kingdom.economy.service.EconomyResult.Failure rejected) {
                player.sendMessage(error(rejected.message()));
            }
            economyStore.saveFrom(economyService);
            openGilts(player, kingdomId);
        } catch (NumberFormatException rejected) {
            pendingGiltPurchases.put(player.getUniqueId(), kingdomId);
            player.sendMessage(error("Type a whole Corona amount, or cancel."));
        }
    }

    private void openWithdrawGui(Player player, String kingdomId) {
        double balance = economyService.getWalletBalance(player.getUniqueId());
        TreasuryWithdrawGui gui =
                TreasuryWithdrawGui.create(kingdomId, balance, CoronaItem.count(player.getInventory()));
        player.openInventory(gui.getInventory());
    }

    /** Deposit sits beside withdrawal: every Corona nugget carried goes into the wallet. */
    private void deposit(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(kingdomId)) {
            RealmFeedback.refuse(player, "You may only deposit at your own kingdom's mint.");
            return;
        }
        int deposited = NuggetDeposit.depositAll(player.getInventory(), economyService, player.getUniqueId());
        if (deposited <= 0) {
            RealmFeedback.refuse(player, "You carry no Corona nuggets to deposit.");
            return;
        }
        economyStore.saveFrom(economyService);
        player.sendMessage(success("Deposited " + deposited + " Corona. Your wallet: "
                + formatCorona(economyService.getWalletBalance(player.getUniqueId())) + "."));
        RealmFeedback.success(player.getLocation());
    }

    private void attemptWithdraw(Player player, String kingdomId, int amount) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(kingdomId)) {
            player.sendMessage(error("You may only withdraw at your own kingdom's mint."));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(error("Withdrawal amount must be positive."));
            return;
        }

        double balance = economyService.getWalletBalance(player.getUniqueId());
        if (balance < amount) {
            player.sendMessage(error("Insufficient Corona for that withdrawal."));
            return;
        }
        if (!CoronaItem.hasSpace(player.getInventory(), amount)) {
            player.sendMessage(error("Not enough inventory space for gold nuggets."));
            return;
        }

        if (!economyService.withdrawWholeCorona(player.getUniqueId(), amount)) {
            player.sendMessage(error("Withdrawal failed."));
            return;
        }

        CoronaItem.give(player.getInventory(), amount);
        economyStore.saveFrom(economyService);
        player.sendMessage(success("Withdrew " + amount + " Corona as gold nuggets."));
    }

    private static String success(String message) {
        return c("&a" + message);
    }

    private static String error(String message) {
        return c("&c" + message);
    }

    private static String info(String message) {
        return c("&b" + message);
    }
}
