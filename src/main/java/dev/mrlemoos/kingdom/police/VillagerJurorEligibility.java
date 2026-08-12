package dev.mrlemoos.kingdom.police;

/**
 * Whether a territory villager may be claimed as a trial juror. Exclusions match the court
 * handoff: the accused, seated offices, the Speaker, Treasury Lords, the villager judge, the
 * Town Crier, and strikers.
 */
public final class VillagerJurorEligibility {

    public record Flags(
            boolean accused,
            boolean seatedMpOrPremier,
            boolean speaker,
            boolean treasuryLord,
            boolean villagerJudge,
            boolean townCrier,
            boolean striking) {}

    private VillagerJurorEligibility() {}

    public static boolean isEligible(Flags flags) {
        if (flags == null) {
            return false;
        }
        return !flags.accused()
                && !flags.seatedMpOrPremier()
                && !flags.speaker()
                && !flags.treasuryLord()
                && !flags.villagerJudge()
                && !flags.townCrier()
                && !flags.striking();
    }
}
