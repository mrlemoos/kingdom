package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.election.ImmutableNpcPolicy;

/**
 * Whether a territory villager may be claimed as a trial juror. Exclusions match the court
 * handoff: the accused, seated offices, the Speaker, the villager judge, strikers, and
 * immutable realm NPCs (Treasury Lords, the Town Crier, the Cleric).
 */
public final class VillagerJurorEligibility {

    public record Flags(
            boolean accused,
            boolean seatedMpOrPremier,
            boolean speaker,
            boolean treasuryLord,
            boolean villagerJudge,
            boolean townCrier,
            boolean striking,
            boolean cleric) {}

    private VillagerJurorEligibility() {}

    public static boolean isEligible(Flags flags) {
        if (flags == null) {
            return false;
        }
        return !flags.accused()
                && !flags.seatedMpOrPremier()
                && !flags.speaker()
                && !flags.villagerJudge()
                && !flags.striking()
                && ImmutableNpcPolicy.mayTakeFurtherOffice(
                        flags.treasuryLord(), flags.townCrier(), flags.cleric());
    }
}
