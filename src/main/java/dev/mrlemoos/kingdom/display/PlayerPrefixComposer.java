package dev.mrlemoos.kingdom.display;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.police.JurisdictionPort;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.WantedNametagPolicy;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared player chat/tab/nametag prefix composition: wanted replaces noble and sworn when shown.
 */
public final class PlayerPrefixComposer {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final MechanicalJusticeService justiceService;
    private final JurisdictionPort jurisdictionPort;

    public PlayerPrefixComposer(KingdomService kingdomService) {
        this(kingdomService, null, null, null);
    }

    public PlayerPrefixComposer(KingdomService kingdomService, PoliceService policeService) {
        this(kingdomService, policeService, null, null);
    }

    public PlayerPrefixComposer(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            JurisdictionPort jurisdictionPort) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = policeService;
        this.justiceService = justiceService;
        this.jurisdictionPort = jurisdictionPort;
    }

    public String fullColouredPrefix(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        boolean showWanted = false;
        if (justiceService != null && jurisdictionPort != null) {
            Optional<String> territoryKingdom = jurisdictionPort.kingdomAt(playerId);
            if (territoryKingdom.isPresent()) {
                showWanted = WantedNametagPolicy.shouldShow(
                        true,
                        justiceService.hasActiveWarrant(territoryKingdom.get(), playerId),
                        true);
            }
        }

        String sworn = "";
        if (policeService != null) {
            Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
            if (membership.isPresent()) {
                sworn = policeService.colouredSwornChatPrefix(
                        membership.get().getKingdomId(), playerId);
            }
        }
        return WantedNametagPolicy.composePrefix(
                showWanted, sworn, kingdomService.colouredNobleChatPrefix(playerId));
    }
}
