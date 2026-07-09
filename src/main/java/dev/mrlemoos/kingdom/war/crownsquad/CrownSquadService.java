package dev.mrlemoos.kingdom.war.crownsquad;

import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Crown squad treasury purchase (Phase 5, Slice 5.2): spawned mobs funded from the kingdom
 * treasury per an already-approved war-spending budget — mirroring {@code
 * EconomyService#placeMint}'s {@code spendFromBudget} pattern rather than an unbudgeted direct
 * treasury debit, so a purchase always requires a prior Parliament supply bill enactment. Every
 * successful purchase ledgers a placeholder {@link CrownSquadUnit} id for the later Bukkit-layer
 * mob spawn; no actual mob is spawned here.
 *
 * <p>Counts against a crown-squad-specific per-kingdom cap ({@link CrownSquadConfig#cap()}).
 * Reconciling this into one shared army cap alongside {@code ConscriptionService}'s pressed
 * villagers (Slice 5.1) is deferred to Slice 5.3's squad assignment work.
 *
 * <p>Domain-only — no Bukkit mob spawning or entity handling.
 */
public final class CrownSquadService {

    private final EconomyService economyService;
    private final Supplier<UUID> idGenerator;
    private final Supplier<Long> clockMs;
    private CrownSquadConfig config;

    private final Map<String, List<CrownSquadUnit>> unitsByKingdom = new HashMap<>();

    public CrownSquadService(EconomyService economyService, CrownSquadConfig config) {
        this(economyService, config, UUID::randomUUID, System::currentTimeMillis);
    }

    public CrownSquadService(EconomyService economyService, CrownSquadConfig config, Supplier<UUID> idGenerator) {
        this(economyService, config, idGenerator, System::currentTimeMillis);
    }

    public CrownSquadService(
            EconomyService economyService,
            CrownSquadConfig config,
            Supplier<UUID> idGenerator,
            Supplier<Long> clockMs) {
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.config = Objects.requireNonNull(config, "config");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    public void setConfig(CrownSquadConfig config) {
        this.config = config != null ? config : CrownSquadConfig.defaults();
    }

    public CrownSquadConfig config() {
        return config;
    }

    /**
     * Purchases one crown squad unit for {@code kingdomId}, debiting the configured {@link
     * CrownSquadConfig#cost()} from the kingdom's approved treasury budget via {@link
     * EconomyService#spendFromBudget}. Rejects when crown squads are disabled, the crown-squad cap
     * is already reached, or the spend fails — whether for want of an approved budget or
     * insufficient treasury balance.
     */
    public WarResult purchase(String kingdomId) {
        if (kingdomId == null || kingdomId.isBlank()) {
            return WarResult.fail("Kingdom id is required.");
        }
        if (!config.enabled()) {
            return WarResult.fail("Crown squads are disabled.");
        }
        int currentCount = countOf(kingdomId);
        if (currentCount >= config.cap()) {
            return WarResult.fail("Crown squad cap reached (" + config.cap() + ").");
        }

        EconomyResult spendResult = economyService.spendFromBudget(kingdomId, config.cost());
        if (spendResult instanceof EconomyResult.Failure failure) {
            return WarResult.fail(failure.message());
        }

        UUID unitId = idGenerator.get();
        CrownSquadUnit unit = new CrownSquadUnit(unitId, kingdomId, clockMs.get());
        unitsByKingdom.computeIfAbsent(kingdomId, ignored -> new ArrayList<>()).add(unit);
        return WarResult.ok("Crown squad purchased for " + config.cost() + " Corona. Unit " + unitId + '.');
    }

    public List<CrownSquadUnit> unitsOf(String kingdomId) {
        if (kingdomId == null) {
            return List.of();
        }
        return List.copyOf(unitsByKingdom.getOrDefault(kingdomId, List.of()));
    }

    public int countOf(String kingdomId) {
        return unitsOf(kingdomId).size();
    }

    /**
     * Demobilisation destroys every crown squad ledgered for {@code kingdomId} — the placeholder
     * units are cleared entirely rather than merely stood down, matching the Bukkit layer's future
     * mob despawn on peace. No-op for an unknown, null, or blank kingdom id.
     */
    public void demobilise(String kingdomId) {
        if (kingdomId == null || kingdomId.isBlank()) {
            return;
        }
        unitsByKingdom.remove(kingdomId);
    }
}
