package dev.mrlemoos.kingdom.granary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The winter day's reckoning at the table: a realm that could not draw its ration leaves every
 * villager a day hungrier, a realm that could wipes the slate, and from the seventh hungry day the
 * lot takes one of those still starving.
 *
 * <p>Pure but for the ledger it keeps, which is all that is written to disk. The same day settled
 * twice — two sweeps inside one realm day — neither lengthens the hunger nor draws the lot again.
 */
public final class HungerDayService {

    private final HungerLedgerStore ledger;
    private final StarvationLot lot;
    private final Map<String, Long> lastSettledDay = new LinkedHashMap<>();

    public HungerDayService(HungerLedgerStore ledger, StarvationLot lot) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.lot = Objects.requireNonNull(lot, "lot");
    }

    /** Settles one realm day for {@code kingdomId} against whether its ration was drawn in full. */
    public HungerDayOutcome settleDay(
            String kingdomId, List<HungerSubject> subjects, boolean unfed, long realmDay, GranaryConfig config) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(subjects, "subjects");
        Objects.requireNonNull(config, "config");

        Long settled = lastSettledDay.get(kingdomId);
        if (settled != null && settled.longValue() == realmDay) {
            return standingOutcome(kingdomId, subjects, config);
        }
        lastSettledDay.put(kingdomId, Long.valueOf(realmDay));

        if (!unfed) {
            Map<UUID, Double> fed = new LinkedHashMap<>();
            for (HungerSubject subject : subjects) {
                ledger.clear(kingdomId, subject.villagerId());
                fed.put(subject.villagerId(), Double.valueOf(1.0));
            }
            return new HungerDayOutcome(fed, Set.of(), Set.of(), Optional.empty());
        }

        Map<UUID, Double> yieldFactors = new LinkedHashMap<>();
        Set<UUID> striking = new LinkedHashSet<>();
        Set<UUID> starving = new LinkedHashSet<>();
        List<UUID> hat = new ArrayList<>();
        for (HungerSubject subject : subjects) {
            int hungryDays = ledger.hungryDays(kingdomId, subject.villagerId()) + 1;
            ledger.setHungryDays(kingdomId, subject.villagerId(), hungryDays);
            yieldFactors.put(subject.villagerId(), Double.valueOf(HungerRamp.yieldFactor(hungryDays, config)));
            if (subject.spared()) {
                continue;
            }
            if (HungerRamp.strikes(hungryDays, config)) {
                striking.add(subject.villagerId());
            }
            if (HungerRamp.starves(hungryDays, config)) {
                starving.add(subject.villagerId());
                hat.add(subject.villagerId());
            }
        }

        Optional<UUID> starved = hat.isEmpty() ? Optional.empty() : lot.draw(List.copyOf(hat));
        if (starved.isPresent()) {
            ledger.clear(kingdomId, starved.get());
        }
        return new HungerDayOutcome(yieldFactors, striking, starving, starved);
    }

    /** The day as it already stands, for a second sweep: read off the ledger, nothing moved. */
    private HungerDayOutcome standingOutcome(
            String kingdomId, List<HungerSubject> subjects, GranaryConfig config) {
        Map<UUID, Double> yieldFactors = new LinkedHashMap<>();
        Set<UUID> striking = new LinkedHashSet<>();
        Set<UUID> starving = new LinkedHashSet<>();
        for (HungerSubject subject : subjects) {
            int hungryDays = ledger.hungryDays(kingdomId, subject.villagerId());
            yieldFactors.put(subject.villagerId(), Double.valueOf(HungerRamp.yieldFactor(hungryDays, config)));
            if (subject.spared()) {
                continue;
            }
            if (HungerRamp.strikes(hungryDays, config)) {
                striking.add(subject.villagerId());
            }
            if (HungerRamp.starves(hungryDays, config)) {
                starving.add(subject.villagerId());
            }
        }
        return new HungerDayOutcome(yieldFactors, striking, starving, Optional.empty());
    }
}
