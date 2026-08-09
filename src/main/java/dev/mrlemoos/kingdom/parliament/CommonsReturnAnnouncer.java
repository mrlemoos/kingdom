package dev.mrlemoos.kingdom.parliament;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Gives the Speaker a voice: the return of the Commons is read to the kingdom's members, and the
 * villager Speaker — or the player who holds the Chair — is heard alongside the words.
 */
public final class CommonsReturnAnnouncer {

    private static final int TOTAL_SEATS = 8;

    private final KingdomService kingdomService;

    public CommonsReturnAnnouncer(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    /** The Speaker reads the full roll-call of the House to the realm's members. */
    public void announceRollCall(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> speak(
                kingdom,
                CommonsReturn.rollCall(
                        kingdom.getElectionState(), TOTAL_SEATS, CommonsReturnAnnouncer::playerName)));
    }

    /** The Speaker reads a single seat's return after a by-election. */
    public void announceSeatReturn(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            Optional<MpSeat> seat = kingdom.getElectionState().seat(seatIndex);
            if (seat.isEmpty()) {
                return;
            }
            speak(kingdom, List.of(CommonsReturn.seatReturn(seat.get(), CommonsReturnAnnouncer::playerName)));
        });
    }

    private void speak(Kingdom kingdom, List<String> lines) {
        List<Player> members = onlineMembers(kingdom.getId());
        Entity speaker = speakerEntity(kingdom, members);
        for (String line : lines) {
            for (Player member : members) {
                member.sendMessage(NoblePrefixDisplay.speakerVillagerNametag() + c(" &f" + line));
            }
            if (speaker != null) {
                speaker.getWorld().playSound(speaker.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            }
        }
    }

    /** The villager Speaker gives voice to the roll; where a player holds the Chair, they do. */
    private Entity speakerEntity(Kingdom kingdom, List<Player> members) {
        Optional<UUID> villagerSpeaker = kingdom.getParliamentState().speakerVillagerEntityId();
        if (villagerSpeaker.isPresent()) {
            Entity found = Bukkit.getEntity(villagerSpeaker.get());
            if (found != null) {
                return found;
            }
        }
        for (Player member : members) {
            if (kingdomService
                    .getMembership(member.getUniqueId())
                    .filter(membership -> membership.getRank() == NobleRank.SPEAKER)
                    .isPresent()) {
                return member;
            }
        }
        return null;
    }

    private List<Player> onlineMembers(String kingdomId) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(online -> kingdomService
                        .getMembership(online.getUniqueId())
                        .filter(membership -> kingdomId.equals(membership.getKingdomId()))
                        .isPresent())
                .map(Player.class::cast)
                .toList();
    }

    private static String playerName(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? "Unknown" : name;
    }
}
