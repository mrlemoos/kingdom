package dev.mrlemoos.kingdom.war.desertion;

/**
 * The classes of desertion offence {@link DesertionEvaluator} recognises. Each is a
 * <b>morale breach</b>: it lowers military morale by weighted severity, never improves it, and
 * never itself sets Traitor — that is a political-track treason conviction only.
 *
 * <ul>
 *   <li>{@link #REFUSE_MUSTER} — explicitly refusing a levy muster call.</li>
 *   <li>{@link #LEAVE_SIEGE_WITHOUT_RELEASE} — leaving an active siege without a siege release
 *       from the subject's commanding officer or the crown/knight at a muster point.</li>
 *   <li>{@link #FIGHTING_FOR_ENEMY} — battlefield treason: dealing damage to liege military
 *       participants while at war and still on the liege's levy roster.</li>
 *   <li>{@link #DEFECTION} — leaving the liege's levy to accept an enemy oath of service or
 *       muster; the <b>dual-track offence</b> — it also lowers political loyalty.</li>
 * </ul>
 */
public enum MoraleBreachKind {
    REFUSE_MUSTER,
    LEAVE_SIEGE_WITHOUT_RELEASE,
    FIGHTING_FOR_ENEMY,
    DEFECTION
}
