package dev.mrlemoos.kingdom.hearth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The winter day at the hearth: each hearth burns the day's fuel out of the container beside it,
 * every villager within reach of one that burnt is warm, and the rest are cold for the day. Warmth
 * wipes the slate; cold lengthens it until the villager downs tools.
 *
 * <p>Pure but for the ledger it keeps, which is all that is written to disk.
 */
public final class HearthDayService {

    private final ColdLedgerStore ledger;

    public HearthDayService(ColdLedgerStore ledger) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
    }

    /**
     * Settles one realm day for {@code kingdomId}. A season that asks no hearths burns nothing and
     * warms everybody, wiping any cold that stood over from a harder season.
     */
    public ColdDayOutcome settleDay(
            String kingdomId,
            List<HearthSite> hearths,
            List<ColdSubject> subjects,
            HearthConfig config,
            boolean hearthsRequired) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(hearths, "hearths");
        Objects.requireNonNull(subjects, "subjects");
        Objects.requireNonNull(config, "config");

        if (!hearthsRequired) {
            for (ColdSubject subject : subjects) {
                ledger.clear(kingdomId, subject.villagerId());
            }
            return new ColdDayOutcome(0, Map.of(), Set.of());
        }

        List<HearthSite> burning = new ArrayList<>();
        for (HearthSite hearth : hearths) {
            if (HearthFuelBurner.burnDay(hearth.fuel(), config.fuelPerDay())) {
                burning.add(hearth);
            }
        }

        Map<UUID, Double> yieldFactors = new LinkedHashMap<>();
        Set<UUID> striking = new LinkedHashSet<>();
        for (ColdSubject subject : subjects) {
            if (isWarm(subject, burning, config.warmthRadius())) {
                ledger.clear(kingdomId, subject.villagerId());
                yieldFactors.put(subject.villagerId(), Double.valueOf(1.0));
                continue;
            }
            int coldDays = ledger.coldDays(kingdomId, subject.villagerId()) + 1;
            ledger.setColdDays(kingdomId, subject.villagerId(), coldDays);
            yieldFactors.put(subject.villagerId(), Double.valueOf(ColdRamp.yieldFactor(coldDays, config)));
            if (!subject.strikeExempt() && ColdRamp.strikes(coldDays, config)) {
                striking.add(subject.villagerId());
            }
        }
        return new ColdDayOutcome(burning.size(), yieldFactors, striking);
    }

    private static boolean isWarm(ColdSubject subject, List<HearthSite> burning, double radius) {
        double limit = radius * radius;
        for (HearthSite hearth : burning) {
            double dx = subject.x() - (hearth.x() + 0.5);
            double dy = subject.y() - (hearth.y() + 0.5);
            double dz = subject.z() - (hearth.z() + 0.5);
            if (dx * dx + dy * dy + dz * dz <= limit) {
                return true;
            }
        }
        return false;
    }
}
