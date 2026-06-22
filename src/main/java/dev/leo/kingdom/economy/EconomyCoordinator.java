package dev.leo.kingdom.economy;

import dev.leo.kingdom.economy.income.ActivityCategory;
import dev.leo.kingdom.economy.income.ActivityCooldownTracker;
import dev.leo.kingdom.economy.income.ActivityRewardCalculator;
import dev.leo.kingdom.economy.income.EconomyConfig;
import dev.leo.kingdom.economy.income.LifeEventCalculator;
import dev.leo.kingdom.economy.income.LifeEventTracker;
import dev.leo.kingdom.economy.model.CreditResult;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.IncomeLocation;
import dev.leo.kingdom.economy.model.KingdomEconomy;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.territory.TerritoryLocation;
import dev.leo.kingdom.economy.territory.TerritoryResolver;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class EconomyCoordinator {

    private static final double WILDERNESS_MULTIPLIER = 0.5;
    private static final long HARVEST_HOUR_MS = 3_600_000L;

    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final TerritoryResolver territoryResolver;
    private final EconomyConfig config;
    private final ActivityCooldownTracker activityCooldownTracker;
    private final LifeEventTracker lifeEventTracker;
    private final ActivityRewardCalculator activityRewardCalculator;
    private final LifeEventCalculator lifeEventCalculator;
    private final Map<UUID, HarvestWindow> harvestWindows = new HashMap<>();
    private Runnable persistenceHook = () -> {};

    public EconomyCoordinator(
            EconomyService economyService,
            KingdomService kingdomService,
            TerritoryResolver territoryResolver,
            EconomyConfig config) {
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.config = config != null ? config : EconomyConfig.defaults();
        this.activityCooldownTracker = new ActivityCooldownTracker(this.config);
        this.lifeEventTracker = new LifeEventTracker(this.config);
        this.activityRewardCalculator = new ActivityRewardCalculator(this.config);
        this.lifeEventCalculator = new LifeEventCalculator(this.config);
    }

    public void setPersistenceHook(Runnable persistenceHook) {
        this.persistenceHook = persistenceHook != null ? persistenceHook : () -> {};
    }

    public Optional<CreditResult> creditPlayerFromActivity(
            Player player, double gross, Location location, ActivityCategory category) {
        if (player == null || gross <= 0.0 || location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        long nowMs = System.currentTimeMillis();
        if (!activityCooldownTracker.canEarn(category, player.getUniqueId(), nowMs)) {
            return Optional.empty();
        }

        CreditResult result = creditPlayer(player, gross, location);
        if (result == null) {
            return Optional.empty();
        }

        activityCooldownTracker.record(category, player.getUniqueId(), nowMs);
        persist();
        return Optional.of(result);
    }

    public Optional<CreditResult> creditPlayerFromLifeEvent(
            Player player, double amount, boolean inOwnKingdom) {
        if (player == null || amount <= 0.0) {
            return Optional.empty();
        }

        long epochDay = epochDay(player);
        if (!lifeEventTracker.canEarnWithinDailyCap(player.getUniqueId(), epochDay, amount)) {
            return Optional.empty();
        }

        CreditResult result = creditPlayer(player, amount, player.getLocation());
        if (result == null) {
            return Optional.empty();
        }

        persist();
        return Optional.of(result);
    }

    public void creditTreasuryFromGdp(String kingdomId, double amount) {
        if (kingdomId == null || kingdomId.isBlank() || amount <= 0.0) {
            return;
        }
        economyService.recordGdpCredit(kingdomId, amount);
        persist();
    }

    public void setLastDailyGdp(String kingdomId, double amount) {
        if (kingdomId == null || kingdomId.isBlank()) {
            return;
        }
        economyService.setLastDailyGdp(kingdomId, amount);
    }

    public double getTotalTaxRevenue(String kingdomId) {
        return economyService.getTotalTaxRevenue(kingdomId);
    }

    public double getTotalGdpRevenue(String kingdomId) {
        return economyService.getTotalGdpRevenue(kingdomId);
    }

    public double getLastDailyGdp(String kingdomId) {
        return economyService.getLastDailyGdp(kingdomId);
    }

    public double getWalletBalance(UUID playerId) {
        return economyService.getWalletBalance(playerId);
    }

    public double getTreasuryBalance(String kingdomId) {
        return economyService.getTreasuryBalance(kingdomId);
    }

    public Optional<MintMatch> findMintAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (Map.Entry<String, KingdomEconomy> entry : economyService.kingdomEconomies().entrySet()) {
            for (MintLocation mint : entry.getValue().mintLocations()) {
                if (mint.worldName().equals(worldName)
                        && mint.x() == x
                        && mint.y() == y
                        && mint.z() == z) {
                    return Optional.of(new MintMatch(entry.getKey(), mint));
                }
            }
        }
        return Optional.empty();
    }

    public TerritoryLocation resolveTerritory(Player player, Location location) {
        String playerKingdomId = kingdomService.getMembership(player.getUniqueId())
                .map(PlayerMembership::getKingdomId)
                .orElse(null);
        return territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                playerKingdomId);
    }

    public boolean isOwnKingdom(Player player, Location location) {
        return resolveTerritory(player, location).type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
    }

    public int harvestCountThisHour(UUID playerId, long nowMs) {
        return harvestWindowFor(playerId, nowMs).count;
    }

    public void recordHarvest(UUID playerId, long nowMs) {
        harvestWindowFor(playerId, nowMs).count++;
    }

    public ActivityCooldownTracker activityCooldownTracker() {
        return activityCooldownTracker;
    }

    public LifeEventTracker lifeEventTracker() {
        return lifeEventTracker;
    }

    public ActivityRewardCalculator activityRewardCalculator() {
        return activityRewardCalculator;
    }

    public LifeEventCalculator lifeEventCalculator() {
        return lifeEventCalculator;
    }

    public EconomyConfig config() {
        return config;
    }

    public EconomyService economyService() {
        return economyService;
    }

    public KingdomService kingdomService() {
        return kingdomService;
    }

    public static long epochDay(Player player) {
        return player.getWorld().getFullTime() / 24000L;
    }

    public static long nightId(Player player) {
        return player.getWorld().getFullTime() / 12000L;
    }

    private CreditResult creditPlayer(Player player, double gross, Location location) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        String playerKingdomId = membership.map(PlayerMembership::getKingdomId).orElse(null);
        NobleRank rank = membership.map(PlayerMembership::getRank).orElse(null);

        TerritoryLocation territory = resolveTerritory(player, location);
        IncomeLocation incomeLocation = toIncomeLocation(territory);
        String incomeKingdomId = territory.kingdomId().orElse(null);
        FiscalRates rates = fiscalRatesFor(playerKingdomId);

        return economyService.creditWallet(
                player.getUniqueId(),
                gross,
                incomeLocation,
                rank,
                playerKingdomId,
                incomeKingdomId,
                rates,
                WILDERNESS_MULTIPLIER);
    }

    private void persist() {
        persistenceHook.run();
    }

    private FiscalRates fiscalRatesFor(String kingdomId) {
        if (kingdomId == null) {
            return FiscalRates.defaults();
        }
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        return economy != null ? economy.activeRates() : FiscalRates.defaults();
    }

    private static IncomeLocation toIncomeLocation(TerritoryLocation territory) {
        return switch (territory.type()) {
            case WILDERNESS -> IncomeLocation.WILDERNESS;
            case OWN_KINGDOM -> IncomeLocation.OWN_KINGDOM;
            case FOREIGN_KINGDOM -> IncomeLocation.FOREIGN_KINGDOM;
        };
    }

    private HarvestWindow harvestWindowFor(UUID playerId, long nowMs) {
        HarvestWindow window = harvestWindows.computeIfAbsent(playerId, ignored -> new HarvestWindow(nowMs));
        if (nowMs - window.windowStartMs >= HARVEST_HOUR_MS) {
            window.windowStartMs = nowMs;
            window.count = 0;
        }
        return window;
    }

    public record MintMatch(String kingdomId, MintLocation location) {}

    private static final class HarvestWindow {
        private long windowStartMs;
        private int count;

        private HarvestWindow(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }
}
