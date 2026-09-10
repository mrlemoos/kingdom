package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.loyalty.MoralePardonRoll;
import dev.mrlemoos.kingdom.loyalty.MoraleResult;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.loyalty.gui.MoralePardonGui;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * The morale pardon, heard at the court.
 *
 * <p>{@code MoraleService#pardon} says the Crown or an appointed Knight restores military morale "at
 * a muster point or court". The realm sites no muster point, but it does site a court — a lectern
 * and a villager judge, set by {@code /kingdom police court set} — and the judge's bench was the one
 * placed office in the realm that answered no right-click at all. So the bench is where the pardon
 * is heard: right-click the judge, read the roll of subjects whose morale has fallen, and click one
 * to restore them to Steadfast.
 *
 * <p>No power is decided here: the rank gate is {@link RankAuthority#canGrantMoralePardon}, the roll
 * is {@link MoralePardonRoll}, and the pardon itself is {@link MoraleService#pardon}.
 */
public final class MoralePardonListener implements Listener {

    private static final String REFUSAL_RANK = "Only the Crown or a Knight may grant a morale pardon.";
    private static final String REFUSAL_DISABLED = "The realm keeps no muster roll, so there is no morale to pardon.";
    private static final String REFUSAL_NOTHING_TO_PARDON =
            "No soldier of the realm wants a pardon; every morale stands Steadfast.";
    private static final String REFUSAL_FOREIGN = "The judge hears only the subjects of this realm.";

    private final KingdomService kingdomService;
    private final PoliceCourtService courtService;
    private final MoraleService moraleService;

    public MoralePardonListener(
            KingdomService kingdomService, PoliceCourtService courtService, MoraleService moraleService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.courtService = Objects.requireNonNull(courtService, "courtService");
        this.moraleService = Objects.requireNonNull(moraleService, "moraleService");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractJudge(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager villager) || !courtService.isJudgeEntity(villager)) {
            return;
        }
        // Cancelled whoever clicks: the judge keeps no trades.
        event.setCancelled(true);
        Optional<String> kingdomId = courtService.kingdomIdForJudge(villager);
        Player player = event.getPlayer();
        if (kingdomId.isEmpty()) {
            player.sendMessage(c("&cThis court serves no realm."));
            return;
        }
        openPardonRoll(player, kingdomId.get());
    }

    /**
     * The business of the bench: shows {@code actor} the roll of subjects of {@code kingdomId} there
     * is anything to pardon, or refuses in plain words and does nothing.
     */
    public void openPardonRoll(Player actor, String kingdomId) {
        Optional<PlayerMembership> member = memberAtCourt(actor, kingdomId);
        if (member.isEmpty()) {
            actor.sendMessage(c("&7" + REFUSAL_FOREIGN));
            return;
        }
        if (!RankAuthority.canGrantMoralePardon(member.get().getRank())) {
            actor.sendMessage(c("&c" + REFUSAL_RANK));
            return;
        }
        if (!moraleService.config().militaryEnabled()) {
            actor.sendMessage(c("&7" + REFUSAL_DISABLED));
            return;
        }
        List<MoralePardonRoll.Subject> roll = rollFor(kingdomId);
        if (roll.isEmpty()) {
            actor.sendMessage(c("&7" + REFUSAL_NOTHING_TO_PARDON));
            return;
        }
        actor.openInventory(
                Objects.requireNonNull(MoralePardonGui.create(roll, namesOf(roll)).getInventory()));
    }

    /**
     * Grants the pardon a click asked for, re-reading every gate: the roll on screen may be a moment
     * stale, and the actor's rank may have changed since it opened.
     */
    public void grantPardon(Player actor, String kingdomId, UUID subjectId) {
        Optional<PlayerMembership> member = memberAtCourt(actor, kingdomId);
        if (member.isEmpty()) {
            actor.sendMessage(c("&7" + REFUSAL_FOREIGN));
            return;
        }
        NobleRank rank = member.get().getRank();
        if (!RankAuthority.canGrantMoralePardon(rank)) {
            actor.sendMessage(c("&c" + REFUSAL_RANK));
            return;
        }
        if (!moraleService.config().militaryEnabled()) {
            actor.sendMessage(c("&7" + REFUSAL_DISABLED));
            return;
        }
        Optional<PlayerMembership> subject = kingdomService.getMembership(subjectId);
        if (subject.isEmpty() || !kingdomId.equals(subject.get().getKingdomId())) {
            actor.sendMessage(c("&7" + REFUSAL_FOREIGN));
            return;
        }
        Optional<MoraleTier> tier = moraleService.tierOf(subjectId);
        if (tier.isEmpty() || tier.get() == MoraleTier.STEADFAST) {
            actor.sendMessage(c("&7There is nothing to pardon: that subject's morale stands Steadfast."));
            return;
        }
        MoraleResult result = moraleService.pardon(subjectId, rank);
        if (result instanceof MoraleResult.Success success) {
            actor.sendMessage(c("&a" + nameOf(subjectId) + " is pardoned. " + success.message()));
            Player pardoned = Bukkit.getPlayer(subjectId);
            if (pardoned != null) {
                pardoned.sendMessage(c("&aThe court grants you a morale pardon. Military morale restored to Steadfast."));
            }
            return;
        }
        if (result instanceof MoraleResult.Disabled disabled) {
            actor.sendMessage(c("&7" + disabled.message()));
            return;
        }
        actor.sendMessage(c("&c" + ((MoraleResult.Failure) result).message()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MoralePardonGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        UUID subjectId = gui.subjectForSlot(event.getSlot());
        if (subjectId == null) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(c("&7" + REFUSAL_FOREIGN));
            return;
        }
        player.closeInventory();
        grantPardon(player, membership.get().getKingdomId(), subjectId);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MoralePardonGui) {
            event.setCancelled(true);
        }
    }

    /** The actor's standing, present only when they are sworn to the realm whose court this is. */
    private Optional<PlayerMembership> memberAtCourt(Player actor, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(actor.getUniqueId());
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return Optional.empty();
        }
        return membership;
    }

    private List<MoralePardonRoll.Subject> rollFor(String kingdomId) {
        List<UUID> members = new ArrayList<>();
        for (Map.Entry<UUID, PlayerMembership> entry : kingdomService.getMembershipsView().entrySet()) {
            if (kingdomId.equals(entry.getValue().getKingdomId())) {
                members.add(entry.getKey());
            }
        }
        return MoralePardonRoll.of(members, moraleService.store().allTiersView());
    }

    private static List<String> namesOf(List<MoralePardonRoll.Subject> roll) {
        List<String> names = new ArrayList<>();
        for (MoralePardonRoll.Subject subject : roll) {
            names.add(nameOf(subject.playerId()));
        }
        return names;
    }

    private static String nameOf(UUID playerId) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name == null || name.isBlank() ? "A subject of the realm" : name;
    }
}
