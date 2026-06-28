package dev.mrlemoos.kingdom.economy.villager;

import java.util.Optional;
import java.util.UUID;

public record EmeraldVillagerTradeRequest(
        Optional<String> kingdomId,
        UUID villagerId,
        int emeraldCost,
        boolean treasuryLord,
        boolean seatedMp,
        boolean kingdomTaggedMp) {}
