package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.mint.TreasuryWithdrawGui;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadEntityService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
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

        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom to withdraw Corona."));
            return;
        }
        if (!membership.get().getKingdomId().equals(mintKingdomId.get())) {
            player.sendMessage(error("You may only withdraw at your own kingdom's mint."));
            return;
        }

        Optional<MintLocation> mint = treasuryLordService.findMintForLord(villager);
        if (mint.isEmpty()) {
            player.sendMessage(error("This mint could not be found."));
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
        player.sendMessage(c("&7Use ") + c("&e/corona deposit") + c("&7 to convert gold ingots."));
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
    }

    private void openWithdrawGui(Player player, String kingdomId) {
        double balance = economyService.getWalletBalance(player.getUniqueId());
        TreasuryWithdrawGui gui = TreasuryWithdrawGui.create(kingdomId, balance);
        player.openInventory(gui.getInventory());
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
