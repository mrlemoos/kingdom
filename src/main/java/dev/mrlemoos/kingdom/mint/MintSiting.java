package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Placing and removing a royal mint with its Lord of the Treasury, whichever road led there — the
 * foundation stone or the operators' command. The mint limit stays in {@link EconomyService}.
 */
public final class MintSiting {

    private final EconomyService economyService;
    private final YamlEconomyStore economyStore;
    private final TreasuryLordService treasuryLordService;

    public MintSiting(
            EconomyService economyService, YamlEconomyStore economyStore, TreasuryLordService treasuryLordService) {
        this.economyService = economyService;
        this.economyStore = economyStore;
        this.treasuryLordService = treasuryLordService;
    }

    /** The kingdom's mints, oldest first. */
    public List<MintLocation> mints(String kingdomId) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        return economy == null ? List.of() : List.copyOf(economy.mintLocations());
    }

    /**
     * Places a mint and stations its Lord of the Treasury.
     *
     * @return the refusal, or the mint as it stands with its Lord
     */
    public Placed place(String kingdomId, MintLocation location, int maxMints) {
        EconomyResult result = economyService.placeRoyalMint(kingdomId, location, maxMints);
        if (!(result instanceof EconomyResult.Success)) {
            return new Placed(result, Optional.empty());
        }
        MintLocation withLord = treasuryLordService.ensureLord(kingdomId, location);
        economyStore.saveFrom(economyService);
        return new Placed(result, Optional.of(withLord));
    }

    /** Takes a mint down with its Lord of the Treasury; false when the kingdom has no such mint. */
    public boolean remove(String kingdomId, MintLocation mint) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        if (economy == null) {
            return false;
        }
        List<MintLocation> mints = new ArrayList<>(economy.mintLocations());
        Optional<MintLocation> standing = mints.stream()
                .filter(candidate -> candidate.worldName().equals(mint.worldName())
                        && candidate.x() == mint.x()
                        && candidate.y() == mint.y()
                        && candidate.z() == mint.z())
                .findFirst();
        if (standing.isEmpty()) {
            return false;
        }
        treasuryLordService.despawnLord(kingdomId, standing.get());
        mints.remove(standing.get());
        KingdomEconomy updated = new KingdomEconomy(
                economy.treasuryBalance(),
                economy.totalTaxRevenue(),
                economy.totalGdpRevenue(),
                economy.lastDailyGdp(),
                economy.activeRates(),
                economy.pendingProposal().orElse(null),
                economy.budget(),
                mints);
        Map<String, KingdomEconomy> kingdomEconomies = new HashMap<>(economyService.kingdomEconomies());
        kingdomEconomies.put(kingdomId, updated);
        economyService.replaceState(economyService.wallets(), economyService.villagerWallets(), kingdomEconomies);
        economyStore.saveFrom(economyService);
        return true;
    }

    /** What came of placing a mint: the economy's verdict and, when it held, the mint with its Lord. */
    public record Placed(EconomyResult result, Optional<MintLocation> mint) {}
}
