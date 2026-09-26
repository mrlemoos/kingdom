package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.helpers.AmountPickGui;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.police.CourtProximity;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.WarrantDesk;
import dev.mrlemoos.kingdom.police.gui.WarrantCancelConfirmGui;
import dev.mrlemoos.kingdom.police.gui.WarrantListGui;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Warrant business where it is done: an arrest reward is posted at the court — right-click its
 * lectern, or sneak and right-click the judge — and the Crown cancels warrants from the warrant
 * register in the Realm Hub. Every act is {@link WarrantDesk}'s.
 */
public final class WarrantDeskListener implements Listener {

    private static final List<AmountPickGui.Preset> REWARD_PRESETS = List.of(
            new AmountPickGui.Preset("10 Corona", 10),
            new AmountPickGui.Preset("25 Corona", 25),
            new AmountPickGui.Preset("50 Corona", 50),
            new AmountPickGui.Preset("100 Corona", 100),
            new AmountPickGui.Preset("250 Corona", 250),
            new AmountPickGui.Preset("500 Corona", 500));
    private static final String REFUSAL_FOREIGN = "The court hears only the subjects of this realm.";
    private static final String REFUSAL_NOT_CROWN = "Only the King or Queen may cancel a warrant.";

    private final KingdomService kingdomService;
    private final WarrantDesk desk;
    private final PoliceCourtService courtService;
    private Consumer<Player> policeSectionOpener;

    public WarrantDeskListener(KingdomService kingdomService, WarrantDesk desk, PoliceCourtService courtService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.desk = Objects.requireNonNull(desk, "desk");
        this.courtService = Objects.requireNonNull(courtService, "courtService");
    }

    /** Where Back leads from the register: the Hub's Police section. */
    public WarrantDeskListener withPoliceSectionOpener(Consumer<Player> policeSectionOpener) {
        this.policeSectionOpener = policeSectionOpener;
        return this;
    }

