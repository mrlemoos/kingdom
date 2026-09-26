package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchConsentBook;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchRites;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.ClericAudience;
import dev.mrlemoos.kingdom.church.ClericService;
import dev.mrlemoos.kingdom.church.FuneralOutcome;
import dev.mrlemoos.kingdom.church.Rite;
import dev.mrlemoos.kingdom.church.RitesEligibility;
import dev.mrlemoos.kingdom.church.VillagerFuneralOutcome;
import dev.mrlemoos.kingdom.church.gui.CoronationGui;
import dev.mrlemoos.kingdom.church.gui.OathOfServiceGui;
import dev.mrlemoos.kingdom.church.gui.RiteConfirmGui;
import dev.mrlemoos.kingdom.church.gui.RitePickGui;
import dev.mrlemoos.kingdom.church.gui.RitesGui;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.command.KingdomChurchHandler;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.church.Marriage;
import dev.mrlemoos.kingdom.police.PoliceAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.oath.OathResult;
import dev.mrlemoos.kingdom.war.oath.OathService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantInventory;

/**
 * The cleric keeps no shop. A right-click opens the rites window with every rite the clicker may ask
 * for, beside the Coronation for the King, Queen or a Prince of the realm the cleric serves and the
 * oath of service; with no rite to offer it opens the Coronation or the oath directly, as before. A
 * request for consent waiting on the clicker is answered first. The vanilla trading window never
 * opens.
 */
public final class ClericGuiListener implements Listener {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final ClericService clericService;
    private final YamlKingdomStore store;
    private final OathService oathService;
    private final ChurchRites rites;

