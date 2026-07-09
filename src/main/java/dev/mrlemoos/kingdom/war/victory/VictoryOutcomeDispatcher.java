package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Set;

/**
 * Notified exactly once by {@link VictoryEvaluator} when a decisive victory is recorded, per the
 * {@link ActiveWar#outcome()} named in the enacted war bill. {@code onAnnexation} receives a
 * <b>snapshot</b> of the attacker's captured chunks taken before demobilisation runs — see the
 * {@link VictoryEvaluator} class Javadoc for why the snapshot must be taken first — so a real
 * {@code RegionMergeExecutor} (Slice 6.6) can still plan a merge even though demobilisation may go
 * on to clear the capture tally. Both methods default to a no-op so this slice can wire the
 * dispatch point without implementing either outcome — {@code onWarTribute} gains a real {@code
 * WarTributeService} in Slice 6.7.
 */
public interface VictoryOutcomeDispatcher {

    /**
     * Called once when {@code war.outcome()} is {@link WarOutcome#ANNEXATION}.
     *
     * @param capturedChunks the attacker's captured-chunk snapshot, taken before demobilisation —
     *     may be empty when no {@code ChunkCaptureService} was supplied to the evaluator
     */
    default void onAnnexation(ActiveWar war, Set<ChunkCoord> capturedChunks) {}

    /** Called once when {@code war.outcome()} is {@link WarOutcome#WAR_TRIBUTE}. */
    default void onWarTribute(ActiveWar war) {}
}
