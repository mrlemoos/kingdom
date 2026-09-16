package dev.mrlemoos.kingdom.economy.villager;

import java.util.Optional;
import java.util.UUID;

public record EmeraldVillagerTradeRequest(
        Optional<String> kingdomId,
        Optional<String> traderKingdomId,
        UUID villagerId,
        int emeraldCost,
        boolean treasuryLord,
        boolean seatedMp,
        boolean kingdomTaggedMp,
        boolean territoryMember,
        boolean onStrike) {

    public EmeraldVillagerTradeRequest(
            Optional<String> kingdomId,
            UUID villagerId,
            int emeraldCost,
            boolean treasuryLord,
            boolean seatedMp,
            boolean kingdomTaggedMp,
            boolean territoryMember,
            boolean onStrike) {
        this(kingdomId, Optional.empty(), villagerId, emeraldCost, treasuryLord, seatedMp, kingdomTaggedMp,
                territoryMember, onStrike);
    }
}
