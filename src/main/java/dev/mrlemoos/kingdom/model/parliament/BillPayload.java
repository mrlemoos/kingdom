package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import java.util.UUID;

public sealed interface BillPayload
        permits BillPayload.Fiscal, BillPayload.Budget, BillPayload.SpendMint, BillPayload.SpendStipend,
                BillPayload.War, BillPayload.Peace {

    record Fiscal(FiscalRates rates) implements BillPayload {}

    record Budget(double amount) implements BillPayload {}

    record SpendMint(MintLocation mintLocation, double cost) implements BillPayload {}

    record SpendStipend(UUID recipientId, double amount, String reason) implements BillPayload {}

    /**
     * Names the sole target kingdom, war aim, victory outcome, and muster deadline duration. A single
     * {@code targetKingdomId} field structurally rejects coalitions/multi-target wars.
     */
    record War(String targetKingdomId, WarAim aim, WarOutcome outcome, int musterDeadlineMcDays)
            implements BillPayload {}

    /**
     * Names the active war to end. Enactment ceases hostilities and demobilises the levy; standing
     * roster membership persists, and no annexation/tribute side effect applies (peace without
     * decisive victory).
     */
    record Peace(String warId) implements BillPayload {}
}
