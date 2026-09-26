package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.SwordArrest;
import dev.mrlemoos.kingdom.police.WarrantDesk;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A sworn constable arrests by striking a wanted subject with an iron sword inside the realm's
 * territory: the blow does no harm and the arrest runs as {@code /kingdom police arrest} would. Any
 * other strike is ordinary combat, and nothing is said. The rule is {@link SwordArrest}; the arrest
 * is {@link WarrantDesk}'s.
 */
public final class ConstableArrestListener implements Listener {

    private final KingdomService kingdomService;
    private final WarrantDesk desk;

    public ConstableArrestListener(KingdomService kingdomService, WarrantDesk desk) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.desk = Objects.requireNonNull(desk, "desk");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStrike(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player constable) || !(event.getEntity() instanceof Player suspect)) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(constable.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        SwordArrest decision = SwordArrest.decide(
                desk.isConstable(kingdomId, constable.getUniqueId()),
                holdsIronSword(constable),
                desk.hasActiveWarrant(kingdomId, suspect.getUniqueId()),
                desk.inJurisdiction(suspect.getLocation(), kingdomId));
        if (decision != SwordArrest.ARREST) {
            return;
        }
        event.setCancelled(true);
        PoliceResult result = desk.arrest(kingdomId, constable.getUniqueId(), suspect.getUniqueId());
        switch (result) {
            case PoliceResult.Success success -> {
                constable.sendMessage(c("&a" + suspect.getName() + " arrested. " + success.message()));
                RealmFeedback.success(suspect.getLocation());
            }
            case PoliceResult.Failure failure -> RealmFeedback.refuse(constable, failure.message());
        }
    }

    /** The instruction while a constable takes up the iron sword. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTakeUpSword(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack taken = player.getInventory().getItem(event.getNewSlot());
        if (taken == null || taken.getType() != Material.IRON_SWORD) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isPresent() && desk.isConstable(membership.get().getKingdomId(), player.getUniqueId())) {
            RealmFeedback.instruct(player, "Strike a [WANTED] subject inside the realm to arrest them.");
        }
    }

    private static boolean holdsIronSword(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD;
    }
}
