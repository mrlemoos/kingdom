package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.ArrestReward;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Posts and settles arrest-reward purses on active warrants. Escrow lives on the warrant; wallet
 * moves use {@link EconomyService} only (never the kingdom treasury).
 */
public final class ArrestRewardService {

    private final KingdomService kingdomService;
    private final MechanicalJusticeService justiceService;
    private final EconomyService economyService;

    public ArrestRewardService(
            KingdomService kingdomService,
            MechanicalJusticeService justiceService,
            EconomyService economyService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
    }

    /**
     * Posts a new purse or tops up an existing one on the suspect's active warrant.
     */
    public PoliceResult postOrTopUp(String kingdomId, UUID posterId, UUID suspectId, double amount) {
        if (amount <= 0) {
            return PoliceResult.fail("Arrest reward amount must be positive.");
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(posterId);
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return PoliceResult.fail("Only members of this kingdom may post an arrest reward.");
        }
        Optional<Warrant> found = justiceService.findActiveForSuspect(kingdomId, suspectId);
        if (found.isEmpty()) {
            return PoliceResult.fail("No active warrant for that suspect.");
        }
        Warrant warrant = found.get();
        Optional<ArrestReward> existing = warrant.arrestReward();
        if (existing.isPresent() && !existing.get().posterId().equals(posterId)) {
            return PoliceResult.fail("Only the original poster may top up this arrest reward.");
        }
        if (!economyService.debitWallet(posterId, amount)) {
            return PoliceResult.fail("Insufficient Corona in your wallet.");
        }
        if (existing.isPresent()) {
            existing.get().topUp(amount);
            return PoliceResult.ok(
                    "Arrest reward topped up to " + existing.get().amount() + " Corona.");
        }
        warrant.setArrestReward(new ArrestReward(posterId, amount));
        return PoliceResult.ok("Arrest reward of " + amount + " Corona posted.");
    }

    public void payOnConstableArrest(Warrant warrant, UUID constableId) {
        Objects.requireNonNull(warrant, "warrant");
        Objects.requireNonNull(constableId, "constableId");
        Optional<ArrestReward> reward = warrant.arrestReward();
        if (reward.isEmpty()) {
            return;
        }
        ArrestReward purse = reward.get();
        economyService.creditWalletDirect(constableId, purse.amount());
        warrant.clearArrestReward();
    }

    public void refundPoster(Warrant warrant) {
        Objects.requireNonNull(warrant, "warrant");
        Optional<ArrestReward> reward = warrant.arrestReward();
        if (reward.isEmpty()) {
            return;
        }
        ArrestReward purse = reward.get();
        economyService.creditWalletDirect(purse.posterId(), purse.amount());
        warrant.clearArrestReward();
    }

    /**
     * Cancels an active warrant and refunds any arrest reward to the poster.
     */
    public PoliceResult cancelAndRefund(String kingdomId, UUID crownId, String warrantId) {
        Optional<Warrant> found = justiceService.findById(kingdomId, warrantId);
        if (found.isEmpty()) {
            return PoliceResult.fail("Unknown warrant.");
        }
        Warrant warrant = found.get();
        PoliceResult cancelled = justiceService.cancelActiveWarrant(kingdomId, crownId, warrantId);
        if (cancelled instanceof PoliceResult.Failure) {
            return cancelled;
        }
        refundPoster(warrant);
        return PoliceResult.ok("Warrant cancelled. Arrest reward refunded to the poster.");
    }

    public PoliceResult cancelActiveForSuspect(String kingdomId, UUID crownId, UUID suspectId) {
        Optional<Warrant> found = justiceService.findActiveForSuspect(kingdomId, suspectId);
        if (found.isEmpty()) {
            return PoliceResult.fail("No active warrant for that suspect.");
        }
        return cancelAndRefund(kingdomId, crownId, found.get().id());
    }
}