    // --- the court -------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLectern(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LECTERN) {
            return;
        }
        // A book in hand or on the lectern is for reading; leave it to vanilla.
        ItemStack held = event.getItem();
        if (held != null && (held.getType() == Material.WRITABLE_BOOK || held.getType() == Material.WRITTEN_BOOK)) {
            return;
        }
        if (block.getState() instanceof Lectern lectern && !lectern.getInventory().isEmpty()) {
            return;
        }
        Optional<String> kingdomId = courtOf(block);
        if (kingdomId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        openRewardWindow(event.getPlayer(), kingdomId.get(), 0);
    }

    /** Sneak and right-click the judge; a plain click still opens the morale pardon roll. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJudge(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getPlayer().isSneaking()) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager) || !courtService.isJudgeEntity(villager)) {
            return;
        }
        Optional<String> kingdomId = courtService.kingdomIdForJudge(villager);
        if (kingdomId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        openRewardWindow(event.getPlayer(), kingdomId.get(), 0);
    }

    private Optional<String> courtOf(Block lectern) {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            Optional<CourtLocation> court = kingdom.getPoliceState().court();
            if (court.isPresent()
                    && court.get().worldName().equals(lectern.getWorld().getName())
                    && CourtProximity.isCourtLectern(
                            court.get().x(), court.get().y(), court.get().z(),
                            lectern.getX(), lectern.getY(), lectern.getZ())) {
                return Optional.of(kingdom.getId());
            }
        }
        return Optional.empty();
    }

    /** The realm's active warrants, to back one with an arrest reward. */
    public void openRewardWindow(Player player, String kingdomId, int page) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            RealmFeedback.refuse(player, REFUSAL_FOREIGN);
            return;
        }
        List<Warrant> active = desk.activeWarrants(kingdomId);
        if (active.isEmpty()) {
            RealmFeedback.refuse(player, "No warrant is out in this realm.");
            return;
        }
        player.openInventory(Objects.requireNonNull(
                WarrantListGui.create(WarrantListGui.Purpose.REWARD, kingdomId, active, page).getInventory()));
    }

    private void openRewardAmount(Player player, String kingdomId, Warrant warrant) {
        String name = WarrantListGui.suspectName(warrant);
        player.openInventory(Objects.requireNonNull(AmountPickGui.create(
                        "&6Reward for " + name,
                        "Arrest reward on " + name,
                        List.of(
                                "Paid from your wallet and held on the warrant.",
                                "A constable who arrests them is paid it;",
                                "it comes back to you if the warrant ends otherwise."),
                        REWARD_PRESETS,
                        "Type the arrest reward for " + name + " in Corona.",
                        (actor, amount) -> postReward(actor, kingdomId, warrant, amount),
                        actor -> openRewardWindow(actor, kingdomId, 0))
                .getInventory()));
    }

    private void postReward(Player player, String kingdomId, Warrant warrant, double amount) {
        if (!desk.nearCourt(player.getLocation(), kingdomId)) {
            RealmFeedback.refuse(player, "Post arrest rewards at the court.");
            return;
        }
        PoliceResult result = desk.postReward(kingdomId, player.getUniqueId(), warrant.suspectId(), amount);
        answer(player, result);
    }

    // --- the Crown's register --------------------------------------------

    /** The warrant register, for the Crown to cancel a warrant. */
    public void openRegister(Player player, int page) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !RankAuthority.isCrown(membership.get().getRank())) {
            RealmFeedback.refuse(player, REFUSAL_NOT_CROWN);
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        player.openInventory(Objects.requireNonNull(WarrantListGui.create(
                        WarrantListGui.Purpose.CANCEL, kingdomId, desk.activeWarrants(kingdomId), page)
                .getInventory()));
    }

    public void openRegister(Player player) {
        openRegister(player, 0);
    }

    // --- clicks ----------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onListClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WarrantListGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        boolean register = gui.purpose() == WarrantListGui.Purpose.CANCEL;
        if (slot == WarrantListGui.SLOT_PREVIOUS && gui.hasPrevious()) {
            reopen(player, gui, gui.page() - 1);
            return;
        }
        if (slot == WarrantListGui.SLOT_NEXT && gui.hasNext()) {
            reopen(player, gui, gui.page() + 1);
            return;
        }
        if (slot == WarrantListGui.SLOT_BACK && register) {
            if (policeSectionOpener != null) {
                policeSectionOpener.accept(player);
            } else {
                player.closeInventory();
            }
            return;
        }
        String warrantId = gui.warrantForSlot(slot);
        if (warrantId == null) {
            return;
        }
        Optional<Warrant> warrant = desk.activeWarrant(gui.kingdomId(), warrantId);
        if (warrant.isEmpty()) {
            RealmFeedback.refuse(player, "That warrant is no longer active.");
            reopen(player, gui, gui.page());
            return;
        }
        if (register) {
            player.openInventory(Objects.requireNonNull(WarrantCancelConfirmGui.create(
                            gui.kingdomId(), warrantId, WarrantListGui.suspectName(warrant.get()), gui.page())
                    .getInventory()));
            return;
        }
        openRewardAmount(player, gui.kingdomId(), warrant.get());
    }

    private void reopen(Player player, WarrantListGui gui, int page) {
        if (gui.purpose() == WarrantListGui.Purpose.CANCEL) {
            openRegister(player, page);
        } else {
            openRewardWindow(player, gui.kingdomId(), page);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WarrantCancelConfirmGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() == WarrantCancelConfirmGui.SLOT_BACK) {
            openRegister(player, gui.registerPage());
            return;
        }
        if (event.getSlot() != WarrantCancelConfirmGui.SLOT_CONFIRM) {
            return;
        }
        PoliceResult result = desk.cancel(gui.kingdomId(), player.getUniqueId(), gui.warrantId());
        answer(player, result);
        if (result instanceof PoliceResult.Success) {
            openRegister(player, gui.registerPage());
        } else {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WarrantListGui
                || event.getInventory().getHolder() instanceof WarrantCancelConfirmGui) {
            event.setCancelled(true);
        }
    }

    /** Success kept in chat and heard and seen where it happened; a refusal heard, not seen. */
    private static void answer(Player player, PoliceResult result) {
        switch (result) {
            case PoliceResult.Success success -> {
                player.sendMessage(c("&a" + success.message()));
                RealmFeedback.success(player.getLocation());
            }
            case PoliceResult.Failure failure -> RealmFeedback.refuse(player, failure.message());
        }
    }
}