    public ClericGuiListener(
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService,
            YamlKingdomStore store,
            OathService oathService,
            ChurchRites rites) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.clericService = Objects.requireNonNull(clericService, "clericService");
        this.store = Objects.requireNonNull(store, "store");
        this.oathService = Objects.requireNonNull(oathService, "oathService");
        this.rites = Objects.requireNonNull(rites, "rites");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractCleric(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !clericService.isCleric(event.getRightClicked())) {
            return;
        }
        // Cancelled whoever clicks: the cleric's trades are not for sale.
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<String> kingdomId = clericService.kingdomIdOf(event.getRightClicked());
        if (kingdomId.isEmpty()) {
            player.sendMessage(c("&cThis cleric serves no kingdom."));
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId.get());
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cThis cleric serves no kingdom."));
            return;
        }
        // Somebody's request waits on this player's answer: that comes before anything else.
        if (rites.openConsent(player)) {
            return;
        }
        openRites(player, kingdom.get());
    }

    /** The rites window, or — with no rite to offer — the Coronation or the oath, as before. */
    private void openRites(Player player, Kingdom kingdom) {
        String kingdomId = kingdom.getId();
        List<Rite> offered = ritesFor(player, kingdomId);
        ClericAudience audience = ClericAudience.of(
                isRoyalOf(kingdomId, player.getUniqueId()),
                oathService.config().enabled(),
                ChurchPresence.atChurch(churchService, kingdomId, player));
        if (!offered.isEmpty()) {
            List<RitesGui.Choice> choices = new ArrayList<>();
            if (audience == ClericAudience.CORONATION) {
                choices.add(RitesGui.Choice.CORONATION);
            } else if (audience == ClericAudience.OATH) {
                choices.add(RitesGui.Choice.OATH);
            }
            for (Rite rite : offered) {
                choices.add(RitesGui.Choice.of(rite));
            }
            player.openInventory(RitesGui.create(
                            kingdomId,
                            kingdom.getDisplayName(),
                            choices,
                            churchService.villagersAwaitingRites(kingdomId))
                    .getInventory());
            return;
        }
        if (audience == ClericAudience.AT_PRAYER) {
            player.sendMessage(c("&7The cleric is at prayer, and keeps no trades."));
            return;
        }
        if (audience == ClericAudience.AWAY_FROM_CHURCH) {
            player.sendMessage(c("&cStand at the church to swear the oath of service."));
            return;
        }
        if (audience == ClericAudience.OATH) {
            openOath(player, kingdom);
            return;
        }
        openCoronation(player, kingdom);
    }

    /** The Crown's coronation is no part of the oath, and waits on no war flag. */
    private void openCoronation(Player player, Kingdom kingdom) {
        String kingdomId = kingdom.getId();
        Optional<UUID> monarch = kingdomService.findMonarch(kingdomId).map(PlayerMembership::getPlayerId);
        String monarchName = monarch.isPresent() ? Bukkit.getOfflinePlayer(monarch.get()).getName() : null;
        boolean crowned = monarch.isPresent() && churchService.isCrowned(kingdomId, monarch.get());
        player.openInventory(CoronationGui.create(kingdomId, kingdom.getDisplayName(), monarchName, crowned)
                .getInventory());
    }

    // --- the rites window -------------------------------------------------

    /** Every rite this player may ask the cleric of {@code kingdomId} for, and no other. */
    private List<Rite> ritesFor(Player player, String kingdomId) {
        UUID playerId = player.getUniqueId();
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        boolean member = membership.isPresent() && kingdomId.equals(membership.get().getKingdomId());
        return RitesEligibility.builder()
                .member(member)
                .atChurch(ChurchPresence.atChurch(churchService, kingdomId, player))
                .consecrated(churchService.isConsecrated(kingdomId))
                .crown(member && PoliceAuthority.canAppointSwornRole(membership.get().getRank()))
                .married(churchService.isMarried(kingdomId, playerId))
                .partnerAtChurch(!partners(player, kingdomId).isEmpty())
                .heldExperience(churchService.hasHeldExperience(kingdomId, playerId))
                .villagerAwaitingRites(churchService.villagersAwaitingRites(kingdomId) > 0)
                .marriagesStanding(!churchService.marriages(kingdomId).isEmpty())
                .build()
                .rites();
    }

    /** Unwed fellow subjects standing at the church, who might be asked. */
    private List<Player> partners(Player asker, String kingdomId) {
        List<Player> partners = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(asker.getUniqueId())) {
                continue;
            }
            Optional<PlayerMembership> membership = kingdomService.getMembership(online.getUniqueId());
            if (membership.isPresent()
                    && kingdomId.equals(membership.get().getKingdomId())
                    && !churchService.isMarried(kingdomId, online.getUniqueId())
                    && ChurchPresence.atChurch(churchService, kingdomId, online)) {
                partners.add(online);
            }
        }
        return partners;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRitesClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RitesGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isLeaveSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        Optional<RitesGui.Choice> choice = gui.choiceAt(event.getSlot());
        Optional<Kingdom> kingdom = kingdomService.getKingdom(gui.kingdomId());
        if (choice.isEmpty() || kingdom.isEmpty()) {
            return;
        }
        String kingdomId = gui.kingdomId();
        switch (choice.get()) {
            case CORONATION -> openCoronation(player, kingdom.get());
            case OATH -> openOath(player, kingdom.get());
            default -> {
                Rite rite = Rite.valueOf(choice.get().name());
                // The window may be stale: ask again whether the rite is still theirs.
                if (!ritesFor(player, kingdomId).contains(rite)) {
                    RealmFeedback.refuse(player, "That rite is not yours to ask for now.");
                    player.closeInventory();
                    return;
                }
                hold(player, kingdomId, rite);
            }
        }
    }

    private void hold(Player player, String kingdomId, Rite rite) {
        Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
        switch (rite) {
            case CONSECRATE -> {
                ChurchResult result = rites.consecrate(kingdomId, celebrant);
                answer(player, kingdomId, result);
                if (result instanceof ChurchResult.Success) {
                    RealmFeedback.kingdomMessage(kingdomService, kingdomId, "&6[Church] &fThe church is consecrated.");
                }
            }
            case MARRY -> {
                List<RitePickGui.Pick> picks = new ArrayList<>();
                for (Player partner : partners(player, kingdomId)) {
                    picks.add(new RitePickGui.Pick(
                            partner.getUniqueId(), partner.getName(), List.of("Click to ask for their hand.")));
                }
                player.openInventory(
                        RitePickGui.create(RitePickGui.Purpose.PARTNER, kingdomId, picks).getInventory());
            }
            case DIVORCE -> {
                Optional<UUID> spouse = churchService.spouseOf(kingdomId, player.getUniqueId());
                if (spouse.isEmpty()) {
                    RealmFeedback.refuse(player, "You are not wed.");
                    return;
                }
                player.openInventory(RiteConfirmGui.create(
                                RiteConfirmGui.Purpose.DIVORCE,
                                kingdomId,
                                spouse.get(),
                                List.of("Ask " + ChurchRites.nameOf(spouse.get()) + " to consent to a divorce.",
                                        "They must answer at the church within a minute."))
                        .getInventory());
            }
            case ANNUL -> {
                List<RitePickGui.Pick> picks = new ArrayList<>();
                for (Marriage marriage : churchService.marriages(kingdomId)) {
                    picks.add(new RitePickGui.Pick(
                            marriage.first(),
                            ChurchRites.nameOf(marriage.first()) + " & " + ChurchRites.nameOf(marriage.second()),
                            List.of("Click to annul this marriage.")));
                }
                player.openInventory(
                        RitePickGui.create(RitePickGui.Purpose.ANNUL, kingdomId, picks).getInventory());
            }
            case FUNERAL -> {
                FuneralOutcome outcome = rites.funeral(kingdomId, celebrant, player);
                answer(player, kingdomId, outcome.result());
            }
            case VILLAGER_FUNERAL -> {
                Optional<VillagerFuneralOutcome> outcome = rites.villagerFuneral(kingdomId, celebrant);
                if (outcome.isEmpty()) {
                    RealmFeedback.refuse(player, "No villager of this realm awaits its rites.");
                    return;
                }
                answer(player, kingdomId, outcome.get().result());
                if (outcome.get().result() instanceof ChurchResult.Success) {
                    player.sendMessage(c("&7" + KingdomChurchHandler.tithedLine(outcome.get())));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RitePickGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(gui.kingdomId());
        if (kingdom.isEmpty()) {
            player.closeInventory();
            return;
        }
        if (gui.isBackSlot(event.getSlot())) {
            openRites(player, kingdom.get());
            return;
        }
        Optional<UUID> pick = gui.pickAt(event.getSlot());
        if (pick.isEmpty()) {
            return;
        }
        String kingdomId = gui.kingdomId();
        if (gui.purpose() == RitePickGui.Purpose.ANNUL) {
            List<String> lines = List.of(
                    ChurchRites.nameOf(pick.get()) + "'s marriage will be annulled.",
                    "Neither spouse's consent is asked.");
            player.openInventory(RiteConfirmGui.create(RiteConfirmGui.Purpose.ANNUL, kingdomId, pick.get(), lines)
                    .getInventory());
            return;
        }
        Player partner = Bukkit.getPlayer(pick.get());
        if (partner == null
                || !partners(player, kingdomId).contains(partner)
                || !ritesFor(player, kingdomId).contains(Rite.MARRY)) {
            RealmFeedback.refuse(player, "They are no longer free to be asked at the church.");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        Optional<String> refusal = rites.propose(ChurchConsentBook.Kind.MARRIAGE, kingdomId, player, partner);
        if (refusal.isPresent()) {
            RealmFeedback.refuse(player, refusal.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RiteConfirmGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        boolean yes = gui.isYesSlot(event.getSlot());
        if (!yes && !gui.isNoSlot(event.getSlot())) {
            return;
        }
        if (gui.purpose() == RiteConfirmGui.Purpose.CONSENT) {
            rites.answer(player, yes);
            return;
        }
        player.closeInventory();
        if (!yes) {
            return;
        }
        String kingdomId = gui.kingdomId();
        if (gui.purpose() == RiteConfirmGui.Purpose.DIVORCE) {
            Player spouse = Bukkit.getPlayer(gui.subject());
            if (!ritesFor(player, kingdomId).contains(Rite.DIVORCE)) {
                RealmFeedback.refuse(player, "That rite is not yours to ask for now.");
                return;
            }
            if (spouse == null) {
                RealmFeedback.refuse(player, "Your spouse must be here to consent. The Crown may annul instead.");
                return;
            }
            Optional<String> refusal = rites.propose(ChurchConsentBook.Kind.DIVORCE, kingdomId, player, spouse);
            if (refusal.isPresent()) {
                RealmFeedback.refuse(player, refusal.get());
            }
            return;
        }
        // Annulment: the Crown's remedy, asked afresh of the rank the clicker holds now.
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            RealmFeedback.refuse(player, "Only the King or Queen may annul a marriage.");
            return;
        }
        Optional<UUID> spouse = churchService.spouseOf(kingdomId, gui.subject());
        ChurchResult result = rites.annul(kingdomId, membership.get().getRank(), gui.subject());
        if (!(result instanceof ChurchResult.Success)) {
            RealmFeedback.refuse(player, result.message());
            return;
        }
        player.sendMessage(c("&a" + result.message()));
        RealmFeedback.success(player.getLocation());
        List<UUID> parties = new ArrayList<>(List.of(gui.subject()));
        if (spouse.isPresent()) {
            parties.add(spouse.get());
        }
        for (UUID party : parties) {
            Player online = Bukkit.getPlayer(party);
            if (online != null && !online.equals(player)) {
                online.sendMessage(c("&6[Church] &fThe Crown has annulled your marriage."));
            }
        }
    }

    /** A rite's result: success heard and seen at the church, a refusal only heard. */
    private void answer(Player player, String kingdomId, ChurchResult result) {
        player.closeInventory();
        if (result instanceof ChurchResult.Success) {
            player.sendMessage(c("&a" + result.message()));
            RealmFeedback.success(rites.churchLocation(kingdomId).orElse(player.getLocation()));
        } else {
            player.sendMessage(c("&c" + result.message()));
            RealmFeedback.refuse(player, result.message());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoronationClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CoronationGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isLeaveSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        if (gui.isOathSlot(event.getSlot())) {
            swear(player, gui.kingdomId());
            return;
        }
        if (!gui.isCrownSlot(event.getSlot()) || !isRoyalOf(gui.kingdomId(), player.getUniqueId())) {
            return;
        }
        crown(player, gui.kingdomId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOathClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OathOfServiceGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isLeaveSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        if (!gui.isSwearSlot(event.getSlot()) || !ChurchPresence.atChurch(churchService, gui.kingdomId(), player)) {
            return;
        }
        swear(player, gui.kingdomId());
    }

    private void swear(Player player, String kingdomId) {
        if (!ChurchPresence.atChurch(churchService, kingdomId, player)) {
            player.sendMessage(c("&cStand at the church to swear the oath of service."));
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        OathResult result;
        if (membership.isPresent()) {
            if (!kingdomId.equals(membership.get().getKingdomId())) {
                player.sendMessage(c("&cYour oath belongs at your own realm's church."));
                return;
            }
            result = oathService.swearAsMember(player.getUniqueId());
        } else {
            result = oathService.swearAsOutsider(
                    kingdomId, player.getUniqueId(), "service to " + realmName(kingdomId));
        }
        if (result instanceof OathResult.Success) {
            store.saveFrom(kingdomService);
            player.sendMessage(c("&a" + messageOf(result)));
            player.closeInventory();
        } else {
            player.sendMessage(c("&c" + messageOf(result)));
        }
    }

    /** Belt and braces: any other road to the cleric's trading window is closed too. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenTrades(InventoryOpenEvent event) {
        if (event.getInventory() instanceof MerchantInventory merchant
                && merchant.getMerchant() instanceof Entity entity
                && clericService.isCleric(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CoronationGui
                || event.getInventory().getHolder() instanceof OathOfServiceGui
                || event.getInventory().getHolder() instanceof RitesGui
                || event.getInventory().getHolder() instanceof RitePickGui
                || event.getInventory().getHolder() instanceof RiteConfirmGui) {
            event.setCancelled(true);
        }
    }

    /** The coronation itself: the only road to it, now that the command is gone. */
    private void crown(Player player, String kingdomId) {
        Optional<UUID> monarch = kingdomService.findMonarch(kingdomId).map(PlayerMembership::getPlayerId);
        if (monarch.isEmpty()) {
            player.sendMessage(c("&cThis realm has no monarch to crown."));
            return;
        }
        Player crowned = Bukkit.getPlayer(monarch.get());
        if (crowned == null || !ChurchPresence.atChurch(churchService, kingdomId, crowned)) {
            player.sendMessage(c("&cThe monarch must stand at the church to be crowned."));
            return;
        }
        Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
        ChurchResult result = churchService.crown(kingdomId, celebrant, monarch.get());
        if (result instanceof ChurchResult.Success) {
            player.sendMessage(c("&a" + result.message()));
            store.saveFrom(kingdomService);
            Bukkit.broadcastMessage(c("&6")
                    + kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId)
                    + " has crowned its monarch.");
            player.closeInventory();
        } else {
            player.sendMessage(c("&c" + result.message()));
        }
    }

    private void openOath(Player player, Kingdom kingdom) {
        boolean member = kingdomService.getMembership(player.getUniqueId()).isPresent();
        player.openInventory(OathOfServiceGui.create(kingdom.getId(), kingdom.getDisplayName(), member).getInventory());
    }

    private String realmName(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId);
    }

    private static String messageOf(OathResult result) {
        if (result instanceof OathResult.Success success) {
            return success.message();
        }
        if (result instanceof OathResult.Disabled disabled) {
            return disabled.message();
        }
        return ((OathResult.Failure) result).message();
    }

    /** The King, Queen or a Prince of the realm the cleric serves. */
    private boolean isRoyalOf(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent()
                && kingdomId.equals(membership.get().getKingdomId())
                && CityService.isRoyalExempt(membership.get().getRank());
    }
}
