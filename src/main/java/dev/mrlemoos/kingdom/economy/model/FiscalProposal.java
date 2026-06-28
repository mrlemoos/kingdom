package dev.mrlemoos.kingdom.economy.model;

import java.util.UUID;

public record FiscalProposal(FiscalRates proposedRates, UUID proposerId, long timestampMillis) {}
