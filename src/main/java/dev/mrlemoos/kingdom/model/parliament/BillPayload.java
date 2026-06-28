package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.UUID;

public sealed interface BillPayload
        permits BillPayload.Fiscal, BillPayload.Budget, BillPayload.SpendMint, BillPayload.SpendStipend {

    record Fiscal(FiscalRates rates) implements BillPayload {}

    record Budget(double amount) implements BillPayload {}

    record SpendMint(MintLocation mintLocation, double cost) implements BillPayload {}

    record SpendStipend(UUID recipientId, double amount, String reason) implements BillPayload {}
}
