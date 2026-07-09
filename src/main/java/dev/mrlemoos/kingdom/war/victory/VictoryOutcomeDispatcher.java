package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarOutcome;

/**
 * Notified exactly once by {@link VictoryEvaluator} when a decisive victory is recorded, per the
 * {@link ActiveWar#outcome()} named in the enacted war bill. Both methods default to a no-op so
 * this slice can wire the dispatch point without implementing either outcome — {@code
 * onAnnexation} gains a real {@code RegionMergeExecutor} in Slice 6.6, and {@code onWarTribute} a
 * real {@code WarTributeService} in Slice 6.7.
 */
public interface VictoryOutcomeDispatcher {

    /** Called once when {@code war.outcome()} is {@link WarOutcome#ANNEXATION}. */
    default void onAnnexation(ActiveWar war) {}

    /** Called once when {@code war.outcome()} is {@link WarOutcome#WAR_TRIBUTE}. */
    default void onWarTribute(ActiveWar war) {}
}
