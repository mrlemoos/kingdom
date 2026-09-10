package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import dev.mrlemoos.kingdom.war.squad.Squad;
import dev.mrlemoos.kingdom.war.squad.SquadMember;
import dev.mrlemoos.kingdom.war.squad.SquadService;
import dev.mrlemoos.kingdom.war.squad.SquadState;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Sneak-right-click rank-and-file to assign it; right-click an assigned unit to cycle its order. */
public final class SquadControlListener implements Listener {

    private final KingdomService kingdoms;
    private final WarService wars;
    private final ConscriptionService conscription;
    private final CrownSquadService crownSquads;
    private final SquadService squads;
    private final Predicate<UUID> militaryParticipant;

    public SquadControlListener(
            KingdomService kingdoms,
            WarService wars,
            ConscriptionService conscription,
            CrownSquadService crownSquads,
            SquadService squads,
            Predicate<UUID> militaryParticipant) {
        this.kingdoms = kingdoms;
        this.wars = wars;
        this.conscription = conscription;
        this.crownSquads = crownSquads;
        this.squads = squads;
        this.militaryParticipant = militaryParticipant;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSquadOrder(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player officer = event.getPlayer();
        Optional<PlayerMembership> membership = kingdoms.getMembership(officer.getUniqueId());
        if (membership.isEmpty()) return;
        PlayerMembership member = membership.get();
        Optional<SquadMember> unit = squadMember(member.getKingdomId(), event.getRightClicked());
        if (unit.isEmpty()) return;
        event.setCancelled(true);
        if (!wars.config().enabled() || !squads.config().enabled()) {
            officer.sendMessage(c("&cSquads are disabled."));
            return;
        }
        if (!RankAuthority.canCommandSquads(member.getRank())) {
            officer.sendMessage(c("&cOnly a Knight or the Crown may command squads."));
            return;
        }
        if (!wars.isAtWar(member.getKingdomId())) {
            officer.sendMessage(c("&cSquads may be commanded only during an active war."));
            return;
        }
        if (!militaryParticipant.test(officer.getUniqueId())) {
            officer.sendMessage(c("&cOnly a military participant may command a squad."));
            return;
        }
        if (officer.isSneaking()) {
            reply(officer, squads.assign(member.getKingdomId(), officer.getUniqueId(), java.util.Set.of(unit.get())));
            return;
        }
        Optional<Squad> squad = squads.squadsForOfficer(officer.getUniqueId()).stream()
                .filter(found -> found.members().contains(unit.get()))
                .findFirst();
        if (squad.isEmpty()) {
            officer.sendMessage(c("&eSneak-right-click this unit to assign it to your squad."));
            return;
        }
        reply(officer, squads.command(squad.get().id(), next(squad.get().state())));
    }

    private Optional<SquadMember> squadMember(String kingdomId, Entity entity) {
        if (conscription.pressedVillager(entity.getUniqueId())
                .filter(pressed -> kingdomId.equals(pressed.kingdomId()))
                .isPresent()) return Optional.of(new SquadMember.PressedVillager(entity.getUniqueId()));
        boolean crownUnit = crownSquads.unitsOf(kingdomId).stream()
                .anyMatch(unit -> unit.unitId().equals(entity.getUniqueId()));
        return crownUnit ? Optional.of(new SquadMember.CrownUnit(entity.getUniqueId())) : Optional.empty();
    }

    private static SquadState next(SquadState state) {
        return switch (state) {
            case IDLE -> SquadState.FOLLOW;
            case FOLLOW -> SquadState.ATTACK;
            case ATTACK, ROUTED -> SquadState.IDLE;
        };
    }

    private static void reply(Player player, WarResult result) {
        String message = result instanceof WarResult.Success success ? success.message() : ((WarResult.Failure) result).message();
        player.sendMessage(c((result instanceof WarResult.Success ? "&a" : "&c") + message));
    }
}
