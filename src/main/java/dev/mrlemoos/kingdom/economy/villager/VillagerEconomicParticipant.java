package dev.mrlemoos.kingdom.economy.villager;

import java.util.UUID;

public record VillagerEconomicParticipant(UUID villagerId, String profession, int tierIndex) {}
